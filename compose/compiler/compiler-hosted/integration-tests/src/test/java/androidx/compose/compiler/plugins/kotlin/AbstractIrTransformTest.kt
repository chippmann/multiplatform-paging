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

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.dumpSrc
import java.io.File
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.jvmPhases
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.psi.KtFile

abstract class AbstractIrTransformTest : AbstractCodegenTest() {
    open val liveLiteralsEnabled get() = false
    open val liveLiteralsV2Enabled get() = false
    open val generateFunctionKeyMetaClasses get() = false
    open val sourceInformationEnabled get() = true
    open val intrinsicRememberEnabled get() = true
    open val decoysEnabled get() = false
    open val metricsDestination: String? get() = null
    open val reportsDestination: String? get() = null
    open val validateIr: Boolean get() = true

    protected fun createComposeIrGenerationExtension(): ComposeIrGenerationExtension =
        ComposeIrGenerationExtension(
            liveLiteralsEnabled,
            liveLiteralsV2Enabled,
            generateFunctionKeyMetaClasses,
            sourceInformationEnabled,
            intrinsicRememberEnabled,
            decoysEnabled,
            metricsDestination,
            reportsDestination,
            validateIr,
        )

    fun verifyCrossModuleComposeIrTransform(
        @Language("kotlin")
        dependencySource: String,
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
        dumpTree: Boolean = false,
        dumpClasses: Boolean = false,
    ) {
        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        val dependencyFileName = "Test_REPLACEME_${uniqueNumber++}"
        val classesDirectory = tmpDir("kotlin-classes")

        classLoader(dependencySource, dependencyFileName, dumpClasses)
            .allGeneratedFiles
            .also {
                // Write the files to the class directory so they can be used by the next module
                // and the application
                it.writeToDir(classesDirectory)
            }

        // Setup for compile
        this.classFileFactory = null
        this.myEnvironment = null
        setUp()

        verifyComposeIrTransform(
            source,
            expectedTransformed,
            "",
            dumpTree = dumpTree,
            additionalPaths = listOf(classesDirectory)
        )
    }

