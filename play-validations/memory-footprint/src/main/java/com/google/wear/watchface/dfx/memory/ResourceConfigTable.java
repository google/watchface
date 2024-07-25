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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import com.google.mu.util.stream.BiStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;

/**
 * A container class that holds, for each resource referenced by the watch face, the {@link
 * UserConfigKey UserConfigKeys} that the resource depends on. A resource depends on a {@link
 * UserConfigKey} if, inside the scene node of a watch face, the resource has a parent with that
 * UserConfigKey. For example:
 *
 * <pre>{@code
 * <ListConfiguration id="l1">
 *     <ListOption id="l1-1">
 *         <PartImage x="0" y="0" width="300" height="100">
 *             <Image resource="image1" />
 *         </PartImage>
 *     </ListOption>
 *     <ListOption id="l1-2">
 *         <PartImage x="0" y="0" width="300" height="100">
 *             <Image resource="image2" />
 *         </PartImage>
 *         <PartImage x="100" y="0" width="300" height="100">
 *             <Image resource="image3" />
 *         </PartImage>
 *     </ListOption>
 * </ListConfiguration>
 * <BooleanConfiguration id="b1">
 *     <BooleanOption id="TRUE">
 *         <PartImage x="0" y="300" width="200" height="100">
 *             <Image resource="image1" />
 *         </PartImage>
 *     </BooleanOption>
 *     <BooleanOption id="FALSE">
 *         <PartImage x="0" y="300" width="200" height="100">
 *             <Image resource="image4" />
 *         </PartImage>
 *     </BooleanOption>
 * </BooleanConfiguration>
 * }</pre>
 *
 * The ResourceConfigTable is the following:
 *
 * <ul>
 *   <li>image1: ListConfiguration(id=l1), BooleanConfiguration(id=b1)
 *   <li>image2: ListConfiguration(id=l1)
 *   <li>image3: ListConfiguration(id=l1)
 *   <li>image4: BooleanConfiguration(id=b1)
 * </ul>
 */
class ResourceConfigTable {

    /**
     * Constructs the ResourceConfigTable from a given DrawableNodeConfigTable, using the provided
     * WatchFaceResourceCollector and under the given variant. The DrawableNodeConfigTable contains,
     * for each node that is being drawn dynamically, the set of user config keys and values that
     * enables the node for rendering. From this information, we can derive for each actual
     * resource, the set of user config keys that they depend on.
     */
    static ResourceConfigTable fromDrawableNodeConfigTable(
            DrawableNodeConfigTable drawableNodeConfigTable,
            WatchFaceResourceCollector resourceCollector,
            VariantConfigValue variant) {
        SetMultimap<String, UserConfigKey> keysForResources =
                MultimapBuilder.hashKeys().hashSetValues().build();

        for (DrawableNodeConfigTable.Entry entry : drawableNodeConfigTable.getAllEntries()) {
            Set<String> resources = resourceCollector.collectResources(entry.node, variant);
            Set<UserConfigKey> userConfigKeys = entry.userConfigSet.config.keySet();
            for (String resource : resources) {
                keysForResources.putAll(resource, userConfigKeys);
            }
        }

        return new ResourceConfigTable(keysForResources);
    }

    private final SetMultimap<String, UserConfigKey> resourceNameToKeys;

    private ResourceConfigTable(SetMultimap<String, UserConfigKey> resourceNameToKeys) {
        this.resourceNameToKeys = resourceNameToKeys;
    }

