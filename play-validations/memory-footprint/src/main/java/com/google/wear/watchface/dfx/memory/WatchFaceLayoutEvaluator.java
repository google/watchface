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

import static com.google.wear.watchface.dfx.memory.DrawableResourceDetails.findInMap;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.w3c.dom.Document;

@SuppressWarnings("KotlinInternal")
public class WatchFaceLayoutEvaluator {
    public static List<MemoryFootprint> evaluate(EvaluationSettings evaluationSettings) {
        try (InputPackage inputPackage = InputPackage.open(evaluationSettings.getWatchFacePath())) {
            WatchFaceData watchFaceData =
                    WatchFaceData.fromResourcesStream(
                            inputPackage.getWatchFaceFiles(), evaluationSettings);
            AndroidManifest manifest = inputPackage.getManifest();
            String wffVersion = manifest == null ? null : String.valueOf(manifest.getWffVersion());
            return watchFaceData.getWatchFaceDocuments().stream()
                    .map(
                            watchFaceDocument ->
                                    getMemoryFootprint(
                                            watchFaceDocument,
                                            watchFaceData.getResourceDetailsMap(),
                                            evaluationSettings))
                    .collect(Collectors.toList());
        }
    }

    static MemoryFootprint evaluate(
            Document currentLayout,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings settings) {
        long startTime = System.currentTimeMillis();
        MemoryFootprint footprint = getMemoryFootprint(currentLayout, resourceMemoryMap, settings);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        if (settings.isVerbose()) {
            System.out.printf("Finished memory computation in %d ms%n", duration);
        }
        return footprint;
    }

    private static MemoryFootprint getMemoryFootprint(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings settings) {
        if (settings.estimateOptimization()) {
            new OptimizationEstimator(document, resourceMemoryMap, settings)
                    .estimateOptimizations();
        }

        long totalFootprint = computeTotalMemory(document, resourceMemoryMap, settings);

        long maxInActive =
                new ActiveMemoryFootprintCalculator(document, resourceMemoryMap, settings)
                        .computeActiveMemoryFootprint();
        long maxInAmbient =
                new AmbientMemoryFootprintCalculator(document, resourceMemoryMap, settings)
                        .computeAmbientMemoryFootprint(450, 450);

        return new MemoryFootprint(
                /* totalBytes= */ totalFootprint,
                // greedy evaluation might lead to more than the total, but the footprint in
                // active or ambient should not exceed total
                /* maxActiveBytes= */ Math.min(maxInActive, totalFootprint),
                /* maxAmbientBytes= */ maxInAmbient);
    }

    /**
     * Evaluates the total memory footprint for the current layout, of all the assets referenced by
     * the watch face.
     */
    private static long computeTotalMemory(
            Document currentLayout,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        WatchFaceResourceCollector resourceCollector =
                new WatchFaceResourceCollector(
                        currentLayout, resourceMemoryMap, evaluationSettings);
        Set<String> allResourceNames =
                resourceCollector.collectResources(findSceneNode(currentLayout));

        return allResourceNames.stream()
                .mapToLong(
                        resourceName ->
                                findInMap(resourceMemoryMap, resourceName).getTotalFootprintBytes())
                .sum();
    }
}