    fun verifyComposeIrTransform(
        @Language("kotlin")
        source: String,
        expectedTransformed: String,
        @Language("kotlin")
        extra: String = "",
        validator: (element: IrElement) -> Unit = {},
        dumpTree: Boolean = false,
        truncateTracingInfoMode: TruncateTracingInfoMode = TruncateTracingInfoMode.TRUNCATE_KEY,
        additionalPaths: List<File> = listOf(),
        applyExtraConfiguration: CompilerConfiguration.() -> Unit = {}
    ) {
        val files = listOf(
            sourceFile("Test.kt", source.replace('%', '$')),
            sourceFile("Extra.kt", extra.replace('%', '$'))
        )
        val irModule = compileToIr(files, additionalPaths, applyExtraConfiguration)
        val keySet = mutableListOf<Int>()
        fun IrElement.validate(): IrElement = this.also { validator(it) }
        val actualTransformed = irModule
            .files[0]
            .validate()
            .dumpSrc()
            .replace('$', '%')
            // replace source keys for start group calls
            .replace(
                Regex(
                    "(%composer\\.start(Restart|Movable|Replaceable)Group\\()-?((0b)?[-\\d]+)"
                )
            ) {
                val stringKey = it.groupValues[3]
                val key = if (stringKey.startsWith("0b"))
                    Integer.parseInt(stringKey.drop(2), 2)
                else
                    stringKey.toInt()
                if (key in keySet) {
                    "${it.groupValues[1]}<!DUPLICATE KEY: $key!>"
                } else {
                    keySet.add(key)
                    "${it.groupValues[1]}<>"
                }
            }
            .replace(
                Regex("(sourceInformationMarkerStart\\(%composer, )([-\\d]+)")
            ) {
                "${it.groupValues[1]}<>"
            }
            // replace traceEventStart values with a token
            // TODO(174715171): capture actual values for testing
            .replace(
                Regex(
                    "traceEventStart\\(-?\\d+, (%dirty|%changed|-1), (%dirty1|%changed1|-1), (.*)"
                )
            ) {
                when (truncateTracingInfoMode) {
                    TruncateTracingInfoMode.TRUNCATE_KEY ->
                        "traceEventStart(<>, ${it.groupValues[1]}, ${it.groupValues[2]}, <>)"
                    TruncateTracingInfoMode.KEEP_INFO_STRING ->
                        "traceEventStart(<>, ${it.groupValues[1]}, ${it.groupValues[2]}, " +
                            it.groupValues[3]
                }
            }
            // replace source information with source it references
            .replace(
                Regex(
                    "(%composer\\.start(Restart|Movable|Replaceable)Group\\" +
                        "([^\"\\n]*)\"(.*)\"\\)"
                )
            ) {
                "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[4], source)}\")"
            }
            .replace(
                Regex("(sourceInformation(MarkerStart)?\\(.*)\"(.*)\"\\)")
            ) {
                "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[3], source)}\")"
            }
            .replace(
                Regex(
                    "(composableLambda[N]?\\" +
                        "([^\"\\n]*)\"(.*)\"\\)"
                )
            ) {
                "${it.groupValues[1]}\"${generateSourceInfo(it.groupValues[2], source)}\")"
            }
            // replace source keys for joinKey calls
            .replace(
                Regex(
                    "(%composer\\.joinKey\\()([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            // composableLambdaInstance(<>, true)
            .replace(
                Regex(
                    "(composableLambdaInstance\\()([-\\d]+, (true|false))"
                )
            ) {
                val callStart = it.groupValues[1]
                val tracked = it.groupValues[3]
                "$callStart<>, $tracked"
            }
            // composableLambda(%composer, <>, true)
            .replace(
                Regex(
                    "(composableLambda\\(%composer,\\s)([-\\d]+)"
                )
            ) {
                "${it.groupValues[1]}<>"
            }
            .trimIndent()
            .trimTrailingWhitespacesAndAddNewlineAtEOF()

        if (dumpTree) {
            println(irModule.dump())
        }
        assertEquals(
            expectedTransformed
                .trimIndent()
                .trimTrailingWhitespacesAndAddNewlineAtEOF(),
            actualTransformed
        )
    }

    private fun MatchResult.isNumber() = groupValues[1].isNotEmpty()
    private fun MatchResult.number() = groupValues[1].toInt()
    private val MatchResult.text get() = groupValues[0]
    private fun MatchResult.isChar(c: String) = text == c
    private fun MatchResult.isFileName() = groups[4] != null

    private fun generateSourceInfo(sourceInfo: String, source: String): String {
        val r = Regex("(\\d+)|([,])|([*])|([:])|C(\\(.*\\))?|L|(P\\(*\\))|@")
        var current = 0
        var currentResult = r.find(sourceInfo, current)
        var result = ""

        fun next(): MatchResult? {
            currentResult?.let {
                current = it.range.last + 1
                currentResult = it.next()
            }
            return currentResult
        }

        // A location has the format: [<line-number>]['@' <offset> ['L' <length>]]
        // where the named productions are numbers
        fun parseLocation(): String? {
            var mr = currentResult
            if (mr != null && mr.isNumber()) {
                // line number, we ignore the value in during testing.
                mr = next()
            }
            if (mr != null && mr.isChar("@")) {
                // Offset
                mr = next()
                if (mr == null || !mr.isNumber()) {
                    return null
                }
                val offset = mr.number()
                mr = next()
                var ellipsis = ""
                val maxFragment = 6
                val rawLength = if (mr != null && mr.isChar("L")) {
                    mr = next()
                    if (mr == null || !mr.isNumber()) {
                        return null
                    }
                    mr.number().also { next() }
                } else {
                    maxFragment
                }
                val eol = source.indexOf('\n', offset).let {
                    if (it < 0) source.length else it
                }
                val space = source.indexOf(' ', offset).let {
                    if (it < 0) source.length else it
                }
                val maxEnd = offset + maxFragment
                if (eol > maxEnd && space > maxEnd) ellipsis = "..."
                val length = minOf(maxEnd, minOf(offset + rawLength, space, eol)) - offset
                return "<${source.substring(offset, offset + length)}$ellipsis>"
            }
            return null
        }

        while (currentResult != null) {
            val mr = currentResult!!
            if (mr.range.first != current) {
                return "invalid source info at $current: '$sourceInfo'"
            }
            when {
                mr.isNumber() || mr.isChar("@") -> {
                    val fragment = parseLocation()
                        ?: return "invalid source info at $current: '$sourceInfo'"
                    result += fragment
                }
                mr.isFileName() -> {
                    return result + ":" + sourceInfo.substring(mr.range.last + 1)
                }
                else -> {
                    result += mr.text
                    next()
                }
            }
            require(mr != currentResult) { "regex didn't advance" }
        }
        if (current != sourceInfo.length)
            return "invalid source info at $current: '$sourceInfo'"
        return result
    }

    fun compileToIr(
        files: List<KtFile>,
        additionalPaths: List<File> = listOf(),
        applyExtraConfiguration: CompilerConfiguration.() -> Unit = {}
    ): IrModuleFragment = compileToIrWithExtension(
        files, createComposeIrGenerationExtension(), additionalPaths, applyExtraConfiguration
    )

    fun compileToIrWithExtension(
        files: List<KtFile>,
        extension: IrGenerationExtension,
        additionalPaths: List<File> = listOf(),
        applyExtraConfiguration: CompilerConfiguration.() -> Unit = {}
    ): IrModuleFragment {
        val classPath = createClasspath() + additionalPaths
        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
        configuration.applyExtraConfiguration()

        configuration.configureJdkClasspathRoots()

        val environment = KotlinCoreEnvironment.createForTests(
            myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        ComposeComponentRegistrar.registerCommonExtensions(environment.project)
        IrGenerationExtension.registerExtension(environment.project, extension)

        val analysisResult = JvmResolveUtil.analyzeAndCheckForErrors(environment, files)
        val codegenFactory = JvmIrCodegenFactory(
            configuration,
            configuration.get(CLIConfigurationKeys.PHASE_CONFIG) ?: PhaseConfig(jvmPhases)
        )

        val state = GenerationState.Builder(
            environment.project,
            ClassBuilderFactories.TEST,
            analysisResult.moduleDescriptor,
            analysisResult.bindingContext,
            files,
            configuration
        ).isIrBackend(true).codegenFactory(codegenFactory).build()

        state.beforeCompile()

        val psi2irInput = CodegenFactory.IrConversionInput.fromGenerationStateAndFiles(
            state,
            files
        )
        return codegenFactory.convertToIr(psi2irInput).irModuleFragment
    }

    enum class TruncateTracingInfoMode {
        TRUNCATE_KEY, // truncates only the `key` parameter
        KEEP_INFO_STRING, // truncates everything except for the `info` string
    }
}