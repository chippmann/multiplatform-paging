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

package androidx.benchmark.macro.perfetto

import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.benchmark.macro.R
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/**
 * Enables capturing a Perfetto trace from a test on Q+ devices.
 *
 * It's possible to support API 28, but there are a few issues to resolve:
 * - Use binary config protos
 * - May need to distribute perfetto binary, with atrace workaround
 * - App tags are not available, due to lack of `<profileable shell=true>`. Can potentially hack
 * around this for individual tags within test infra as needed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(29)
class PerfettoCapture {
    private val helper = PerfettoHelper()

    /**
     * Kill perfetto process, if it is running.
     */
    fun cancel() {
        if (helper.isPerfettoRunning) {
            helper.stopPerfetto()
        }
    }

    /**
     * Start collecting perfetto trace.
     *
     * TODO: provide configuration options
     */
    fun start() {
        val context = InstrumentationRegistry.getInstrumentation().context
        // Write textproto asset to external files dir, so it can be read by shell
        // TODO: use binary proto (which will also give us rooted 28 support)
        val configBytes = context.resources.openRawResource(R.raw.trace_config).readBytes()
        val textProtoFile = File(context.getExternalFilesDir(null), "trace_config.textproto")
        try {
            textProtoFile.writeBytes(configBytes)
            // Start tracing
            if (!helper.startCollecting(textProtoFile.absolutePath, true)) {
                // TODO: move internal failures to be exceptions
                throw IllegalStateException("Unable to read start collecting")
            }
        } finally {
            textProtoFile.delete()
        }
    }

    /**
     * Stop collection, and record trace to the specified file path.
     *
     * @param destinationPath Absolute path to write perfetto trace to. Must be shell-writable,
     * such as result of `context.getExternalFilesDir(null)` or other similar `external` paths.
     */
    fun stop(destinationPath: String) {
        if (!helper.stopCollecting(400, destinationPath)) {
            // TODO: move internal failures to be exceptions
            throw IllegalStateException("Unable to store perfetto trace")
        }
    }

    /*
     * Get path for an file to be written to additionalTestOutputDir
     *
     * NOTE: this method of getting additionalTestOutputDir duplicates behavior in
     *androidx.benchmark.Arguments`, and should be unified at some point.
     */
    fun destinationPath(traceName: String): String {
        val additionalTestOutputDir = InstrumentationRegistry.getArguments()
            .getString("additionalTestOutputDir")

        @Suppress("DEPRECATION") // Legacy code path for versions of agp older than 3.6
        val testOutputDir = additionalTestOutputDir?.let { File(it) }
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(testOutputDir, traceName).absolutePath
    }
}
