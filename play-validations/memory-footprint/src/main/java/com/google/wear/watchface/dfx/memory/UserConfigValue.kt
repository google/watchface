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
package com.google.wear.watchface.dfx.memory

import com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute
import org.w3c.dom.Node
import java.util.Arrays
import java.util.Optional

/**
 * Represents a UserConfiguration value. For example, in the following XML:
 *
 * <pre>{@code
 * <UserConfigurations>
 *   <BooleanConfiguration id="dateDisplayed" defaultValue="TRUE" displayName="Display Date" />
 *   <ListConfiguration id="icon-id-option" displayName="SET 1">
 *     <ListOption id="0" icon="style-minute-blue" displayName="SET 1 0" />
 *     <ListOption id="1" icon="style-minute-purple.png" displayName="SET 1 1" />
 *   </ListConfiguration>
 * </UserConfigurations>
 * }</pre>
 *
 * The UserConfigValues are: - for dateDisplayed: TRUE, FALSE, - for icon-id-option: 0, and 1.
 */
internal data class UserConfigValue(private val configValueId: String) {

    /**
     * The UserConfiguration children node names that are treated as configuration values. We ignore
     * ColorOptions because they don't enable or disable particular DOM nodes.
     */
    enum class SupportedConfigs {
        ListConfiguration,
        BooleanConfiguration;

        companion object {
            @JvmStatic
            fun isValidUserConfigNode(node: Node): Boolean {
                return entries.any { it.toString() == node.nodeName }
            }
        }
    }

    /**
     * Selects the child of the node that must be evaluated based on this configuration id. It works
     * on either a ListConfiguration with ListOptions or BooleanConfiguration with BooleanOptions.
     * If no match with the same id as configValueId is found in the child nodes, then it returns
     * None.
     *
     * @throws IllegalArgumentException If the node is not a ListConfiguration or a
     *   BooleanConfiguration.
     */
    fun getNodeToEvaluate(node: Node): Optional<Node> {
        if (!SupportedConfigs.isValidUserConfigNode(node)) {
            throw IllegalArgumentException(
                String.format("Cannot extract User Configuration child from %s", node.nodeName)
            )
        }
        for (i in 0 until node.childNodes.length) {
            val listOption = node.childNodes.item(i)
            try {
                if (
                    listOption.attributes.getNamedItem("id").nodeValue ==
                        configValueId
                ) {
                    return Optional.of(listOption)
                }
            } catch (e: NullPointerException) {
                // ignore child nodes without ID
            }
        }
        // If no node is found to match the setting id
        return Optional.empty()
    }

    companion object {
        @JvmStatic
        fun fromNode(node: Node): UserConfigValue {
            return UserConfigValue(
                getNodeAttribute(node, "id")
                    .orElseThrow { IllegalArgumentException("Node does not contain an id") }
            )
        }
    }
}