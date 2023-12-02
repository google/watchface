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

package com.google.wear.watchface.dfx.memory;

import com.google.common.collect.ImmutableList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.List;
import java.util.Optional;

/** Contains the CLI arguments that the script was invoked with. */
final class EvaluationSettings {

    private static final String HONEYFACE_VERSION = "honeyface";

    private static final List<String> SUPPORTED_VERSIONS = ImmutableList.of(HONEYFACE_VERSION, "1");

    private static final int GREEDY_DEFAULT_LIMIT = 10_000_000;

    private final String watchFacePath;

    private final String schemaVersion;

    private long ambientLimitBytes = MemoryFootprint.toBytes(10);

    private long activeLimitBytes = MemoryFootprint.toBytes(10);

    private int greedyEvaluationSwitch = GREEDY_DEFAULT_LIMIT;

    private boolean verbose = false;

    private boolean reportMode = false;

    private boolean supportOldStyleAnalogOrDigitalClock = true;

    private boolean deduplicateAmbient = true;

    private boolean applyV1OffloadLimitations = false;

    private boolean estimateOptimization = false;

    EvaluationSettings(String watchFacePath, String schemaVersion) {
        this.watchFacePath = watchFacePath;
        this.schemaVersion = schemaVersion;
    }

    EvaluationSettings(String watchFacePath, String schemaVersion, int greedyEvaluationSwitch) {
        this.watchFacePath = watchFacePath;
        this.schemaVersion = schemaVersion;
        this.greedyEvaluationSwitch = greedyEvaluationSwitch;
    }

    /** Path to the watch face package to be evaluated. */
    public String getWatchFacePath() {
        return watchFacePath;
    }

    /** Watch Face Format schema version that the evaluation should use. */
    public String getSchemaVersion() {
        return schemaVersion;
    }

    /** The memory limit in bytes for the ambient mode. Defaults to 10 * 10^20 bytes (or 10 MB). */
    public long getAmbientLimitBytes() {
        return ambientLimitBytes;
    }

    /** The memory limit in bytes for the active mode. Defaults to 10 * 10^20 bytes (or 10 MB). */
    public long getActiveLimitBytes() {
        return activeLimitBytes;
    }

    /**
     * Whether or not the script should work in verbose mode. Defaults to false. Enables extra
     * logging.
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * The maximum number of configurations of a watch face under which the real footprint is
     * calculated. If a watch face has more than this number of configurations, the greedy
     * evaluations is used.
     */
    public int getGreedyEvaluationSwitch() {
        return greedyEvaluationSwitch;
    }

    /** Generate a JSON report instead of validating the watch face against the memory limits. */
    public boolean isReportMode() {
        return reportMode;
    }

    /** Whether or not old style analog clock XML should be supported. */
    public boolean supportOldStyleAnalogOrDigitalClock() {
        return supportOldStyleAnalogOrDigitalClock;
    }

    /** Whether or not resources should be deduplicted in ambient. */
    public boolean deduplicateAmbient() {
        return deduplicateAmbient;
    }

    public boolean isHoneyfaceMode() {
        return this.schemaVersion.equals(HONEYFACE_VERSION);
    }

    /**
     * Whether or not V1 DWF offloading limitations should be enforced. A maximum of 2 layers and
     * two clocks.
     */
    public boolean applyV1OffloadLimitations() {
        return applyV1OffloadLimitations;
    }

    /**
     * Whether or not we shuld estimate the effects of optimizations such as resizing images,
     * cropping them, etc...
     */
    public boolean estimateOptimization() {
        return estimateOptimization;
    }

