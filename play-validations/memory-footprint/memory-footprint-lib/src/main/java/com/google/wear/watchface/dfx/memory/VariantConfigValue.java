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
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.getNodeAttribute;

import java.util.Optional;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Represents a single value of a mode variant. */
class VariantConfigValue {
    private static final String AMBIENT = "AMBIENT";

    static VariantConfigValue ambient(EvaluationSettings evaluationSettings) {
        return new VariantConfigValue(true, evaluationSettings);
    }

    static VariantConfigValue active(EvaluationSettings evaluationSettings) {
        return new VariantConfigValue(false, evaluationSettings);
    }

    private final boolean isAmbient;
    private final EvaluationSettings evaluationSettings;
    private static final String IS_AMBIENT_EXPRESSION = "[STATE.IS_AMBIENT]";

    private VariantConfigValue(boolean isAmbient, EvaluationSettings evaluationSettings) {
        this.isAmbient = isAmbient;
        this.evaluationSettings = evaluationSettings;
    }

    boolean isAmbient() {
        return isAmbient;
    }

    boolean isNodeSkipped(Node node) {
        if (node.getNodeName().equals("Compare")) {
            return isSkippedExpressionCompareNode(node);
        }
        if (node.getNodeName().equals("Default")) {
            return isSkippedExpressionDefaultNode(node);
        }

        if (evaluationSettings.isHoneyfaceMode()) {
            return isNodeSkippedHoneyface(node);
        }

        Optional<String> alphaAttributeForVariant =
                childrenStream(node)
                        .filter(this::variantNodeForAlphaMatchesCurrentValue)
                        .map(n -> n.getAttributes().getNamedItem("value").getNodeValue())
                        .findFirst();
        Optional<String> ownAlphaAttribute =
                Optional.ofNullable(node.getAttributes())
                        .map(attributes -> attributes.getNamedItem("alpha"))
                        .map(Node::getNodeValue);

        // the variant alpha takes precedence over the node alpha attribute
        Optional<String> actualAlpha =
                alphaAttributeForVariant.isPresent() ? alphaAttributeForVariant : ownAlphaAttribute;

        // if the alpha value is 0, then the node is disabled under the current variant, otherwise
        // the node is enabled
        return actualAlpha.map(s -> s.equals("0")).orElse(false);
    }

    private boolean isNodeSkippedHoneyface(Node node) {
        Optional<String> alphaAttribute = getHoneyfaceAlphaAttributeNode(node);

        if (!alphaAttribute.isPresent()) {
            // if no alpha attribute for the current variant is defined, then the node should not be
            // skipped
            return false;
        }

        return alphaAttribute.map(x -> x.equals("0")).get();
    }

    private Optional<String> getHoneyfaceAlphaAttributeNode(Node node) {
        Optional<NamedNodeMap> attributes = Optional.ofNullable(node.getAttributes());
        String alphaAttribute = isAmbient ? "alphaAmbient" : "alpha";
        return attributes
                .map(actualAttributes -> actualAttributes.getNamedItem(alphaAttribute))
                .map(Node::getNodeValue);
    }

    private boolean variantNodeForAlphaMatchesCurrentValue(Node node) {
        // Find the first Variant tag under the parentNode that works in the current
        // VariantConfigValue and targets the alpha channel.
        if (!node.getNodeName().equals("Variant")) {
            return false;
        }
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return false;
        }
        Node mode = attributes.getNamedItem("mode");
        if (mode == null) {
            return false;
        }

        String nodeVariantValue = mode.getNodeValue();
        if (nodeVariantValue.equals(AMBIENT)) {
            if (!isAmbient) {
                return false;
            }
        } else {
            throw new IllegalArgumentException(
                    String.format("Variant node with mode %s is not supported", nodeVariantValue));
        }
        Node target = attributes.getNamedItem("target");
        if (target == null) {
            return false;
        }
        return target.getNodeValue().equals("alpha");
    }

    /**
     * Returns true if {@code compareNode} has an ambient expression and the variant is not ambient.
     * I.e. {@code compareNode} is only rendered in ambient and should be skipped.
     */
    private boolean isSkippedExpressionCompareNode(Node compareNode) {
        // If either the expression can't be found, or it doesn't match IS_AMBIENT_EXPRESSION then
        // don't skip.
        if (!IS_AMBIENT_EXPRESSION.equals(findCompareNodeExpression(compareNode))) {
            return false;
        }

        // It's an ambient expression, so only skip if we're not in ambient.
        return !isAmbient;
    }

    /**
     * Returns true if the {@code defaultNode} has a sibling Compare node which is drawn in ambient
     * and the variant is ambient. I.e. the sibling node would be drawn and the {@code defaultNode}
     * should be skipped.
     */
    private boolean isSkippedExpressionDefaultNode(Node defaultNode) {
        if (hasSiblingAmbientExpression(defaultNode)) {
            return isAmbient;
        }
        return false;
    }

    /** Returns true if {@code node} has a sibling IS_AMBIENT expression node. */
    private boolean hasSiblingAmbientExpression(Node node) {
        NodeList siblingNodes = node.getParentNode().getChildNodes();
        for (int i = 0; i < siblingNodes.getLength(); i++) {
            Node sibling = siblingNodes.item(i);
            if (sibling.getNodeName().equals("Compare")
                    && IS_AMBIENT_EXPRESSION.equals(findCompareNodeExpression(sibling))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the `expression` attribute of the given Compare node and looks up it's definition
     * within the sibling <Expressions> node.
     *
     * <p>The XML for a compare node looks as follows:
     *
     * <pre>{@code
     * <p><Condition>
     *     <Expressions>
     *         <Expression name="isAmbient"><![CDATA[ [STATE.IS_AMBIENT]]]></Expression>
     *     </Expressions>
     *     <Compare expression="isAmbient"> ... </Compare>
     *     ...
     * </Condition>
     * }</pre>
     */
    private String findCompareNodeExpression(Node compareNode) {
        Optional<String> expressionName = getNodeAttribute(compareNode, "expression");
        if (!expressionName.isPresent()) {
            return null;
        }

        Node expressionsNode =
                findFirstChildWithNodeName(compareNode.getParentNode(), "Expressions");
        if (expressionsNode == null) {
            return null;
        }

        // Search the children of expressionsNode for an expression node whose name attribute
        // matches expressionName. If we find it, return the trimmed text content.
        NodeList expressionsChildNodes = expressionsNode.getChildNodes();
        for (int j = 0; j < expressionsChildNodes.getLength(); j++) {
            Node expressionsChild = expressionsChildNodes.item(j);
            if (!expressionsChild.getNodeName().equals("Expression")
                    || !getNodeAttribute(expressionsChild, "name").equals(expressionName)) {
                continue;
            }
            return expressionsChild.getTextContent().trim();
        }

        return null;
    }

    private Node findFirstChildWithNodeName(Node parentNode, String nodeName) {
        NodeList childNodes = parentNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeName().equals(nodeName)) {
                return child;
            }
        }
        return null;
    }
}
