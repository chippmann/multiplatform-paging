/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.tiles;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Holder for ProtoTiles' Resources class, to be parceled and transferred to Wear.
 *
 * <p>All this does is to serialize Resources as a protobuf and transmit it.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ResourcesData extends ProtoParcelable {
    public static final int VERSION_PROTOBUF = 1;

    public static final Creator<ResourcesData> CREATOR =
            newCreator(ResourcesData.class, ResourcesData::new);

    public ResourcesData(@NonNull byte[] payload, int version) {
        super(payload, version);
    }
}
