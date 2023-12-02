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
import static com.google.wear.watchface.dfx.memory.UserConfigValue.SupportedConfigs.isValidUserConfigNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.childrenStream;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isBitmapFont;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isDrawableNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isFont;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isPartAnimatedImage;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isPartImage;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;

/** Computes the memory footprint of a watch face for a single variant. */
class VariantMemoryFootprintCalculator {
    private final Document document;
    private final Map<String, DrawableResourceDetails> resourceMemoryMap;
    private final EvaluationSettings evaluationSettings;
    private final Node sceneNode;
    private final WatchFaceResourceCollector resourceCollector;

    VariantMemoryFootprintCalculator(
            Document document,
            Map<String, DrawableResourceDetails> resourceMemoryMap,
            EvaluationSettings evaluationSettings) {
        this.document = document;
        this.sceneNode = findSceneNode(document);
        this.resourceMemoryMap = resourceMemoryMap;
        this.evaluationSettings = evaluationSettings;
        this.resourceCollector =
                new WatchFaceResourceCollector(document, resourceMemoryMap, evaluationSettings);
    }

    /**
     * Evaluates the watch face layout document under the given variant.
     *
     * @param variant the variant to consider when traversing and evaluating the watch face.
     * @return the memory footprint, in bytes, that the watch face consumes under the given variant.
     */
    long evaluateBytes(VariantConfigValue variant) {
        // find the user config keys that are mutually exclusive, meaning that no two keys from
        // different sets are parents of the same resource.
        List<Set<UserConfigKey>> userConfigSplit =
                ResourceConfigTable.findConfigsForResources(
                                document, variant, resourceMemoryMap, evaluationSettings)
                        .replaceWithTopLevelKeys(document)
                        .joinRelatedUserConfigKeys();

        // map each self-contained set of user config keys to a lazy iterator that generates all
        // possible combinations of key-value pairs from that set of user config keys
        List<SizedIterator<UserConfigSet>> configIterators =
                userConfigSplit.stream().map(UserConfigKey::buildConfigSets).collect(toList());

        long totalNumberOfConfigs =
                configIterators.stream().mapToLong(SizedIterator::getSize).sum();

        if (evaluationSettings.isVerbose()) {
            System.out.printf("Watch face has %d configs%n", totalNumberOfConfigs);
        }

        if (totalNumberOfConfigs > evaluationSettings.getGreedyEvaluationSwitch()) {
            if (evaluationSettings.isVerbose()) {
                System.out.println("Using greedy evaluation%n");
            }
            return greedyEvaluate(variant);
        }

        return lazyEvaluate(variant, configIterators);
    }

    /**
     * Evaluates the watch face layout using a greedy approach that counts duplicated resources as
     * many times as they appear. This will lead to an over approximation of the memory footprint.
     * This approach is only used when the watch face has too many user configs and cannot be
     * accurately evaluated in a reasonable amount of time.
     */
    private long greedyEvaluate(VariantConfigValue variant) {
        return greedyEvaluate(sceneNode, variant);
    }

    private long greedyEvaluate(Node currentNode, VariantConfigValue variant) {
        if (variant.isNodeSkipped(currentNode)) {
            return 0;
        }
        if (isDrawableNode(currentNode)) {
            return sumDrawableResourceFootprintBytes(
                    resourceMemoryMap, collectDrawableNodeResourceReferences(currentNode, variant));
        }
        LongStream childrenFootprints =
                childrenStream(currentNode)
                        .mapToLong(childNode -> greedyEvaluate(childNode, variant));
        if (isValidUserConfigNode(currentNode)) {
            return childrenFootprints.max().orElse(0);
        }
        return childrenFootprints.sum();
    }

    /**
     * Lazily evaluates the watch face layout, returning the maximum memory footprint that the watch
     * face can have, in bytes, under any user configuration.
     *
     * <p>The user configurations are represented as a list of iterators, each producing partial
     * user config sets. Any two user config sets from different iterators are mutually exclusive,
     * meaning that they do not affect the same resource. This allows us to evaluate the iterators
     * in isolation, without having to compute all combinations of user configs.
     *
     * <p>We use a lazy iterator because we can still have too many configs to store in memory,
     * hence we are * producing and evaluating them lazily.
     *
     * @param variant the variant under which the layout is evaluated
     * @param configIterators the list of iterators producing mutually-exclusive config sets.
     */
    private long lazyEvaluate(
            VariantConfigValue variant, List<SizedIterator<UserConfigSet>> configIterators) {
        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, variant);

