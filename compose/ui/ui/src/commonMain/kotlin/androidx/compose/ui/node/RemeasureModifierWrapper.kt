/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints

/**
 * Wrapper around the [OnRemeasuredModifier] to notify whenever a remeasurement happens.
 */
internal class RemeasureModifierWrapper(
    wrapped: LayoutNodeWrapper,
    modifier: OnRemeasuredModifier
) : DelegatingLayoutNodeWrapper<OnRemeasuredModifier>(wrapped, modifier) {
    override fun performMeasure(constraints: Constraints): Placeable {
        val placeable = super.performMeasure(constraints)
        val invokeRemeasureCallbacks = {
            modifier.onRemeasured(measuredSize)
        }
        layoutNode.owner?.snapshotObserver?.pauseSnapshotReadObservation(invokeRemeasureCallbacks)
            ?: invokeRemeasureCallbacks.invoke()
        return placeable
    }
}