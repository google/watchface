package com.google.wear.watchface.dfx.memory

import com.google.common.collect.ImmutableSetMultimap
import com.google.common.graph.GraphBuilder
import com.google.common.graph.Traverser
import org.w3c.dom.Document

/**
 * A container class that holds, for each resource referenced by the watch face, the
 * [UserConfigKeys][UserConfigKey] that the resource depends on. A resource depends on a
 * [UserConfigKey] if, inside the scene node of a watch face, the resource has a parent with that
 * UserConfigKey. For example:
 *
 * ```
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
 * ```
 *
 * The ResourceConfigTable is the following:
 *
 *   - image1: ListConfiguration(id=l1), BooleanConfiguration(id=b1)
 *   - image2: ListConfiguration(id=l1)
 *   - image3: ListConfiguration(id=l1)
 *   - image4: BooleanConfiguration(id=b1)
 */
internal class ResourceConfigTable(
    private val resourceNameToKeys: ImmutableSetMultimap<String, UserConfigKey>,
) {

    /**
     * Computes a mutually exclusive split of the UserConfigKeys, where two keys are mutually
     * exclusive if there is no resource that needs both of them to be rendered.
     */
    fun joinRelatedUserConfigKeys(): List<Set<UserConfigKey>> {
        // for each entry in the map, the value represents the user config keys that are parents
        // of that entry. The entries are resource names.

        // create a graph where each value in the map represents a component in this graph
        // some components might be connected by at least one shared user config key
        val graph = GraphBuilder.undirected().build<UserConfigKey>()
        for (value in resourceNameToKeys.values()) {
            graph.addNode(value)
        }

        for (resource in resourceNameToKeys.keySet()) {
            val userConfigKeys = resourceNameToKeys.get(resource).toTypedArray()

            for (i in 0 until userConfigKeys.size - 1) {
                graph.putEdge(userConfigKeys[i], userConfigKeys[i + 1])
            }
        }

        val userConfigKeyTraverser = Traverser.forGraph(graph)
        val allNodes = graph.nodes().toMutableSet()

        return buildList {
            while (allNodes.isNotEmpty()) {
                val nextUnhandledKey = allNodes.iterator().next()
                val nextComponent =
                    userConfigKeyTraverser.depthFirstPreOrder(nextUnhandledKey).toSet()
                add(nextComponent)
                allNodes.removeAll(nextComponent)
            }
        }
    }

    /**
     * Computes a [ResourceConfigTable] where each reference to a [UserConfigKey] is replaced to the
     * [UserConfigKey] defined outside the watch face's scene node. When creating the
     * [ResourceConfigTable], it is easier to find the keys in the SceneNode. However, using them
     * might lead to bugs because the scene node might not always have exhaustive List or Boolean
     * Options, so we have to compute the set of possible values based on the top-level definition
     * of User Configs. The following snippet illustrates this case:
     *
     * ```
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
     * ```
     */
    fun replaceWithTopLevelKeys(document: Document): ResourceConfigTable {
        val userConfigKeys = UserConfigKey.readUserConfigKeys(document).associateBy { it.keyId }

        // in this object's resourceNameToKeys, replace each reference to a user config key with the
        // top level user config definition, which contains all allowed values
        val resourceNameToTopLevelKeys = resourceNameToKeys.entries().asSequence()
            .map { it.key!! to userConfigKeys[it.value.keyId]!! }
            .toMultimap()

        return ResourceConfigTable(resourceNameToTopLevelKeys)
    }

    private fun <K : Any, V : Any> Sequence<Pair<K, V>>.toMultimap() =
        fold(ImmutableSetMultimap.Builder<K, V>()) { acc, crt ->
            acc.put(crt.first, crt.second)
        }.build()

    companion object {
        /**
         * Constructs the ResourceConfigTable from a given DrawableNodeConfigTable, using the
         * provided WatchFaceResourceCollector and under the given variant. The
         * DrawableNodeConfigTable contains, for each node that is being drawn dynamically, the set
         * of user config keys and values that enables the node for rendering. From this
         * information, we can derive for each actual resource, the set of user config keys that
         * they depend on.
         */
        @JvmStatic
        fun fromDrawableNodeConfigTable(
            drawableNodeConfigTable: DrawableNodeConfigTable,
            resourceCollector: WatchFaceResourceCollector,
            variant: VariantConfigValue,
        ): ResourceConfigTable {
            val keysForResourcesBuilder = ImmutableSetMultimap.Builder<String, UserConfigKey>()

            for (entry in drawableNodeConfigTable.allEntries) {
                val resources = resourceCollector.collectResources(entry.node, variant)
                val userConfigKeys = entry.userConfigSet.config.keys
                for (resource in resources) {
                    keysForResourcesBuilder.putAll(resource, userConfigKeys)
                }
            }
            return ResourceConfigTable(keysForResourcesBuilder.build())
        }
    }
}