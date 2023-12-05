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

import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute;

import static java.lang.Math.min;

import com.google.common.collect.ImmutableList;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes the memory footprint of a watch face in ambient.
 *
 * <p>Assumptions: - Everything except for <AnalogClock>, <DigitalClock> and <ComplicationSlot> is
 * composited down into one or more full screen layers. - <BooleanConfiguration> and
 * <ListConfiguration> nodes do not have side effects out side of their child nodes. I.e. they're
 * not included in any expressions. - The renderer only needs the resources for the current style.
 */
class AmbientMemoryFootprintCalculator {
    private final VariantConfigValue ambientConfigValue;
    private final Document document;
    private final Map<String, DrawableResourceDetails> resourceMemoryMap;
    /** Maps a resource to the first resource found with the same sha-1. */
    private final Map<String, String> resourceDedupMap = new HashMap<>();
    private final WatchFaceResourceCollector resourceCollector;
    private final EvaluationSettings evaluationSettings;

    // The list of time related sources from
    // viewer/src/main/java/com/samsung/watchface/WatchFaceDocument.java filtered to only include
    // sources that contain minutes since in ambient we only render once per minute and things with
    // hours are treated as static and baked into a layer.
    private static final List<String> OFFLOADABLE_TIME_RELATED_DATA_SOURCES =
            ImmutableList.of(
                    "MINUTE",
                    "MINUTE_SECOND",
                    "MINUTE_Z",
                    "HOUR_0_11_MINUTE",
                    "HOUR_0_23_MINUTE",
                    "HOUR_1_12_MINUTE",
                    "HOUR_1_24_MINUTE",
                    "UTC_TIMESTAMP");

    private static final List<String> ICU_FUNCTIONS = ImmutableList.of("icuText", "icuBestText");

    AmbientMemoryFootprintCalculator(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        this.ambientConfigValue = VariantConfigValue.ambient(evaluationSettings);
        this.document = document;
        this.resourceMemoryMap = resourceMemoryMap;
        this.resourceCollector =
                new WatchFaceResourceCollector(document, resourceMemoryMap, evaluationSettings);
        this.evaluationSettings = evaluationSettings;

        if (evaluationSettings.deduplicateAmbient()) {
            computeResourceDedupMap();
        }
    }

    private void computeResourceDedupMap() {
        HashMap<String, String> sha1ToResource = new HashMap<>();
        for (Map.Entry<String, DrawableResourceDetails> entry : resourceMemoryMap.entrySet()) {
            String dedupedResource = sha1ToResource.get(entry.getValue().getSha1());
            if (dedupedResource == null) {
                sha1ToResource.put(entry.getValue().getSha1(), entry.getKey());
                dedupedResource = entry.getKey();
            } else if (evaluationSettings.isVerbose()) {
                System.out.printf(
                        "Resource %s is a duplicate of: %s\n", entry.getKey(), dedupedResource);
            }
            resourceDedupMap.put(entry.getKey(), dedupedResource);
        }
    }

    /**
     * Computes the maximum number of layers and the maximum total dynamic resource bytes needed by
     * any style configuration. The result is the sum of the maximum total dynamic resource bytes
     * and the memory needed for all the full screen layers.
     */
    long computeAmbientMemoryFootprint(long screenWidth, long screenHeight) {
        PerConfigurationDynamicResources perConfigurationDynamicResources =
                new PerConfigurationDynamicResources();
        Visitor visitor =
                new Visitor(
                        perConfigurationDynamicResources, /* prevNodeIsDrawnDynamically= */ true,
                        /* numClocks= */ 0);
        visitor.visitNodes(findSceneNode(document));

        long maximumResourceUsage =
                perConfigurationDynamicResources.computeMaximumResourceUsage(
                        visitor.optionResources);

        // There's always at least one layer, even if all the Part* nodes are hidden.
        if (visitor.numLayers == 0) {
            visitor.numLayers = 1;
        }

        // In V1 we support a maximum of 2 layers and 2 clocks.
        if (evaluationSettings.applyV1OffloadLimitations()) {
            visitor.numLayers = min(visitor.numLayers, 2);
        }

        if (evaluationSettings.isVerbose()) {
            System.out.printf(
                    "Number of layers: %s, maximumResourceUsage: %s\n",
                    visitor.numLayers, maximumResourceUsage);
        }

        return (screenWidth * screenHeight * visitor.numLayers * 4) + maximumResourceUsage;
    }

    /** Collects the set of resources each option uses in each configuration. */
    private class PerConfigurationDynamicResources {
        private final List<List<Set<String>>> resourcesPerConfigurationPerOption =
                new ArrayList<>();

        void addConfigurationResources(List<Set<String>> perOptionResources) {
            resourcesPerConfigurationPerOption.add(perOptionResources);
        }

