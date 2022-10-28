/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.solver.prepared.result

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XCodeBlock.Builder.Companion.addLocalVal
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.isInt
import androidx.room.compiler.processing.isKotlinUnit
import androidx.room.compiler.processing.isLong
import androidx.room.compiler.processing.isVoid
import androidx.room.compiler.processing.isVoidObject
import androidx.room.parser.QueryType
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.binder.PreparedQueryResultBinder

/**
 * An adapter for [PreparedQueryResultBinder] that executes queries with INSERT, UPDATE or DELETE
 * statements.
 */
class PreparedQueryResultAdapter(
    private val returnType: XType,
    private val queryType: QueryType
) {
    companion object {
        fun create(
            returnType: XType,
            queryType: QueryType
        ) = if (isValidReturnType(returnType, queryType)) {
            PreparedQueryResultAdapter(returnType, queryType)
        } else {
            null
        }

        private fun isValidReturnType(returnType: XType, queryType: QueryType): Boolean {
            if (returnType.isVoid() || returnType.isVoidObject() || returnType.isKotlinUnit()) {
                return true
            } else {
                return when (queryType) {
                    QueryType.INSERT -> returnType.isLong()
                    QueryType.UPDATE, QueryType.DELETE -> returnType.isInt()
                    else -> false
                }
            }
        }
    }

    fun executeAndReturn(
        stmtQueryVal: String,
        preparedStmtProperty: XPropertySpec?,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            val stmtMethod = if (queryType == QueryType.INSERT) {
                "executeInsert"
            } else {
                "executeUpdateDelete"
            }
            addStatement("%N.beginTransaction()", dbProperty)
            beginControlFlow("try").apply {
                if (returnType.isVoid() || returnType.isVoidObject() || returnType.isKotlinUnit()) {
                    addStatement("%L.%L()", stmtQueryVal, stmtMethod)
                    addStatement("%N.setTransactionSuccessful()", dbProperty)
                    if (returnType.isVoidObject()) {
                        addStatement("return null")
                    } else if (returnType.isKotlinUnit() && language == CodeLanguage.JAVA) {
                        addStatement("return %T.INSTANCE", Unit::class.asClassName())
                    }
                } else {
                    val resultVar = scope.getTmpVar("_result")
                    addLocalVal(
                        resultVar,
                        returnType.asTypeName(),
                        "%L.%L()",
                        stmtQueryVal, stmtMethod
                    )
                    addStatement("%N.setTransactionSuccessful()", dbProperty)
                    addStatement("return %L", resultVar)
                }
            }
            nextControlFlow("finally").apply {
                addStatement("%N.endTransaction()", dbProperty)
                if (preparedStmtProperty != null) {
                    addStatement("%N.release(%L)", preparedStmtProperty, stmtQueryVal)
                }
            }
            endControlFlow()
        }
    }
}