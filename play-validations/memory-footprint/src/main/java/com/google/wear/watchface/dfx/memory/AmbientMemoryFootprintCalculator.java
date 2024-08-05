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
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
        this.evaluationSettings = evaluationSettings;
        this.resourceCollector = createResourceCollector();
    }

    private WatchFaceResourceCollector createResourceCollector() {
        if (!evaluationSettings.deduplicateAmbient()) {
            return new WatchFaceResourceCollector(document, resourceMemoryMap, evaluationSettings);
        }
        Map<String, String> dedupMap =
                computeResourceDedupMap(resourceMemoryMap, evaluationSettings);
        return new WatchFaceResourceCollector(document, resourceMemoryMap, evaluationSettings) {

            @Override
            Set<String> collectResources(Node currentNode, VariantConfigValue variant) {
                return super.collectResources(currentNode, variant).stream()
                        .map(dedupMap::get)
                        .collect(toSet());
            }

            @Override
            Set<String> collectResources(Node node) {
                return super.collectResources(node).stream().map(dedupMap::get).collect(toSet());
            }
        };
    }

    private static Map<String, String> computeResourceDedupMap(
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        Map<String, String> sha1ToResource = new HashMap<>();
        Map<String, String> dedupMap = new HashMap<>();
        for (Map.Entry<String, DrawableResourceDetails> entry : resourceMemoryMap.entrySet()) {
            if (entry.getValue().getSha1() == null) {
                dedupMap.put(entry.getKey(), entry.getKey());
            } else {
                String dedupedResource = sha1ToResource.get(entry.getValue().getSha1());
                if (dedupedResource == null) {
                    sha1ToResource.put(entry.getValue().getSha1(), entry.getKey());
                    dedupedResource = entry.getKey();
                } else if (evaluationSettings.isVerbose()) {
                    System.out.printf(
                            "Resource %s is a duplicate of: %s\n", entry.getKey(), dedupedResource);
                }
                dedupMap.put(entry.getKey(), dedupedResource);
            }
        }
        return dedupMap;
    }

    /**
     * Computes the maximum number of layers and the maximum total dynamic resource bytes needed by
     * any style configuration. The result is the sum of the maximum total dynamic resource bytes
     * and the memory needed for all the full screen layers.
     */
    long computeAmbientMemoryFootprint(long screenWidth, long screenHeight) {
        Visitor visitor = new Visitor(/* prevNodeIsDrawnDynamically= */ true, /* numClocks= */ 0);
        visitor.visitNodes(findSceneNode(document));

        long maximumResourceUsage =
                new DynamicNodePerConfigurationFootprintCalculator(
                                document,
                                evaluationSettings,
                                ambientConfigValue,
                                resourceCollector,
                                visitor.drawableNodeConfigTable,
                                this::evaluateResource)
                        .calculateMaxFootprintBytes();

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

    private long evaluateResource(String resource) {
        DrawableResourceDetails details =
                DrawableResourceDetails.findInMap(resourceMemoryMap, resource);
        long imageBytes = details.getBiggestFrameFootprintBytes();

        // If this image can be downsampled then the size is halved.
        if (details.canUseRGB565()) {
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
                    details.canUseRGB565() ? "RGB565" : "ARGB8888");
        }
        return imageBytes;
    }

    private class Visitor {
        private final DrawableNodeConfigTable drawableNodeConfigTable =
                new DrawableNodeConfigTable();

        /**
         * The number of layers needed so far. A layer is needed to render anything underneath,
         * in-between or on top of a Complication / AnalogClock / DigitalClock.
         */
        private int numLayers = 0;

        /** The number of clocks found so far. */
        private int numClocks;

        /** Used for detecting layers. */
        private boolean prevNodeIsDrawnDynamically;

        Visitor(boolean prevNodeIsDrawnDynamically, int numClocks) {
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
                drawableNodeConfigTable.addNodeWithEmptyConfig(node);
            }

            if (nodeName.equals("ComplicationSlot")) {
                // In DWF offloading V1 complications are not offloaded.
                if (evaluationSettings.applyV1OffloadLimitations()) {
                    return false;
                }

                endsLayer = true;
                drawableNodeConfigTable.addNodeWithEmptyConfig(node);
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

        /**
         * Process a <BooleanConfiguration> or a <ListConfiguration> recursively. For layers the
         * maximum number added by any option is recorded. It is expected this computation will work
         * for hierarchical configurations, but this has not been tested since the XML format has
         * not been finalized.
         *
         * <p>Nodes that are drawn dynamically are added to {@link Visitor#drawableNodeConfigTable}
         */
        private void processConfiguration(Node node) {
            int maxConfigNumLayers = 0;

            UserConfigKey userConfigKey = UserConfigKey.fromNode(node);

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

                UserConfigValue userConfigValue = UserConfigValue.fromNode(child);

                Visitor visitor = new Visitor(prevNodeIsDrawnDynamically, numClocks);
                visitor.visitNodes(childNodes.item(i));

                drawableNodeConfigTable.addAll(
                        visitor.drawableNodeConfigTable.withConfig(userConfigKey, userConfigValue));

                if (maxConfigNumLayers < visitor.numLayers) {
                    maxConfigNumLayers = visitor.numLayers;
                }

                // Make sure a layer is generated if we subsequently visit a Part* node.
                prevNodeIsDrawnDynamically = visitor.prevNodeIsDrawnDynamically;
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