        /**
         * Given the resources used outside of a configuration, computes the maximum memory used by
         * the resources for any permutation of options.
         */
        long computeMaximumResourceUsage(Set<String> topLevelResources) {
            List<Set<String>> sets = new ArrayList<>();
            sets.add(topLevelResources);

            // Compute all combinations of resources for every setting.
            for (List<Set<String>> optionSets : resourcesPerConfigurationPerOption) {
                List<Set<String>> combinedSets = new ArrayList<>();

                for (Set<String> setA : sets) {
                    for (Set<String> setB : optionSets) {
                        Set<String> combined = new HashSet<>(setA);
                        combined.addAll(setB);
                        combinedSets.add(combined);
                    }
                }

                sets = combinedSets;
            }

            // Compute the maximum memory needed by any set of resources.
            long maxTotal = 0;
            int numResources = 0;
            long maxResourceSize = 0;
            for (Set<String> set : sets) {
                long total = 0;
                for (String resource : set) {
                    DrawableResourceDetails details = resourceMemoryMap.get(resource);
                    if (details == null) {
                        throw new TestFailedException(
                                String.format(
                                        "Asset %s was not found in the watch face package",
                                        resource));
                    }
                    long imageBytes = details.getBiggestFrameFootprintBytes();

                    // If this image can be downsampled then the size is halved.
                    if (details.canUseARGB4444()) {
                        imageBytes /= 2;
                    }

                    if (evaluationSettings.isVerbose()) {
                        System.out.printf(
                                "Counting resource %s; %s bytes, %s mb, %s x %s %s%n",
                                resource,
                                details.getBiggestFrameFootprintBytes(),
                                ((double) imageBytes) / 1024 / 1024,
                                details.getWidth(),
                                details.getHeight(),
                                details.canUseARGB4444() ? "ARGB4444" : "ARGB8888" );
                    }
                    total += imageBytes;
                    if (maxResourceSize < imageBytes) {
                        maxResourceSize = imageBytes;
                    }
                }
                if (maxTotal < total) {
                    maxTotal = total;
                }
                if (numResources < set.size()) {
                    numResources = set.size();
                }
            }

            if (evaluationSettings.isVerbose()) {
                System.out.printf("Resource count %s average size %s max size %s\n", numResources,
                        maxTotal / numResources, maxResourceSize);
            }

            return maxTotal;
        }
    }

    private class Visitor {
        private final PerConfigurationDynamicResources perConfigurationDynamicResources;

        /**
         * The number of layers needed so far. A layer is needed to render anything underneath,
         * in-between or on top of a Complication / AnalogClock / DigitalClock.
         */
        private int numLayers = 0;

        /** The number of clocks found so far. */
        private int numClocks = 0;

        /** Used for detecting layers. */
        private boolean prevNodeIsDrawnDynamically;

        /**
         * Resources for the current option. NB the top level is deemed to be a (degenerate) setting
         * with only one option.
         */
        private final Set<String> optionResources = new HashSet<>();

        Visitor(
                PerConfigurationDynamicResources perConfigurationDynamicResources,
                boolean prevNodeIsDrawnDynamically,
                int numClocks) {
            this.perConfigurationDynamicResources = perConfigurationDynamicResources;
            this.prevNodeIsDrawnDynamically = prevNodeIsDrawnDynamically;
            this.numClocks = numClocks;
        }

        /**
         * Calls {@link #visit} for node and every child, as long as a node isn't skipped in
         * ambient. See: {@link VariantConfigValue#isNodeSkipped}.
         */
        void visitNodes(Node node) {
            if (ambientConfigValue.isNodeSkipped(node)) {
                return;
            }

            if (visit(node)) {
                NodeList children = node.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    visitNodes(children.item(i));
                }
            }
        }

        /** Visits {@code node} and returns true if it should be called for any child nodes. */
        private boolean visit(Node node) {
            String nodeName = node.getNodeName();
            if (nodeName.equals("BooleanConfiguration") || nodeName.equals("ListConfiguration")) {
                processConfiguration(node);
                return false;
            }

            boolean endsLayer = false;

            // If it's a clock, then maybe end the layer and collect resources.
            if (nodeName.equals("AnalogClock")
                    || nodeName.equals("DigitalClock")
                    || isNodeOldStyleAnalogClock(node)
                    || isNodeOldStyleDigitalClock(node)) {
                numClocks++;

                // In DWF offloading V1 we support a maximum of two offloaded clocks.
                if (numClocks > 2 && evaluationSettings.applyV1OffloadLimitations()) {
                    return false;
                }

                endsLayer = true;
                collectResources(node);
            }

            if (nodeName.equals("ComplicationSlot")) {
                // In DWF offloading V1 complications are not offloaded.
                if (evaluationSettings.applyV1OffloadLimitations()) {
                    return false;
                }

                endsLayer = true;
                collectResources(node);
            }

            if (endsLayer) {
                if (!prevNodeIsDrawnDynamically && evaluationSettings.isVerbose()) {
                    System.out.printf("Layer %s ends at: %s\n", numLayers, pathToString(node));
                }

                // Any subsequent Part* nodes need a layer.
                prevNodeIsDrawnDynamically = true;
                return false;
            } else {
                checkIfNewLayerIsRequired(node);
            }

            return true;
        }

        /** Collects the resources under node, deduplicating if required. */
        private void collectResources(Node node) {
            for (String resource : resourceCollector.collectResources(node, ambientConfigValue)) {
                if (evaluationSettings.deduplicateAmbient()) {
                    optionResources.add(resourceDedupMap.get(resource));
                } else {
                    optionResources.add(resource);
                }
            }
        }

