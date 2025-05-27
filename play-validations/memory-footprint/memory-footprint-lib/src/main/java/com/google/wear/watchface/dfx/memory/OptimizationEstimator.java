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

import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findBitmapFontsNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import static java.lang.Math.max;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Estimates the effects of optimizing the DWF. */
class OptimizationEstimator {
    private final Document document;
    private final Map<String, DrawableResourceDetails> resourceMemoryMap;
    private final EvaluationSettings evaluationSettings;

    OptimizationEstimator(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        this.document = document;
        this.resourceMemoryMap = resourceMemoryMap;
        this.evaluationSettings = evaluationSettings;
    }

    private class Metadata {
        final Map<DrawableResourceDetails, HashSet<Node>> resourceToImageNodesMap = new HashMap<>();
        final Map<String, HashSet<Node>> bitmapFontFamilyToNodesMap = new HashMap<>();
    }

    /**
     * Analyzes the DWF, looking for optimizations. Size changes ro images are written to
     * DrawableResourceDetails in memory, but the actual optimizations are not applied.
     */
    void estimateOptimizations() {
        try {
            // Initially collect metadata about the scene, linking drawables and bitmap font
            // families to the nodes that are using them.
            Metadata metadata = new Metadata();
            collectMetadata(findSceneNode(document), metadata);

            // Then estimate the effects of various optimizations.
            long totalSaving = estimateBitmapFontsOptimizations(metadata);
            totalSaving += estimateImageNodesDrawableOptimization(metadata);

            if (evaluationSettings.isVerbose()) {
                System.out.println("Total saving: " + totalSaving);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /** Visits node and any children, populating {@link Metadata}. */
    private void collectMetadata(Node node, Metadata metadata) {
        switch (node.getNodeName()) {
            case "BitmapFont":
                {
                    Optional<String> family = getNodeAttribute(node, "family");
                    if (family.isPresent()) {
                        metadata.bitmapFontFamilyToNodesMap
                                .computeIfAbsent(family.get(), d -> new HashSet<Node>())
                                .add(node);
                    }
                    break;
                }

                // TODO(b/308123875): Support PartAnimatedImage
            case "PartImage":
                {
                    DrawableResourceDetails drawable = findImageNodeDrawableResourceDetails(node);
                    if (drawable != null) {
                        metadata.resourceToImageNodesMap
                                .computeIfAbsent(drawable, d -> new HashSet<Node>())
                                .add(node);
                    }
                }
                break;
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectMetadata(children.item(i), metadata);
        }
    }

    /** If there's a a BitmapFonts node, examine each font for possible savings. */
    private long estimateBitmapFontsOptimizations(Metadata metadata) {
        Node node = findBitmapFontsNode(document);
        if (node == null) {
            return 0;
        }

        int total = 0;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("BitmapFont")) {
                total += estimateIndividualBitmapFontOptimization(child, metadata);
            }
        }
        return total;
    }

    private long estimateIndividualBitmapFontOptimization(Node node, Metadata metadata) {
        String fontFamily = getNodeAttribute(node, "name").orElse("");
        Set<Node> nodesUsingFontFamily = metadata.bitmapFontFamilyToNodesMap.get(fontFamily);

        // Compute the maximum (vertical) size at which this font is requested.
        int maxHeight = 0;
        if (nodesUsingFontFamily != null) {
            for (Node nodeUsingFontFamily : nodesUsingFontFamily) {
                int size =
                        Integer.parseInt(getNodeAttribute(nodeUsingFontFamily, "size").orElse("0"));
                maxHeight = max(maxHeight, size);
            }
        }

        int resourceCount = 0;
        long resizedWidth = 0;
        long unoptimizedSize = 0;
        long optimizedSize = 0;
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            Optional<DrawableResourceDetails> maybeDrawable =
                    getNodeAttribute(child, "resource").map(resourceMemoryMap::get);

            if (!maybeDrawable.isPresent()) {
                continue;
            }

            DrawableResourceDetails drawable = maybeDrawable.get();
            DrawableResourceDetails.Bounds bounds = drawable.getBounds();
            if (bounds == null) {
                continue;
            }
            resourceCount++;

            // Here we assume that each glyph is scaled to the maximum requested size and that it
            // can be tightly cropped. To support this renderer will need to support an x & y
            // offset per glyph.
            float aspectRatio = (float) drawable.getWidth() / (float) drawable.getHeight();
            float maxWidth = maxHeight * aspectRatio;
            float scaleX = maxWidth / (float) drawable.getWidth();
            float scaleY = (float) maxHeight / (float) drawable.getHeight();
            int newWidth = (int) ((float) bounds.getWidth() * scaleX);
            int newHeight = (int) ((float) bounds.getHeight() * scaleY);
            long newSize = (long) newWidth * (long) newHeight * 4;

            optimizedSize += newSize;
            drawable.setOptimizedSizeAndBytes(newSize, newWidth, newHeight);
        }

        if (maxHeight == 0) {
            if (evaluationSettings.isVerbose()) {
                System.out.printf(
                        "BitmapFont: %s unused. Saving %s\n", fontFamily, unoptimizedSize);
            }
            return unoptimizedSize;
        }

        return unoptimizedSize - optimizedSize;
    }

