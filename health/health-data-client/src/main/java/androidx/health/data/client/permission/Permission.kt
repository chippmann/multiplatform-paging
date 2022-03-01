/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.health.data.client.permission

import androidx.health.data.client.records.Record
import kotlin.reflect.KClass

/**
 * Class to represent a permission which consists of a [KClass] representing a data type and a
 * [AccessType] enum representing an access type.
 */
public data class Permission(
    public val recordType: KClass<out Record>,
    public val accessType: AccessType,
) {
    companion object {
        /** Creates a permission of the given [accessType] for record type [T]. */
        public inline fun <reified T : Record> create(accessType: AccessType): Permission {
            return Permission(T::class, accessType)
        }
    }
}