    /**
     * Computes a mutually exclusive split of the UserConfigKeys, where two keys are mutually
     * exclusive if there is no resource that needs both of them to be rendered.
     */
    List<Set<UserConfigKey>> joinRelatedUserConfigKeys() {
        // for each entry in the map, the value represents the user config keys that are parents
        // of that entry. The entries are resource names.

        // create a graph where each value in the map represents a component in this graph
        // some components might be connected by at least one shared user config key
        MutableGraph<UserConfigKey> graph = GraphBuilder.undirected().build();
        resourceNameToKeys.values().forEach(graph::addNode);

        for (String resource : resourceNameToKeys.keySet()) {
            UserConfigKey[] userConfigKeys =
                    resourceNameToKeys.get(resource).toArray(new UserConfigKey[0]);

            for (int i = 0; i < userConfigKeys.length - 1; i++) {
                graph.putEdge(userConfigKeys[i], userConfigKeys[i + 1]);
            }
        }

        Traverser<UserConfigKey> userConfigKeyTraverser = Traverser.forGraph(graph);
        Set<UserConfigKey> allNodes = new HashSet<>(graph.nodes());

        ImmutableList.Builder<Set<UserConfigKey>> resultBuilder = ImmutableList.builder();

        while (!allNodes.isEmpty()) {
            UserConfigKey nextUnhandledKey = allNodes.iterator().next();
            Set<UserConfigKey> nextComponent =
                    Sets.newHashSet(userConfigKeyTraverser.depthFirstPreOrder(nextUnhandledKey));
            resultBuilder.add(nextComponent);
            allNodes.removeAll(nextComponent);
        }

        return resultBuilder.build();
    }

    /**
     * Computes a ResourceConfigTable where each reference to a UserConfigKey is replaced to the
     * UserConfigKey defined outside the watch face's scene node. When creating the
     * ResourceConfigTable, it is easier to find the keys in the SceneNode. However, using them
     * might lead to bugs because the scene node might not always have exhaustive List or Boolean
     * Options, so we have to compute the set of possible values based on the top-level definition
     * of User Configs. The following snippet illustrates this case:
     *
     * <pre>{@code
     * <WatchFace width="450" height="450">
     *     <UserConfigurations>
     *         <ListConfiguration id="l1" displayName="list1" defaultValue="l1-1">
     *             <ListOption id="l1-1" />
     *             <ListOption id="l1-2" />
     *             <ListOption id="l1-3" />
     *         </ListConfiguration>
     *     </UserConfigurations>
     *     <Scene>
     *         <ListConfiguration id="l1">
     *             <ListOption id="l1-1">
     *                 <PartImage x="0" y="0" width="200" height="200">
     *                     <Image resource="image1" />
     *                 </PartImage>
     *             </ListOption>
     *             <ListOption id="l1-2">
     *                 <PartImage x="0" y="0" width="200" height="200">
     *                     <Image resource="image2" />
     *                 </PartImage>
     *             </ListOption>
     *         </ListConfiguration>
     *
     *         <ListConfiguration id="l1">
     *             <ListOption id="l1-2">
     *                 <PartImage x="0" y="0" width="200" height="200">
     *                     <Image resource="image2" />
     *                 </PartImage>
     *             </ListOption>
     *             <ListOption id="l1-3">
     *                 <PartImage x="0" y="0" width="200" height="200">
     *                     <Image resource="image3" />
     *                 </PartImage>
     *             </ListOption>
     *         </ListConfiguration>
     *     </Scene>
     * </WatchFace>
     * }</pre>
     */
    ResourceConfigTable replaceWithTopLevelKeys(Document document) {
        Map<String, UserConfigKey> userConfigKeys =
                UserConfigKey.readUserConfigKeys(document).stream()
                        .collect(toMap(UserConfigKey::getKeyId, identity()));

        // in this object's resourceNameToKeys, replace each reference to a user config key with the
        // top level user config definition, which contains all allowed values
        ImmutableSetMultimap<String, UserConfigKey> resourceNameToTopLevelKeys =
                BiStream.from(resourceNameToKeys.entries())
                        .mapValues(value -> userConfigKeys.get(value.getKeyId()))
                        .collect(ImmutableSetMultimap::toImmutableSetMultimap);

        return new ResourceConfigTable(resourceNameToTopLevelKeys);
    }
}
