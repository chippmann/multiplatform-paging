/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.config

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.compat.Camera2Backend
import androidx.camera.camera2.pipe.compat.Camera2CameraAvailabilityMonitor
import androidx.camera.camera2.pipe.compat.Camera2CameraController
import androidx.camera.camera2.pipe.compat.Camera2CameraOpener
import androidx.camera.camera2.pipe.compat.Camera2CaptureSequenceProcessorFactory
import androidx.camera.camera2.pipe.compat.Camera2CaptureSessionsModule
import androidx.camera.camera2.pipe.compat.Camera2MetadataCache
import androidx.camera.camera2.pipe.compat.CameraAvailabilityMonitor
import androidx.camera.camera2.pipe.compat.CameraMetadataProvider
import androidx.camera.camera2.pipe.compat.CameraOpener
import androidx.camera.camera2.pipe.compat.StandardCamera2CaptureSequenceProcessorFactory
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Scope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Module(
    subcomponents = [
        Camera2ControllerComponent::class
    ]
)
internal abstract class Camera2Module {
    @Binds
    @CameraPipeCameraBackend
    abstract fun bindCameraPipeCameraBackend(camera2Backend: Camera2Backend): CameraBackend

    @Binds
    abstract fun bindCameraOpener(camera2CameraOpener: Camera2CameraOpener): CameraOpener

    @Binds
    abstract fun bindCameraMetadataProvider(
        camera2MetadataCache: Camera2MetadataCache
    ): CameraMetadataProvider

    @Binds
    abstract fun bindCameraAvailabilityMonitor(
        camera2CameraAvailabilityMonitor: Camera2CameraAvailabilityMonitor
    ): CameraAvailabilityMonitor
}

@Scope
internal annotation class Camera2ControllerScope

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Camera2ControllerScope
@Subcomponent(
    modules = [
        Camera2ControllerConfig::class,
        Camera2ControllerModule::class,
        Camera2CaptureSessionsModule::class
    ]
)
internal interface Camera2ControllerComponent {
    fun cameraController(): CameraController

    @Subcomponent.Builder
    interface Builder {
        fun camera2ControllerConfig(config: Camera2ControllerConfig): Builder
        fun build(): Camera2ControllerComponent
    }
}

@Module
internal class Camera2ControllerConfig(
    private val cameraBackend: CameraBackend,
    private val graphConfig: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val streamGraph: StreamGraph,
) {
    @Provides
    fun provideCameraGraphConfig() = graphConfig

    @Provides
    fun provideCameraBackend() = cameraBackend

    @Provides
    fun provideStreamGraph() = streamGraph as StreamGraphImpl

    @Provides
    fun provideGraphListener() = graphListener
}

@Module
internal abstract class Camera2ControllerModule {
    @Binds
    abstract fun bindCamera2RequestProcessorFactory(
        factoryStandard: StandardCamera2CaptureSequenceProcessorFactory
    ): Camera2CaptureSequenceProcessorFactory

    @Binds
    abstract fun bindCameraController(
        camera2CameraController: Camera2CameraController
    ): CameraController

    companion object {
        @Camera2ControllerScope
        @Provides
        fun provideCoroutineScope(threads: Threads): CoroutineScope {
            return CoroutineScope(
                threads.lightweightDispatcher.plus(CoroutineName("CXCP-Camera2Controller"))
            )
        }
    }
}