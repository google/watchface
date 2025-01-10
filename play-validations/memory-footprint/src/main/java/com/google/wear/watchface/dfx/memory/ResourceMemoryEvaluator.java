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

import static com.google.wear.watchface.dfx.memory.MemoryFootprint.toMB;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.samsung.watchface.WatchFaceXmlValidator;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.w3c.dom.Document;

/** Computes the asset memory footprint for a given watch face. */
public class ResourceMemoryEvaluator {

    private static final int EXIT_STATUS_BAD_ARGUMENTS = 2;
    private static final int EXIT_STATUS_WATCH_FACE_FAILED = 1;

    public static void main(String[] args) {
        Optional<EvaluationSettings> settings = EvaluationSettings.parseFromArguments(args);

        if (!settings.isPresent()) {
            System.exit(EXIT_STATUS_BAD_ARGUMENTS);
        }

        if (settings.get().isReportMode()) {
            evaluateMemoryFootprintJsonReport(settings.get());
        } else {
            evaluateInHumanReadableMode(settings.get());
        }
    }

    private static void evaluateMemoryFootprintJsonReport(EvaluationSettings settings) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Optional<MemoryFootprint> memoryFootprint =
                    evaluateMemoryFootprint(settings).stream().reduce(MemoryFootprint::max);
            if (!memoryFootprint.isPresent()) {
                throw new IllegalArgumentException("The provided watch face has no xml layouts");
            }
            System.out.println(gson.toJson(memoryFootprint.get()));
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String stackTrace = stringWriter.toString();
            ImmutableMap<String, String> errorObject =
                    ImmutableMap.of("error", e.getMessage(), "stackTrace", stackTrace);
            System.out.println(gson.toJson(errorObject));
            System.exit(EXIT_STATUS_WATCH_FACE_FAILED);
        }
    }

    private static void evaluateInHumanReadableMode(EvaluationSettings settings) {
        System.out.println("Starting memory footprint test with " + settings.getWatchFacePath());
        try {
            List<MemoryFootprint> memoryFootprint = evaluateMemoryFootprint(settings);
            // We don't want to print the memory footprint to the manual testers
            if (settings.isVerbose()) {
                printMemoryFootprints(memoryFootprint);
            }

            for (MemoryFootprint footprint : memoryFootprint) {
                footprint.validate(settings);
            }

            System.out.println("Test report:");
            System.out.println(
                    TestResultFormatter.formatSuccess(
                            "Watch Face has passed the memory footprint test."));
        } catch (TestFailedException e) {
            // TestFailedExceptions are expected. They are part of the test report and should be
            // human readable. Do not print the stack trace.
            System.out.println("Test report:");
            System.out.println(TestResultFormatter.formatFailure(e.getMessage()));
            System.exit(EXIT_STATUS_WATCH_FACE_FAILED);
        } catch (Exception e) {
            // Any other exception is unexpected. Print the stack trace and a human-readable message
            // telling the tester they should seek assistance.
            System.out.println(TestResultFormatter.formatException(e.getMessage()));
            e.printStackTrace();
            System.exit(EXIT_STATUS_WATCH_FACE_FAILED);
        }
    }

    /**
     * Parses a watch face package and evaluates the memory footprint for all of its layouts.
     *
     * @param evaluationSettings the settings object for running the watch face evaluation.
     * @return the list of memory footprints, one for each layout supported by the watch face.
     */
    static List<MemoryFootprint> evaluateMemoryFootprint(EvaluationSettings evaluationSettings) {
        try (InputPackage inputPackage = InputPackage.open(evaluationSettings.getWatchFacePath())) {

            AndroidManifest manifest = inputPackage.getManifest();

            WatchFaceData watchFaceData =
                    WatchFaceData.fromResourcesStream(
                            inputPackage.getWatchFaceFiles(), evaluationSettings);
            if (!evaluationSettings.isHoneyfaceMode()) {
                String manifestWffVersion = String.valueOf(manifest.getWffVersion());
                String cliWffVersion = evaluationSettings.getSchemaVersion();
                if (cliWffVersion != null
                        && !cliWffVersion.equals(manifestWffVersion)
                        && !evaluationSettings.isReportMode()) {
                    System.out.printf(
                            "Warning: Specified WFF version (%s) "
                                    + "does not match version in manifest (%s)%n",
                            cliWffVersion, manifestWffVersion);
                }
                validateFormat(
                        watchFaceData, cliWffVersion != null ? cliWffVersion : manifestWffVersion);
            }
            return watchFaceData.watchFaceDocuments.stream()
                    .map(
                            watchFaceDocument ->
                                    evaluateWatchFaceForLayout(
                                            watchFaceData.resourceDetailsMap,
                                            watchFaceDocument,
                                            evaluationSettings))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static MemoryFootprint evaluateWatchFaceForLayout(
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            Document currentLayout,
            EvaluationSettings settings) {
        return WatchFaceLayoutEvaluator.evaluate(currentLayout, resourceMemoryMap, settings);
    }

    /**
     * Runs the watch face format XSD validation.
     *
     * @param watchFaceData the watch face data containing the watchface xml documents.
     * @param watchFaceFormatVersion the watch face format version.
     * @throws TestFailedException if the watch face does not comply to the format version.
     */
    private static void validateFormat(WatchFaceData watchFaceData, String watchFaceFormatVersion) {
        WatchFaceXmlValidator xmlValidator = new WatchFaceXmlValidator();
        for (Document watchFaceDocument : watchFaceData.watchFaceDocuments) {
            boolean documentHasValidSchema =
                    xmlValidator.validate(watchFaceDocument, watchFaceFormatVersion);
            if (!documentHasValidSchema) {
                throw new TestFailedException(
                        "Watch Face has syntactic errors and cannot be parsed.");
            }
        }
    }

    /**
     * Prints the number of layouts that the watch face supports, the total memory footprint of the
     * watch face, the maximum memory footprint in ambient mode and in the active mode.
     *
     * @param footprintsForLayouts the memory footprint of all the layouts that the watch face
     *     supports.
     */
    private static void printMemoryFootprints(List<MemoryFootprint> footprintsForLayouts) {
        Optional<MemoryFootprint> maxMemoryFootprint =
                footprintsForLayouts.stream().reduce(MemoryFootprint::max);

        if (!maxMemoryFootprint.isPresent()) {
            System.out.println("No memory footprints found for the watch face");
            return;
        }

        System.out.printf("Number of supported layouts: %d%n", footprintsForLayouts.size());

        System.out.printf(
                "Total images memory footprint: %.2f MB%n",
                toMB(maxMemoryFootprint.get().getTotalBytes()));
        System.out.printf(
                "Max memory footprint in active: %.2f MB%n",
                toMB(maxMemoryFootprint.get().getMaxActiveBytes()));
        System.out.printf(
                "Max memory footprint in ambient: %.2f MB%n",
                toMB(maxMemoryFootprint.get().getMaxAmbientBytes()));
    }
}
