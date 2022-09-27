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

package androidx.camera.camera2.pipe.testing

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * The FakeCameraBackend implements [CameraBackend] and creates [CameraControllerSimulator]s.
 */
@RequiresApi(21)
class FakeCameraBackend(private val fakeCameras: Map<CameraId, CameraMetadata>) : CameraBackend {
    private val lock = Any()
    private val fakeCameraIds = fakeCameras.keys.toList()

    private val _cameraControllers = mutableListOf<CameraControllerSimulator>()
    val cameraControllers: List<CameraControllerSimulator>
        get() = synchronized(lock) { _cameraControllers.toList() }

    override val id: CameraBackendId
        get() = FAKE_CAMERA_BACKEND

    override fun readCameraIdList(): List<CameraId> = fakeCameraIds
    override fun readCameraMetadata(cameraId: CameraId): CameraMetadata =
        checkNotNull(fakeCameras[cameraId]) {
            "fakeCameras does not contain $cameraId. Available cameras are: $fakeCameras"
        }

    override fun disconnectAllAsync(): Deferred<Unit> {
        _cameraControllers.forEach {
            it.simulateCameraStopped()
        }
        return CompletableDeferred(Unit)
    }

    override fun shutdownAsync(): Deferred<Unit> {
        _cameraControllers.forEach {
            it.simulateCameraStopped()
        }
        return CompletableDeferred(Unit)
    }

    override fun createCameraController(
        cameraContext: CameraContext,
        graphConfig: CameraGraph.Config,
        graphListener: GraphListener,
        streamGraph: StreamGraph
    ): CameraController {
        val cameraController = CameraControllerSimulator(
            cameraContext,
            graphConfig,
            graphListener,
            streamGraph
        )
        synchronized(lock) {
            _cameraControllers.add(cameraController)
        }
        return cameraController
    }

    companion object {
        private val FAKE_CAMERA_BACKEND = CameraBackendId("camerapipe.testing.fake_backend")
    }
}