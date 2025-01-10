/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.wear.watchface.dfx.memory

import com.google.common.collect.ImmutableList
import java.util.Optional
import java.util.jar.Manifest
import kotlin.system.exitProcess
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException

/**
 * Contains the CLI arguments that the script was invoked with.
 *
 * @property watchFacePath Path to the watch face package to be evaluated.
 */
class EvaluationSettings(
    val watchFacePath: String
) {
    constructor(
        watchFacePath: String,
        greedyEvaluationSwitch: Int,
    ) : this(watchFacePath) {
        this.greedyEvaluationSwitch = greedyEvaluationSwitch
    }

    constructor(
        watchFacePath: String,
        version: String,
    ) : this(watchFacePath) {
        this.schemaVersion = version
    }

    constructor(
        watchFacePath: String,
        applyV1OffloadLimitations: Boolean,
        estimateOptimization: Boolean,
    ) : this(watchFacePath) {
        this.applyV1OffloadLimitations = applyV1OffloadLimitations
        this.estimateOptimization = estimateOptimization
    }

    /**
     * The schema version to validate against. This is only used when manually set via command-line,
     * which then overrides the value read from the Manifest file.
     */
    var schemaVersion: String? = null

    /**
     * The maximum number of configurations of a watch face under which the real footprint is
     * calculated. If a watch face has more than this number of configurations, the greedy
     * evaluations is used.
     */
    var greedyEvaluationSwitch: Int = GREEDY_DEFAULT_LIMIT
        private set

    /** The memory limit in bytes for the ambient mode. Defaults to 10 * 10^20 bytes (or 10 MB). */
    var ambientLimitBytes: Long = MemoryFootprint.toBytes(10.0)
        private set
    /** The memory limit in bytes for the active mode. Defaults to 100 * 10^20 bytes (or 10 MB). */
    var activeLimitBytes: Long = MemoryFootprint.toBytes(100.0)
        private set
    /**
     * Whether or not the script should work in verbose mode. Defaults to false. Enables extra
     * logging.
     */
    @get:JvmName("isVerbose")
    var verbose: Boolean = false
        private set
    /** Generate a JSON report instead of validating the watch face against the memory limits. */
    @get:JvmName("isReportMode")
    var reportMode: Boolean = false
        private set
    /** Whether or not old style analog clock XML should be supported. */
    @get:JvmName("supportOldStyleAnalogOrDigitalClock")
    var supportOldStyleAnalogOrDigitalClock: Boolean = true
        private set
    /** Whether or not resources should be deduplicted in ambient. */
    @get:JvmName("deduplicateAmbient")
    var deduplicateAmbient: Boolean = true
        private set
    /**
     * Whether or not V1 DWF offloading limitations should be enforced. A maximum of 2 layers and
     * two clocks.
     */
    @get:JvmName("applyV1OffloadLimitations")
    var applyV1OffloadLimitations: Boolean = false
        private set
    /**
     * Whether or not we should estimate the effects of optimizations such as resizing images,
     * cropping them, etc...
     */
    @get:JvmName("estimateOptimization")
    var estimateOptimization: Boolean = false
        private set

    val isHoneyfaceMode
        get() = schemaVersion == HONEYFACE_VERSION

    private object CliParserOptions {
        val options = Options()

        val watchFacePathOption =
            options.createOption {
                longOpt("watch-face")
                    .desc("Path to the watch face package to be evaluated. Required.")
                    .hasArg()
                    .required()
            }
        val schemaVersionOption =
            options.createOption {
                longOpt("schema-version")
                    .desc(
                        "Watch Face Format schema version of the watch face. This " +
                            "overrides the version specified in the manifest file")
                    .hasArg()
                    .type(String::class.java)
            }
        val ambientLimitOption =
            options.createOption {
                longOpt("ambient-limit-mb")
                    .desc("Limit in MB for the ambient mode. Optional.")
                    .hasArg()
                    // Must be Number.class. Commons-cli does not handle Integer.class
                    .type(Number::class.java)
            }
        val activeLimitOption =
            options.createOption {
                longOpt("active-limit-mb")
                    .desc("Limit in MB for the active mode. Optional.")
                    .hasArg()
                    .type(Number::class.java)
            }
        val disableOldStyleClocksOption =
            options.createOption {
                longOpt("disable-old-style-clocks")
                    .desc(
                        "Whether or not old style analog or digital clocks should be, " +
                            "disabled defaults to false."
                    )
                    .hasArg(false)
            }
        val disableAmbientDeduplicationOption =
            options.createOption {
                longOpt("disable-ambient-deduplication")
                    .desc("Whether or not in ambient we should de-duplicate resources.")
                    .hasArg(false)
            }
        val verboseOption =
            options.createOption {
                longOpt("verbose").desc("Turn on verbose mode. Optional.").hasArg(false)
            }
        val versionOption =
            options.createOption {
                longOpt("version").desc("Show the script's version and quit.").hasArg(false)
            }
        val helpOption =
            options.createOption {
                longOpt("help").desc("Display this help message.").hasArg(false)
            }
        val estimateOptimizationOption =
            options.createOption {
                longOpt("estimate-optimization").desc("Assume DWF optimizations.").hasArg(false)
            }

        val applyV1OffloadLimitationsOption =
            options.createOption {
                longOpt("apply-v1-offload-limitations")
                    .desc(
                        "Whether or not V1 offloading limitations should be applied to the" +
                            " calculation. I.e. a maximum of 2 layers and two clocks."
                    )
                    .hasArg(false)
            }

        val greedyEvaluationSwitchOption =
            options.createOption {
                longOpt("greedy-after-iterations")
                    .desc(
                        "If the watch face has more than this number of " +
                            "configurations, then the evaluation runs in " +
                            "greedy mode. Optional. Defaults to $GREEDY_DEFAULT_LIMIT.",
                    )
                    .hasArg()
                    .type(Number::class.java)
            }

        val reportModeOption =
            options.createOption {
                longOpt("report")
                    .desc(
                        "Generate a JSON report instead of validating the watch " +
                            "face against the memory limits."
                    )
                    .hasArg(false)
            }

        private fun Options.createOption(block: Option.Builder.() -> Option.Builder): Option =
            Option.builder().block().build().also { this.addOption(it) }
    }

    companion object {
        private const val HONEYFACE_VERSION = "honeyface"
        private const val GREEDY_DEFAULT_LIMIT = 10_000_000

        private val SUPPORTED_VERSIONS: List<String> = ImmutableList.of(HONEYFACE_VERSION, "1", "2")

        @JvmStatic
        fun parseFromArguments(vararg arguments: String): Optional<EvaluationSettings> =
            with(CliParserOptions) {
                val cliInvokeCommand = "java -jar memory-footprint.jar"

                val parser = DefaultParser()
                try {
                    arguments.forEach { arg ->
                        when (arg) {
                            "--version" -> {
                                printVersion()
                                exitProcess(0)
                            }
                            "--help" -> {
                                HelpFormatter().printHelp(cliInvokeCommand, options, true)
                                exitProcess(0)
                            }
                            else -> {}
                        }
                    }

                    val line = parser.parse(options, arguments)

                    val evaluationSettings =
                        EvaluationSettings(
                            line.getOptionValue(watchFacePathOption)
                        )

                    if (line.hasOption(activeLimitOption)) {
                        evaluationSettings.activeLimitBytes =
                            MemoryFootprint.toBytes(
                                (line.getParsedOptionValue(activeLimitOption) as Number)
                                    .toInt()
                                    .toDouble()
                            )
                    }
                    if (line.hasOption(ambientLimitOption)) {
                        evaluationSettings.ambientLimitBytes =
                            MemoryFootprint.toBytes(
                                (line.getParsedOptionValue(ambientLimitOption) as Number)
                                    .toInt()
                                    .toDouble()
                            )
                    }
                    if (line.hasOption(greedyEvaluationSwitchOption)) {
                        evaluationSettings.greedyEvaluationSwitch =
                            (line.getParsedOptionValue(greedyEvaluationSwitchOption) as Number)
                                .toInt()
                    }
                    if (line.hasOption(disableOldStyleClocksOption)) {
                        evaluationSettings.supportOldStyleAnalogOrDigitalClock = false
                    }
                    if (line.hasOption(disableAmbientDeduplicationOption)) {
                        evaluationSettings.deduplicateAmbient = false
                    }
                    if (line.hasOption(verboseOption)) {
                        evaluationSettings.verbose = true
                    }
                    if (line.hasOption(applyV1OffloadLimitationsOption)) {
                        evaluationSettings.applyV1OffloadLimitations = true
                    }
                    if (line.hasOption(estimateOptimizationOption)) {
                        evaluationSettings.estimateOptimization = true
                    }
                    if (line.hasOption(reportModeOption)) {
                        evaluationSettings.reportMode = true
                    }
                    if (line.hasOption(schemaVersionOption)) {
                        validateSchemaVersion(line.getOptionValue(schemaVersionOption))
                        evaluationSettings.schemaVersion = line.getOptionValue(schemaVersionOption)
                    }
                    Optional.of(evaluationSettings)
                } catch (e: Exception) {
                    println("Error: " + e.localizedMessage)
                    HelpFormatter().printHelp(cliInvokeCommand, options, true)
                    Optional.empty()
                }
            }

        private fun printVersion() {
            val resources =
                EvaluationSettings::class.java.classLoader.getResources("META-INF/MANIFEST.MF")
            while (resources.hasMoreElements()) {
                val manifest = Manifest(resources.nextElement().openStream())
                val version = manifest.mainAttributes.getValue("Version")

                val hash = manifest.mainAttributes.getValue("Git-Hash")
                println(
                    "memory-footprint version: " + (version ?: "n/a") + " hash: " + (hash ?: "n/a")
                )
            }
        }

        private fun validateSchemaVersion(schemaVersionOption: String) {
            if (!SUPPORTED_VERSIONS.contains(schemaVersionOption)) {
                throw ParseException(
                    "Argument --schema-version has a wrong value. Supported values are " +
                        SUPPORTED_VERSIONS.joinToString(", "),
                )
            }
        }
    }
}
