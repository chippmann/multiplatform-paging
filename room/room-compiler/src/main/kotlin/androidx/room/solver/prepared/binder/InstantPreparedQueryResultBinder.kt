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

package androidx.room.solver.prepared.binder

import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.solver.CodeGenScope
import androidx.room.solver.prepared.result.PreparedQueryResultAdapter

/**
 * Default binder for prepared queries.
 */
class InstantPreparedQueryResultBinder(adapter: PreparedQueryResultAdapter?) :
    PreparedQueryResultBinder(adapter) {

    override fun executeAndReturn(
        prepareQueryStmtBlock: CodeGenScope.() -> String,
        preparedStmtProperty: XPropertySpec?,
        dbProperty: XPropertySpec,
        scope: CodeGenScope
    ) {
        scope.builder.apply {
            addStatement("%N.assertNotSuspendingTransaction()", dbProperty)
        }
        adapter?.executeAndReturn(
            stmtQueryVal = scope.prepareQueryStmtBlock(),
            preparedStmtProperty = preparedStmtProperty,
            dbProperty = dbProperty,
            scope = scope
        )
    }
}