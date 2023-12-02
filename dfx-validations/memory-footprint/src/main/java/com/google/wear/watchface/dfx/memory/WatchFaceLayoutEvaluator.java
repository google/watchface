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

import static com.google.wear.watchface.dfx.memory.FootprintResourceReference.sumDrawableResourceFootprintBytes;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;

import static java.util.stream.Collectors.toSet;

import org.w3c.dom.Document;

import java.util.Map;
import java.util.Set;

class WatchFaceLayoutEvaluator {

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

        VariantMemoryFootprintCalculator variantMemoryFootprintCalculator =
                new VariantMemoryFootprintCalculator(document, resourceMemoryMap, settings);

        long maxInActive =
                variantMemoryFootprintCalculator.evaluateBytes(VariantConfigValue.active(settings));
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
        return sumDrawableResourceFootprintBytes(
                resourceMemoryMap,
                allResourceNames.stream()
                        .map(FootprintResourceReference::totalOf)
                        .collect(toSet()));
    }
}
