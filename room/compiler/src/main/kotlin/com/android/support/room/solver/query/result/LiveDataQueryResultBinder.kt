/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.support.room.solver.query.result

import com.android.support.room.ext.AndroidTypeNames
import com.android.support.room.ext.L
import com.android.support.room.ext.LifecyclesTypeNames
import com.android.support.room.ext.N
import com.android.support.room.ext.RoomTypeNames.INVALIDATION_OBSERVER
import com.android.support.room.ext.T
import com.android.support.room.ext.typeName
import com.android.support.room.parser.Table
import com.android.support.room.solver.CodeGenScope
import com.android.support.room.writer.DaoWriter
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror

/**
 * Converts the query into a LiveData and returns it. No query is run until necessary.
 */
class LiveDataQueryResultBinder(val typeArg: TypeMirror, val tableNames: Set<Table>,
                                adapter: QueryResultAdapter?)
    : QueryResultBinder(adapter) {
    override fun convertAndReturn(sqlVar: String, argsVar: String, dbField: FieldSpec,
                                  scope: CodeGenScope) {
        val typeName = typeArg.typeName()

        val liveDataImpl = TypeSpec.anonymousClassBuilder("").apply {
            superclass(ParameterizedTypeName.get(LifecyclesTypeNames.COMPUTABLE_LIVE_DATA,
                    typeName))
            val observerLockField = FieldSpec.builder(TypeName.BOOLEAN,
                    scope.getTmpVar("_startedObserving"), Modifier.PRIVATE).initializer("false")
                    .build()
            addField(observerLockField)
            addMethod(createComputeMethod(
                    observerLockField = observerLockField,
                    typeName = typeName,
                    sqlVar = sqlVar,
                    argsVar = argsVar,
                    dbField = dbField,
                    scope = scope
            ))
        }.build()
        scope.builder().apply {
            addStatement("return $L.getLiveData()", liveDataImpl)
        }
    }

    private fun createComputeMethod(sqlVar: String, argsVar: String, typeName: TypeName,
                                    observerLockField : FieldSpec,  dbField: FieldSpec,
                                    scope: CodeGenScope): MethodSpec {
        return MethodSpec.methodBuilder("compute").apply {
            addAnnotation(Override::class.java)
            addModifiers(Modifier.PROTECTED)
            returns(typeName)
            val outVar = scope.getTmpVar("_result")
            val cursorVar = scope.getTmpVar("_cursor")

            beginControlFlow("if (!$N)", observerLockField).apply {
                addStatement("$N = true", observerLockField)
                addStatement("$N.getInvalidationTracker().addWeakObserver($L)",
                        dbField, createAnonymousObserver())
            }
            endControlFlow()

            addStatement("final $T $L = $N.query($L, $L)", AndroidTypeNames.CURSOR, cursorVar,
                    DaoWriter.dbField, sqlVar, argsVar)
            beginControlFlow("try").apply {
                val adapterScope = scope.fork()
                adapter?.convert(outVar, cursorVar, adapterScope)
                addCode(adapterScope.builder().build())
                addStatement("return $L", outVar)
            }
            nextControlFlow("finally").apply {
                addStatement("$L.close()", cursorVar)
            }
            endControlFlow()
        }.build()
    }

    private fun createAnonymousObserver(): TypeSpec {
        val tableNamesList = tableNames.joinToString(",") { "\"${it.name}\"" }
        return TypeSpec.anonymousClassBuilder(tableNamesList).apply {
            superclass(INVALIDATION_OBSERVER)
            addMethod(MethodSpec.methodBuilder("onInvalidated").apply {
                returns(TypeName.VOID)
                addAnnotation(Override::class.java)
                addModifiers(Modifier.PUBLIC)
                addStatement("invalidate()")
            }.build())
        }.build()
    }
}
