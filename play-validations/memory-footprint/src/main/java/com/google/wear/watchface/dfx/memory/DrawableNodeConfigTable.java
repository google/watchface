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
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.isDrawableNode;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A container class that holds, for each drawable node of the watch face, the user config set that
 * leads the node to being rendered. For example:
 *
 * <pre>{@code
 * <ListConfiguration id="l1">
 *     <ListOption id="l1-1">
 *         <PartImage name="image1" x="0" y="0" width="300" height="100">
 *             <Image resource="image1" />
 *         </PartImage>
 *     </ListOption>
 *     <ListOption id="l1-2">
 *         <PartImage name="image2" x="0" y="0" width="300" height="100">
 *             <Image resource="image2" />
 *         </PartImage>
 *         <PartImage name="image3" x="100" y="0" width="300" height="100">
 *             <Image resource="image3" />
 *         </PartImage>
 *     </ListOption>
 * </ListConfiguration>
 * <BooleanConfiguration id="b1">
 *     <BooleanOption id="TRUE">
 *         <PartImage name="image1-duplicate" x="0" y="300" width="200" height="100">
 *             <Image resource="image1" />
 *         </PartImage>
 *     </BooleanOption>
 *     <BooleanOption id="FALSE">
 *         <PartImage name="image4" x="0" y="300" width="200" height="100">
 *             <Image resource="image4" />
 *         </PartImage>
 *     </BooleanOption>
 * </BooleanConfiguration>
 * }</pre>
 *
 * The drawable node config table is:
 *
 * <ul>
 *   <li>PartImage(image1): (l1 -> l1-1)
 *   <li>PartImage(image2): (l1 -> l1-2)
 *   <li>PartImage(image1-duplicate): (b1 -> TRUE)
 *   <li>PartImage(image4): (b1 -> FALSE)
 * </ul>
 *
 * <p>A drawable node is a node that requires drawable resources to be rendered, hence it influences
 * the memory footprint. See {@link WatchFaceDocuments#isDrawableNode(org.w3c.dom.Node)}.
 */
class DrawableNodeConfigTable {

    private final List<Entry> drawableNodes;

    DrawableNodeConfigTable() {
        this(new ArrayList<>());
    }

    DrawableNodeConfigTable(List<Entry> drawableNodes) {
        this.drawableNodes = drawableNodes;
    }

    static class Entry {
        Node node;
        UserConfigSet userConfigSet;

        public Entry(Node node, UserConfigSet userConfigSet) {
            this.node = node;
            this.userConfigSet = userConfigSet;
        }

        @Override
        public String toString() {
            return "Entry{" + "node=" + node + ", userConfigSet=" + userConfigSet + '}';
        }

        /**
         * Checks if this entry is rendered when the watch face configuration is set according to
         * the given user config set. An entry is rendered if and only if every key in its
         * userConfigSet is present in the referenceConfigSet and their values match. An entry with
         * the empty set will match any reference config set.
         */
        boolean matchesConfigSet(UserConfigSet referenceConfigSet) {
            return userConfigSet.config.entrySet().stream()
                    .allMatch(
                            requiredConfigEntry ->
                                    referenceConfigSet.containsKey(requiredConfigEntry.getKey())
                                            && referenceConfigSet
                                                    .get(requiredConfigEntry.getKey())
                                                    .equals(requiredConfigEntry.getValue()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry entry = (Entry) o;
            return Objects.equals(node, entry.node)
                    && Objects.equals(userConfigSet, entry.userConfigSet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(node, userConfigSet);
        }
    }

    /** Returns the drawable nodes that depend on some configuration to be rendered. */
    List<Entry> getDependentDrawableNodes() {
        return drawableNodes.stream()
                .filter(drawable -> !drawable.userConfigSet.config.isEmpty())
                .collect(toList());
    }

    /**
     * Returns the drawable nodes that do not depend on some configuration, but which are rendered
     * all the time, regardless of what is the current user configuration of a watch face.
     */
    List<Entry> getIndependentDrawableNodes() {
        return drawableNodes.stream()
                .filter(drawable -> drawable.userConfigSet.config.isEmpty())
                .collect(toList());
    }

    List<Entry> getAllEntries() {
        return drawableNodes;
    }

    /**
     * Creates the DrawableNodeConfigTable for a given watch face and a given variant.
     *
     * @param sceneNode the watch face's scene node
     * @param variant the variant for which to compute the config table. If a resource is not
     *     rendered in the given variant (ambient or active), it will not be added to the table.
     */
    static DrawableNodeConfigTable create(Node sceneNode, VariantConfigValue variant) {
        return new DrawableNodeConfigTable(
                findEntriesForNode(sceneNode, variant, new UserConfigSet(new HashMap<>())));
    }

    /** Recursively traverse the watch face to compute the DrawableNodeConfigTable. */
    private static List<DrawableNodeConfigTable.Entry> findEntriesForNode(
            Node currentNode, VariantConfigValue variant, UserConfigSet configSetSoFar) {
        if (variant.isNodeSkipped(currentNode)) {
            return Collections.emptyList();
        }
        if (isDrawableNode(currentNode)) {
            return Collections.singletonList(
                    new DrawableNodeConfigTable.Entry(currentNode, configSetSoFar));
        }

        if (isValidUserConfigNode(currentNode)) {
            UserConfigKey key = UserConfigKey.fromNode(currentNode);
            // In case of nested user configs, the format allows nesting the same user config under
            // itself. We have to filter out incompatible branches, so that we don't get in a state
            // where we're mapping the same drawable node to two different values of the same key
            Stream<UserConfigValue> configsToConsider =
                    key.getConfigurationValues().stream()
                            .filter(
                                    configValue ->
                                            userConfigIsCompatibleWithConfigSet(
                                                    key, configValue, configSetSoFar));

            return configsToConsider
                    .flatMap(
                            // for each user config option, find all DrawableNodeConfigTable entries
                            // by appending the config option to the configSet in the recursive call
                            configValue ->
                                    configValue
                                            .getNodeToEvaluate(currentNode)
                                            .map(
                                                    userConfigOption ->
                                                            findEntriesForNode(
                                                                    userConfigOption,
                                                                    variant,
                                                                    configSetSoFar.plus(
                                                                            key, configValue))
                                                                    .stream())
                                            .orElse(Stream.empty()))
                    .collect(toList());
        }

        // if the current node is a regular node, recursively call findEntriesForNode
        // for each child node
        return childrenStream(currentNode)
                .map(node -> findEntriesForNode(node, variant, configSetSoFar))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    /**
     * Checks if the key and value are compatible with the config set. A (key, value) config is
     * compatible with a config set if the config set does not already contain the key or if the
     * config set contains the key and the value of that key in the config set is the same as the
     * value argument.
     */
    private static boolean userConfigIsCompatibleWithConfigSet(
            UserConfigKey key, UserConfigValue configValue, UserConfigSet configSet) {

        return !configSet.containsKey(key) || configValue.equals(configSet.get(key));
    }

    public DrawableNodeConfigTable withConfig(UserConfigKey key, UserConfigValue value) {
        List<Entry> newEntries =
                drawableNodes.stream()
                        .filter(
                                // if a particular node in the watchface DOM is found under two
                                // different values for the same user config key, then it will never
                                // be rendered, so we do not keep it in the new table. This is to
                                // avoid the case when multiple ListConfiguration or
                                // BooleanConfiguration with the same id are nested.
                                entry ->
                                        userConfigIsCompatibleWithConfigSet(
                                                key, value, entry.userConfigSet))
                        .map(entry -> new Entry(entry.node, entry.userConfigSet.plus(key, value)))
                        .collect(toList());
        return new DrawableNodeConfigTable(newEntries);
    }

    public void addAll(DrawableNodeConfigTable other) {
        this.drawableNodes.addAll(other.drawableNodes);
    }

    public void addNodeWithEmptyConfig(Node node) {
        this.drawableNodes.add(new Entry(node, new UserConfigSet(emptyMap())));
    }
}
