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

import static com.google.wear.watchface.dfx.memory.WatchFaceData.SYSTEM_DEFAULT_FONT;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.childrenStream;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isBitmapFont;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isFont;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@SuppressWarnings("KotlinInternal")
class WatchFaceResourceCollector {
    private final Map<String, Node> bitmapFontDefinitions;
    private final Map<String, DrawableResourceDetails> resourceMemoryMap;
    private final EvaluationSettings evaluationSettings;

    WatchFaceResourceCollector(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        this.resourceMemoryMap = resourceMemoryMap;
        this.evaluationSettings = evaluationSettings;
        try {
            bitmapFontDefinitions = parseBitmapFontDefinitions(document, evaluationSettings);
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Could not parse bitmap font definitions", e);
        }
    }

    /**
     * Recursively traverses the watch face and collects all the drawable resources contained inside
     * the current node and all its descendants, ignoring variant.
     *
     * @param node the root node from which the resources are collected.
     * @return the set of resources referenced by the current node and its sub-nodes.
     */
    Set<String> collectResources(Node node) {
        return collectResources(node, null);
    }

    /**
     * Recursively traverses the watch face and collects all the drawable resources contained inside
     * the current node and all its descendants, under the given variant.
     *
     * @param currentNode the root node from which the resources are collected.
     * @param variant the variant under which the resources must be visible. If null, then variant
     *     is ignored and all nodes will be evaluated.
     * @return the set of resources referenced by the current node and its sub-nodes.
     */
    Set<String> collectResources(Node currentNode, VariantConfigValue variant) {
        if (variant != null && variant.isNodeSkipped(currentNode)) {
            return emptySet();
        } else if (isFont(currentNode)) {
            return collectFontResources(currentNode);
        } else if (isBitmapFont(currentNode)) {
            return collectBitmapFontResources(currentNode, variant);
        } else if (isLeafNodeWithResource(currentNode)) {
            return collectLeafNodeResourceReferences(currentNode);
        } else {
            return collectChildrenResources(currentNode, variant);
        }
    }

    /** Collects the resources referenced by a font. */
    private Set<String> collectFontResources(Node currentNode) {
        Optional<String> fontFamily = getNodeAttribute(currentNode, "family");
        if (!fontFamily.isPresent()) {
            throw new TestFailedException("Font in Scene does not have a family");
        }

        Set<String> resourceReferences = new HashSet<>();
        String family = fontFamily.get();
        if (!resourceMemoryMap.containsKey(family)) {
            if (evaluationSettings.isVerbose()) {
                System.out.println("Using system default font for unknown font family " + family);
            }
            family = SYSTEM_DEFAULT_FONT;
        }
        resourceReferences.add(family);
        return resourceReferences;
    }

    /**
     * Collects the resources referenced by a bitmap font. These resources are the ones defined by
     * the top-level BitmapFont for the given currentNode's family plus the resources defined inside
     * the children of the bitmap font (InlineImages).
     */
    private Set<String> collectBitmapFontResources(Node currentNode, VariantConfigValue variant) {
        Optional<String> bitmapFontFamily = getNodeAttribute(currentNode, "family");
        if (!bitmapFontFamily.isPresent()) {
            throw new TestFailedException("Bitmap Font in Scene does not have a family");
        }
        if (!bitmapFontDefinitions.containsKey(bitmapFontFamily.get())) {
            throw new TestFailedException(
                    String.format(
                            "Could not find bitmap font family definition for font family %s",
                            bitmapFontFamily.get()));
        }
        // Collect the resources from the elements inside the top-level BitmapFont node that is
        // referenced by the current node.
        Node bitmapFontDefinition = bitmapFontDefinitions.get(bitmapFontFamily.get());
        Stream<String> bitmapFontResources =
                childrenStream(bitmapFontDefinition)
                        .flatMap(
                                bitmapFontDefinitionNode ->
                                        collectLeafNodeResourceReferences(bitmapFontDefinitionNode)
                                                .stream());
        // Collect the resources referenced by the current bitmap font tag, such as InlineImage
        Stream<String> inlineImagesResources =
                collectChildrenResources(currentNode, variant).stream();
        return Stream.concat(bitmapFontResources, inlineImagesResources).collect(toSet());
    }

    /**
     * Collects the drawable resource referenced by the current leaf node. A leaf node is a node
     * that contains a resource reference and does not contain any children with resource
     * references.
     */
    private Set<String> collectLeafNodeResourceReferences(Node currentNode) {
        Set<String> resourceReferences = new HashSet<>();
        // Collect resources, but ignore complication icon references.
        getNodeAttribute(currentNode, "resource")
                .filter(this::resourceIsValid)
                .ifPresent(resourceReferences::add);
        getNodeAttribute(currentNode, "thumbnail")
                .filter(this::resourceIsValid)
                .ifPresent(resourceReferences::add);
        return resourceReferences;
    }

    private boolean resourceIsValid(String resource) {
        boolean isNotExpression = !(resource.startsWith("[") && resource.endsWith("]"));
        if (evaluationSettings.isHoneyfaceMode()) {
            // in honeyface mode, filter out empty resource references
            return isNotExpression && resource.length() != 0;
        } else {
            // in DWF mode, empty resources are a bug in the watch face and should not be filtered
            // out, but be handled at a higher level
            return isNotExpression;
        }
    }

    private Set<String> collectChildrenResources(Node currentNode, VariantConfigValue variant) {
        return childrenStream(currentNode)
                .flatMap(node -> collectResources(node, variant).stream())
                .collect(toSet());
    }

    /** Checks if the current node is a leaf node that uses a drawable asset. */
    private static boolean isLeafNodeWithResource(Node currentNode) {
        String nodeName = currentNode.getNodeName();
        return nodeName.equals("Image")
                || nodeName.equals("AnimatedImage")
                || nodeName.equals("HourHand")
                || nodeName.equals("InlineImage")
                || nodeName.equals("Thumbnail")
                || nodeName.equals("MinuteHand")
                || nodeName.equals("SecondHand");
    }

    private static Map<String, Node> parseBitmapFontDefinitions(
            Document document, EvaluationSettings evaluationSettings)
            throws XPathExpressionException {
        ImmutableMap.Builder<String, Node> resourcesForFontsMapBuilder = ImmutableMap.builder();
        XPathExpression xpath =
                XPathFactory.newInstance()
                        .newXPath()
                        .compile(
                                String.format(
                                        "/%s/BitmapFonts/BitmapFont",
                                        WatchFaceDocuments.getWatchFaceRootNode(
                                                evaluationSettings)));
        NodeList nodeList = (NodeList) xpath.evaluate(document, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentElement = nodeList.item(i);
            getNodeAttribute(currentElement, "name")
                    .ifPresent(
                            fontName -> resourcesForFontsMapBuilder.put(fontName, currentElement));
        }
        return resourcesForFontsMapBuilder.build();
    }
}