        long footprintOfResourcesWithConfigs =
                configIterators.stream()
                        .mapToLong(
                                userConfigIterator ->
                                        evaluateConfigSetForMaxFootprintBytes(
                                                userConfigIterator,
                                                variant,
                                                drawableNodeConfigTable
                                                        .getDependentDrawableNodes()))
                        .sum();
        long footprintOfLeafsWithoutConfigs =
                evaluateIndependentDrawableNodesBytes(variant, drawableNodeConfigTable);

        return footprintOfResourcesWithConfigs + footprintOfLeafsWithoutConfigs;
    }

    /**
     * Computes the memory footprint of the drawable nodes that do not have any user configuration
     * parent, hence are not affected by user configs and are always rendered.
     */
    private long evaluateIndependentDrawableNodesBytes(
            VariantConfigValue variant, DrawableNodeConfigTable drawableNodeConfigTable) {
        Set<FootprintResourceReference> topLevelResourceReferences =
                drawableNodeConfigTable.getIndependentDrawableNodes().stream()
                        .flatMap(
                                drawable ->
                                        collectDrawableNodeResourceReferences(
                                                drawable.node, variant)
                                                .stream())
                        .collect(toSet());
        return sumDrawableResourceFootprintBytes(resourceMemoryMap, topLevelResourceReferences);
    }

    /**
     * Iterates over all the configs produced by the given iterator and computes the memory
     * footprint of the resources that match the config. Returns the maximum footprint for any
     * config set.
     */
    private long evaluateConfigSetForMaxFootprintBytes(
            SizedIterator<UserConfigSet> iterator,
            VariantConfigValue variant,
            List<DrawableNodeConfigTable.Entry> drawablesWithConfigs) {
        long maxFootprint = 0;
        while (iterator.hasNext()) {
            UserConfigSet next = iterator.next();
            List<DrawableNodeConfigTable.Entry> nodesMatchingConfigSet =
                    drawablesWithConfigs.stream()
                            .filter(entry -> entry.matchesConfigSet(next))
                            .collect(toList());

            Set<FootprintResourceReference> resourcesForConfigSet =
                    nodesMatchingConfigSet.stream()
                            .flatMap(
                                    leafAndConfig ->
                                            collectDrawableNodeResourceReferences(
                                                    leafAndConfig.node, variant)
                                                    .stream())
                            .collect(toSet());
            long footprintForConfig =
                    sumDrawableResourceFootprintBytes(resourceMemoryMap, resourcesForConfigSet);
            maxFootprint = max(footprintForConfig, maxFootprint);
        }
        return maxFootprint;
    }

    /**
     * Collect the footprint resource references from a node that renders images. It is important to
     * work with nodes (PartAnimatedImage, PartImage etc) because there are differences in the logic
     * for computing memory footprint between ambient and active depending on the node.
     */
    private Set<FootprintResourceReference> collectDrawableNodeResourceReferences(
            Node currentNode, VariantConfigValue variant) {
        if (isPartAnimatedImage(currentNode)) {
            return collectPartAnimatedImageResources(currentNode, variant);
        } else if (isPartImage(currentNode) || isBitmapFont(currentNode) || isFont(currentNode)) {
            return collectTotalOfResources(currentNode);
        }
        // explicitly check for the expected nodes and throw if we forget one of them to make it
        // easier to add more cases
        throw new IllegalArgumentException(
                String.format("Drawable node %s was not handled", currentNode.getNodeName()));
    }

    private Set<FootprintResourceReference> collectPartAnimatedImageResources(
            Node currentNode, VariantConfigValue variant) {
        Set<String> resourceNamesUnderAnimatedImage =
                resourceCollector.collectResources(currentNode);
        // Animated images are counted differently in ambient and active.
        if (variant.isAmbient()) {
            // If we're evaluating the watch face in ambient, then we want the biggest frame
            // of the resources referenced by the animation.
            String resourceNameWithBiggestFrame =
                    Collections.max(
                            resourceNamesUnderAnimatedImage,
                            Comparator.comparingLong(
                                    resourceName ->
                                            resourceMemoryMap
                                                    .get(resourceName)
                                                    .getBiggestFrameFootprintBytes()));
            return Collections.singleton(
                    FootprintResourceReference.biggestFrameOf(resourceNameWithBiggestFrame));
        } else {
            return resourceNamesUnderAnimatedImage.stream()
                    .map(FootprintResourceReference::totalOf)
                    .collect(toSet());
        }
    }

    private Set<FootprintResourceReference> collectTotalOfResources(Node currentNode) {
        return resourceCollector.collectResources(currentNode).stream()
                .map(FootprintResourceReference::totalOf)
                .collect(toSet());
    }
}
