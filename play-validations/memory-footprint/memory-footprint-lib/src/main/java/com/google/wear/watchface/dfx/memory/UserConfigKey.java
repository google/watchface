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

import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.childrenStream;
import static java.util.Collections.emptyIterator;
import static java.util.Collections.emptyList;

import com.google.wear.watchface.dfx.memory.UserConfigValue.SupportedConfigs;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/** Represents the key of a UserConfiguration ie a ListConfiguration or a BooleanConfiguration. */
class UserConfigKey {

    /** The id of the user configuration. */
    private final String keyId;

    /**
     * The user configuration node. It is used to compute the possible values for this config key.
     */
    private final Node originNode;

    UserConfigKey(String keyId) {
        this.keyId = keyId;
        this.originNode = null;
    }

    public UserConfigKey(String keyId, Node originNode) {
        this.keyId = keyId;
        this.originNode = originNode;
    }

    public String getKeyId() {
        return keyId;
    }

    /**
     * Computes equality based only on the keyId. Used to retrieve values from the map representing
     * a config set.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserConfigKey)) {
            return false;
        }
        UserConfigKey that = (UserConfigKey) o;
        return keyId.equals(that.keyId);
    }

    /**
     * Computes hashCode based only on the keyId. Used to retrieve values from the map representing
     * a config set.
     */
    @Override
    public int hashCode() {
        return Objects.hash(keyId);
    }

    @Override
    public String toString() {
        return "UserConfigKey{" + "keyId='" + keyId + '\'' + '}';
    }

    /**
     * Returns the list option values or the boolean values for the current configuration key. For
     * example, considering the following XML:
     *
     * <pre>{@code
     * <ListConfiguration id="list-1">
     *    <ListOption id="0" />
     *    <ListOption id="1" />
     * </ListConfiguration>
     * <BooleanConfiguration id="background" />
     * }</pre>
     *
     * <p>list-1 would produce ["0", "1"], while background would produce ["TRUE", "FALSE"]
     */
    public List<UserConfigValue> getConfigurationValues() {
        if (originNode == null) {
            return emptyList();
        }
        SupportedConfigs nodeType = SupportedConfigs.valueOf(originNode.getNodeName());
        switch (nodeType) {
            case SupportedConfigs.ListConfiguration:
                return childrenStream(originNode)
                        .filter(listOption -> listOption.getNodeName().equals("ListOption"))
                        .map(listOption -> new UserConfigValue(getNodeId(listOption)))
                        .collect(Collectors.toList());
            case SupportedConfigs.BooleanConfiguration:
                return Arrays.asList(new UserConfigValue("TRUE"), new UserConfigValue("FALSE"));
            default:
                throw new IllegalStateException(
                        String.format(
                                "Cannot extract configuration values out of node type %s",
                                nodeType));
        }
    }

    /**
     * Extracts all the UserConfigurations from a watch face document and maps them to
     * ConfigurationKeys.
     */
    public static List<UserConfigKey> readUserConfigKeys(Document watchFaceDocument) {
        Node userConfigurations =
                watchFaceDocument.getElementsByTagName("UserConfigurations").item(0);
        if (userConfigurations == null) {
            return Collections.emptyList();
        }
        return childrenStream(userConfigurations)
                .filter(SupportedConfigs::isValidUserConfigNode)
                .map(configNode -> new UserConfigKey(getNodeId(configNode), configNode))
                .collect(Collectors.toList());
    }

    /**
     * Builds all the possible configurations that could affect the watch face layout. The set of
     * possible configurations is a cartesian product of the set of values for each Configuration
     * Key in the configs argument. Each map in the returned list represents a tuple of this
     * cartesian product where each element is associated to its originating Configuration Key.
     */
    static SizedIterator<UserConfigSet> buildConfigSets(Iterable<UserConfigKey> configs) {
        return buildConfigSets(configs.iterator());
    }

    private static SizedIterator<UserConfigSet> buildConfigSets(Iterator<UserConfigKey> configs) {
        if (!configs.hasNext()) {
            return SizedIterator.fromIterator(emptyIterator(), 0);
        }
        UserConfigKey head = configs.next();
        SizedIterator<UserConfigSet> tailExpanded = buildConfigSets(configs);
        return SizedIterator.combine(
                tailExpanded,
                head.getConfigurationValues(),
                (configSet, configValue) -> configSet.plus(head, configValue),
                (configValue) -> UserConfigSet.singleton(head, configValue));
    }

    public static UserConfigKey fromNode(Node node) {
        return new UserConfigKey(getNodeId(node), node);
    }

    private static String getNodeId(Node node) {
        return node.getAttributes().getNamedItem("id").getNodeValue();
    }
}
