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

import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute;

import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

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
 * <p>The UserConfigValues are: - for dateDisplayed: TRUE, FALSE, - for icon-id-option: 0, and 1.
 */
class UserConfigValue {

    public static UserConfigValue fromNode(Node node) {
        return new UserConfigValue(
                getNodeAttribute(node, "id")
                        .orElseThrow(
                                () -> new IllegalArgumentException("Node does not contain an id")));
    }

    /**
     * The UserConfiguration children node names that are treated as configuration values. We ignore
     * ColorOptions because they don't enable or disable particular DOM nodes.
     */
    enum SupportedConfigs {
        ListConfiguration,
        BooleanConfiguration;

        static boolean isValidUserConfigNode(Node node) {
            return Arrays.stream(SupportedConfigs.values())
                    .map(Enum::toString)
                    .anyMatch(x -> x.equals(node.getNodeName()));
        }
    }

    private final String configValueId;

    public UserConfigValue(String configValueId) {
        this.configValueId = configValueId;
    }

    /**
     * Selects the child of the node that must be evaluated based on this configuration id. It works
     * on either a ListConfiguration with ListOptions or BooleanConfiguration with BooleanOptions.
     * If no match with the same id as configValueId is found in the child nodes, then it returns
     * None.
     *
     * @throws IllegalArgumentException If the node is not a ListConfiguration or a
     *     BooleanConfiguration.
     */
    public Optional<Node> getNodeToEvaluate(Node node) {
        if (!SupportedConfigs.isValidUserConfigNode(node)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot extract User Configuration child from %s", node.getNodeName()));
        }
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node listOption = node.getChildNodes().item(i);
            try {
                if (listOption
                        .getAttributes()
                        .getNamedItem("id")
                        .getNodeValue()
                        .equals(configValueId)) {
                    return Optional.of(listOption);
                }
            } catch (NullPointerException e) {
                // ignore child nodes without ID
            }
        }
        // If no node is found to match the setting id
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserConfigValue)) return false;
        UserConfigValue that = (UserConfigValue) o;
        return Objects.equals(configValueId, that.configValueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configValueId);
    }

    @Override
    public String toString() {
        return "UserConfigValue{" + "configValueId='" + configValueId + '\'' + '}';
    }
}