    /**
     * Minimal CLI arguments parsing.
     *
     * @param arguments The CLI arguments that the script was invoked with.
     * @return the EvaluationSettings object containing the CLI arguments with default values for
     *     non-required arguments, or {@code Optional.empty()} if the arguments are invalid.
     */
    public static Optional<EvaluationSettings> parseFromArguments(String... arguments) {
        Option watchFacePathOption =
                Option.builder()
                        .longOpt("watch-face")
                        .desc("Path to the watch face package to be evaluated. Required.")
                        .hasArg()
                        .required()
                        .build();
        Option schemaVersionOption =
                Option.builder()
                        .longOpt("schema-version")
                        .desc("Watch Face Format schema version of the watch face. Required.")
                        .hasArg()
                        .required()
                        .build();
        Option ambientLimitOption =
                Option.builder()
                        .longOpt("ambient-limit-mb")
                        .desc("Limit in MB for the ambient mode. Optional.")
                        .hasArg()
                        // Must be Number.class. Commons-cli does not handle Integer.class
                        .type(Number.class)
                        .build();
        Option activeLimitOption =
                Option.builder()
                        .longOpt("active-limit-mb")
                        .desc("Limit in MB for the active mode. Optional.")
                        .hasArg()
                        .type(Number.class)
                        .build();
        Option disableOldStyleClocksOption =
                Option.builder()
                        .longOpt("disable-old-style-clocks")
                        .desc(
                                "Whether or not old style analog or digital clocks should be, "
                                        + "disabled defaults to false.")
                        .hasArg(false)
                        .build();
        Option disableAmbientDeduplicationOption =
                Option.builder()
                        .longOpt("disable-ambient-deduplication")
                        .desc("Whether or not in ambient we should de-duplicate resources.")
                        .hasArg(false)
                        .build();
        Option verboseOption =
                Option.builder()
                        .longOpt("verbose")
                        .desc("Turn on verbose mode. Optional.")
                        .hasArg(false)
                        .build();
        Option estimateOptimizationOption =
                Option.builder()
                        .longOpt("estimate-optimization")
                        .desc("Assume DWF optimizations.")
                        .hasArg(false)
                        .build();

        Option applyV1OffloadLimitationsOption =
                Option.builder()
                        .longOpt("apply-v1-offload-limitations")
                        .desc("Whether or not V1 offloading limitations should be applied to " +
                                "the calculation. I.e. a maximum of 2 layers and two clocks.")
                        .hasArg(false)
                        .build();

        Option greedyEvaluationSwitchOption =
                Option.builder()
                        .longOpt("greedy-after-iterations")
                        .desc(
                                String.format(
                                        "If the watch face has more than this number of "
                                                + "configurations, then the evaluation runs in "
                                                + "greedy mode. Optional. Defaults to %s",
                                        GREEDY_DEFAULT_LIMIT))
                        .hasArg()
                        .type(Number.class)
                        .build();

        Option reportModeOption =
                Option.builder()
                        .longOpt("report")
                        .desc(
                                "Generate a JSON report instead of validating the watch "
                                        + "face against the memory limits.")
                        .hasArg(false)
                        .build();

        Options options = new Options();
        options.addOption(watchFacePathOption);
        options.addOption(schemaVersionOption);
        options.addOption(ambientLimitOption);
        options.addOption(activeLimitOption);
        options.addOption(disableOldStyleClocksOption);
        options.addOption(disableAmbientDeduplicationOption);
        options.addOption(verboseOption);
        options.addOption(applyV1OffloadLimitationsOption);
        options.addOption(estimateOptimizationOption);
        options.addOption(greedyEvaluationSwitchOption);
        options.addOption(reportModeOption);

        String cliInvokeCommand = "java -jar memory-footprint.jar";

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, arguments);
            validateSchemaVersion(line.getOptionValue(schemaVersionOption));
            EvaluationSettings evaluationSettings =
                    new EvaluationSettings(
                            line.getOptionValue(watchFacePathOption),
                            line.getOptionValue(schemaVersionOption));
            if (line.hasOption(activeLimitOption)) {
                evaluationSettings.activeLimitBytes =
                        MemoryFootprint.toBytes(
                                ((Number) line.getParsedOptionValue(activeLimitOption)).intValue());
            }
            if (line.hasOption(ambientLimitOption)) {
                evaluationSettings.ambientLimitBytes =
                        MemoryFootprint.toBytes(
                                ((Number) line.getParsedOptionValue(ambientLimitOption))
                                        .intValue());
            }
            if (line.hasOption(greedyEvaluationSwitchOption)) {
                evaluationSettings.greedyEvaluationSwitch =
                        ((Number) line.getParsedOptionValue(greedyEvaluationSwitchOption))
                                .intValue();
            }
            if (line.hasOption(disableOldStyleClocksOption)) {
                evaluationSettings.supportOldStyleAnalogOrDigitalClock = false;
            }
            if (line.hasOption(disableAmbientDeduplicationOption)) {
                evaluationSettings.deduplicateAmbient = false;
            }
            if (line.hasOption(verboseOption)) {
                evaluationSettings.verbose = true;
            }
            if (line.hasOption(applyV1OffloadLimitationsOption)) {
                evaluationSettings.applyV1OffloadLimitations = true;
            }
            if (line.hasOption(estimateOptimizationOption)) {
                evaluationSettings.estimateOptimization = true;
            }
            if (line.hasOption(reportModeOption)) {
                evaluationSettings.reportMode = true;
            }
            return Optional.of(evaluationSettings);
        } catch (ParseException e) {
            System.out.println("Error: " + e.getLocalizedMessage());
            new HelpFormatter().printHelp(cliInvokeCommand, options, true);
            return Optional.empty();
        }
    }

    private static void validateSchemaVersion(String schemaVersionOption) throws ParseException {
        if (!SUPPORTED_VERSIONS.contains(schemaVersionOption)) {
            throw new ParseException(
                    String.format(
                            "Argument --schema-version has a wrong value. Supported values are %s",
                            String.join(", ", SUPPORTED_VERSIONS)));
        }
    }
}
