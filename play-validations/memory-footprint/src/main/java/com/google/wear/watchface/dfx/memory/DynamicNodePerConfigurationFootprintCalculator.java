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

import static com.google.wear.watchface.dfx.memory.UserConfigValue.SupportedConfigs.isValidUserConfigNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.childrenStream;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isDrawableNode;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.LongStream;

/**
 * Calculates the maximum memory footprint needed to render the given dynamic nodes for any user
 * style combination. The dynamic nodes, ie. the nodes that contain drawable resources and need to
 * be re-drawn on each render, are provided via the {@code drawableNodeConfigTable} constructor
 * argument, together with the user style configurations that cause them to be drawn. In active,
 * this contains all the nodes that have drawable resource references. In ambient however, where the
 * watch face interactivity is restricted, only a sub-set of the dynamic nodes are calculated (the
 * clocks and complication slots).
 */
class DynamicNodePerConfigurationFootprintCalculator {
    private final Document document;
    private final EvaluationSettings evaluationSettings;
    private final Node sceneNode;
    private final WatchFaceResourceCollector resourceCollector;
    private final VariantConfigValue variant;
    private final DrawableNodeConfigTable drawableNodeConfigTable;
    private final Set<Node> dynamicNodesToConsider;

    private final Function<String, Long> evaluator;

    DynamicNodePerConfigurationFootprintCalculator(
            Document document,
            EvaluationSettings evaluationSettings,
            VariantConfigValue variant,
            WatchFaceResourceCollector resourceCollector,
            DrawableNodeConfigTable drawableNodeConfigTable,
            Function<String, Long> evaluator) {
        this.document = document;
        this.sceneNode = findSceneNode(document);
        this.evaluationSettings = evaluationSettings;
        this.variant = variant;
        this.resourceCollector = resourceCollector;
        this.drawableNodeConfigTable = drawableNodeConfigTable;
        this.dynamicNodesToConsider =
                drawableNodeConfigTable.getAllEntries().stream()
                        .map(entry -> entry.node)
                        .collect(toSet());
        this.evaluator = evaluator;
    }

    long calculateMaxFootprintBytes() {
        // find the user config keys that are mutually exclusive, meaning that no two keys from
        // different sets are parents of the same resource.
        List<Set<UserConfigKey>> userConfigSplit =
                ResourceConfigTable.fromDrawableNodeConfigTable(
                                drawableNodeConfigTable, resourceCollector, variant)
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
            return greedyEvaluate();
        }

        return lazyEvaluate(configIterators);
    }

    /**
     * Evaluates the watch face layout using a greedy approach that counts duplicated resources as
     * many times as they appear. This will lead to an over approximation of the memory footprint.
     * This approach is only used when the watch face has too many user configs and cannot be
     * accurately evaluated in a reasonable amount of time.
     */
    long greedyEvaluate() {
        return greedyEvaluate(sceneNode);
    }

    private long greedyEvaluate(Node currentNode) {
        if (variant.isNodeSkipped(currentNode)) {
            return 0;
        }
        if (isDrawableNode(currentNode) && dynamicNodesToConsider.contains(currentNode)) {
            return resourceCollector.collectResources(currentNode, variant).stream()
                    .mapToLong(evaluator::apply)
                    .sum();
        }
        LongStream childrenFootprints = childrenStream(currentNode).mapToLong(this::greedyEvaluate);
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
     * @param configIterators the list of iterators producing mutually-exclusive config sets.
     */
    private long lazyEvaluate(List<SizedIterator<UserConfigSet>> configIterators) {
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
        return drawableNodeConfigTable.getIndependentDrawableNodes().stream()
                .flatMap(entry -> resourceCollector.collectResources(entry.node, variant).stream())
                .distinct()
                .mapToLong(evaluator::apply)
                .sum();
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

            long footprintForConfig =
                    nodesMatchingConfigSet.stream()
                            .flatMap(
                                    entry ->
                                            resourceCollector
                                                    .collectResources(entry.node, variant)
                                                    .stream())
                            .distinct()
                            .mapToLong(evaluator::apply)
                            .sum();
            maxFootprint = max(footprintForConfig, maxFootprint);
        }
        return maxFootprint;
    }
}