        /**
         * Process a <BooleanConfiguration> or a <ListConfiguration> recursively. For layers the
         * maximum number added by any option is recorded. It is expected this computation will work
         * for hierarchical configurations, but this has not been tested since the XML format has
         * not been finalized.
         *
         * <p>Resources are added to {@link Visitor#perConfigurationDynamicResources}
         */
        private void processConfiguration(Node node) {
            long maxConfigNumLayers = 0;
            List<Set<String>> perOptionResources = new ArrayList<>();

            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                switch (child.getNodeName()) {
                    case "BooleanOption":
                    case "ListOption":
                        break;

                    default:
                        continue;
                }

                Visitor visitor = new Visitor(
                        perConfigurationDynamicResources, prevNodeIsDrawnDynamically, numClocks);
                visitor.visitNodes(childNodes.item(i));

                if (maxConfigNumLayers < visitor.numLayers) {
                    maxConfigNumLayers = visitor.numLayers;
                }

                if (!visitor.optionResources.isEmpty()) {
                    perOptionResources.add(visitor.optionResources);
                }

                // Make sure a layer is generated if we subsequently visit a Part* node.
                prevNodeIsDrawnDynamically = visitor.prevNodeIsDrawnDynamically;
            }

            if (!perOptionResources.isEmpty()) {
                perConfigurationDynamicResources.addConfigurationResources(perOptionResources);
            }

            numLayers += maxConfigNumLayers;
        }

        /**
         * We assume that in ambient only <AnalogClock>, <DigitalClock> and <ComplicationSlot> need
         * to be drawn dynamically and that everything else is static.
         *
         * <p>A layer must be generated for any static render nodes before, in-between or after a
         * dynamic node.
         *
         * <p>We assume that every static drawable node will be encapsulated in a Part* node.
         */
        private void checkIfNewLayerIsRequired(Node node) {
            if (!node.getNodeName().startsWith("Part")) {
                return;
            }

            // If the previous node in the draw order was <AnalogClock> or
            // <ComplicationSlot> or <DigitalClock> then we need a new layer.
            if (prevNodeIsDrawnDynamically) {
                numLayers++;
                if (evaluationSettings.isVerbose()) {
                    System.out.printf("Layer %s starts at: %s\n", numLayers, pathToString(node));
                }
                prevNodeIsDrawnDynamically = false;
            }
        }

        /**
         * Recognizes an old style AnalogClock node, where a Part* Node contains an angle transform
         * based on the time.
         */
        private boolean isNodeOldStyleAnalogClock(Node node) {
            if (!evaluationSettings.supportOldStyleAnalogOrDigitalClock()) {
                return false;
            }

            if (!node.getNodeName().startsWith("Part")) {
                return false;
            }

            NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node child = childNodes.item(i);
                if (child.getNodeName().equals("Transform")) {
                    if (!getNodeAttribute(child, "target").orElse("").equals("angle")) {
                        continue;
                    }

                    String valueString = getNodeAttribute(child, "value").orElse("");
                    for (String property : OFFLOADABLE_TIME_RELATED_DATA_SOURCES) {
                        if (valueString.contains(property)) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * Recognizes an old style DigitalClock node, where a PartText Node contains either an icu
         * expression or a time based expression.
         */
        private boolean isNodeOldStyleDigitalClock(Node node) {
            if (!evaluationSettings.supportOldStyleAnalogOrDigitalClock()) {
                return false;
            }

            if (!node.getNodeName().equals("PartText")) {
                return false;
            }

            // Recursively visit node and every child under it, checking for any that have time
            // based expressions.
            ArrayDeque<Node> stack = new ArrayDeque<>();
            stack.push(node);

            while (!stack.isEmpty()) {
                Node n = stack.pop();
                if (nodeHasTimeBasedExpression(n)) {
                    return true;
                }

                NodeList children = n.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    stack.push(children.item(i));
                }
            }

            return false;
        }
    }

    private static boolean nodeHasTimeBasedExpression(Node node) {
        String expression = getNodeAttribute(node, "expression").orElse("");
        if (expression.isEmpty()) {
            return false;
        }
        for (String icuFunction : ICU_FUNCTIONS) {
            if (expression.contains(icuFunction)) {
                return true;
            }
            for (String property : OFFLOADABLE_TIME_RELATED_DATA_SOURCES) {
                if (expression.contains(property)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getNodeName(Node node) {
        String path = node.getNodeName();
        String id = getNodeAttribute(node, "id").orElse("");
        if (!id.isEmpty()) {
            path = path + " id = \"" + id + "\"";
        }

        String name = getNodeAttribute(node, "name").orElse("");
        if (!name.isEmpty()) {
            path = path + " name = \"" + name + "\"";
        }

        return path;
    }

    private static String pathToString(Node node) {
        String path = getNodeName(node);
        while (true) {
            node = node.getParentNode();
            if (node == null) {
                break;
            }

            path = getNodeName(node) + "/" + path;
        }
        return path;
    }
}