    /** Estimate optimzing all drawables referenced by ImageNodes (cropping and scaling). */
    private long estimateImageNodesDrawableOptimization(Metadata metadata) {
        long total = 0;
        for (Map.Entry<DrawableResourceDetails, HashSet<Node>> entry :
                metadata.resourceToImageNodesMap.entrySet()) {
            float maxRendredWidth = 0;
            float maxRendredHeight = 0;

            for (Node node : entry.getValue()) {
                maxRendredWidth =
                        max(
                                maxRendredWidth,
                                Float.parseFloat(getNodeAttribute(node, "width").orElse("0")));

                maxRendredHeight =
                        max(
                                maxRendredHeight,
                                Float.parseFloat(getNodeAttribute(node, "height").orElse("0")));
            }

            total +=
                    estimateDrawableOptimization(entry.getKey(), maxRendredWidth, maxRendredHeight);
        }

        return total;
    }

    private long estimateDrawableOptimization(
            DrawableResourceDetails drawable, float maxRenderedWidth, float maxRenderedHeight) {
        DrawableResourceDetails.Bounds bounds = drawable.getBounds();
        if (bounds == null) {
            return 0;
        }

        long maxImageWidth = drawable.getWidth();
        long maxImageHeight = drawable.getHeight();

        // Compute the x & y scale factors from the maximum rendered width & height to the image
        // width & height.
        float scaleX = maxRenderedWidth / (float) maxImageWidth;
        float scaleY = maxRenderedHeight / (float) maxImageHeight;

        // Apply this scale to the visible bounds. I.e. transforming this to screen space.
        float boundsL = (float) floor(scaleX * (float) bounds.left);
        float boundsT = (float) floor(scaleY * (float) bounds.top);
        float boundsR = (float) ceil(scaleX * (float) bounds.right);
        float boundsB = (float) ceil(scaleY * (float) bounds.bottom);

        // From the screen space bounds, compute the new width, height and the bytes saved.
        float newWidth = boundsR - boundsL;
        float newHeight = boundsB - boundsT;
        long oldSize = 4 * maxImageWidth * maxImageHeight;
        long newSize = 4 * ((long) newWidth) * ((long) newHeight);

        drawable.setOptimizedSizeAndBytes(newSize, (int) newWidth, (int) newHeight);

        long saving = oldSize - newSize;
        saving *= 4;
        if (saving > 0 && evaluationSettings.isVerbose()) {
            System.out.printf(
                    "Resize: %s %s x %s [%s, %s - %s, %s] -> %s x %s Saving: %s\n",
                    drawable.getName(),
                    maxImageWidth,
                    maxImageHeight,
                    bounds.left,
                    bounds.top,
                    bounds.right,
                    bounds.bottom,
                    (int) newWidth,
                    (int) newHeight,
                    saving);
        }

        return saving;
    }

    private DrawableResourceDetails findImageNodeDrawableResourceDetails(Node imageNode) {
        NodeList children = imageNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("Image")) {
                return getNodeAttribute(child, "resource").map(resourceMemoryMap::get).orElse(null);
            }
        }
        return null;
    }
}
