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

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** A set of operations performed on a watch face document or on nodes of a watch face document. */
class WatchFaceDocuments {

    static String getWatchFaceRootNode(EvaluationSettings evaluationSettings) {
        return evaluationSettings.isHoneyfaceMode() ? "Watchface" : "WatchFace";
    }

    static boolean isPartAnimatedImage(Node currentNode) {
        return currentNode.getNodeName().equals("PartAnimatedImage");
    }

    static boolean isPartImage(Node currentNode) {
        return currentNode.getNodeName().equals("PartImage");
    }

    static boolean isBitmapFont(Node currentNode) {
        return currentNode.getNodeName().equals("BitmapFont");
    }

    static boolean isFont(Node currentNode) {
        return currentNode.getNodeName().equals("Font");
    }

    static boolean isClock(Node currentNode) {
        return currentNode.getNodeName().equals("DigitalClock")
                || currentNode.getNodeName().equals("AnalogClock");
    }

    static boolean isDrawableNode(Node currentNode) {
        return isPartImage(currentNode)
                || isPartAnimatedImage(currentNode)
                || isBitmapFont(currentNode)
                || isFont(currentNode)
                || isClock(currentNode);
    }

    static Node findSceneNode(Document document) {
        return document.getElementsByTagName("Scene").item(0);
    }

    static Node findBitmapFontsNode(Document document) {
        NodeList nodeList = document.getElementsByTagName("BitmapFonts");
        if (nodeList == null) {
            return null;
        }
        return nodeList.item(0);
    }

    /** Stream all children of an XML node. */
    static Stream<Node> childrenStream(Node node) {
        return IntStream.range(0, node.getChildNodes().getLength())
                .mapToObj(index -> node.getChildNodes().item(index));
    }

    /** Safely get an attribute from the given node. Returns null if no such attribute exists. */
    static Optional<String> getNodeAttribute(Node currentNode, String attribute) {
        NamedNodeMap attributes = currentNode.getAttributes();
        if (attributes == null) {
            // A node might have no attributes
            return Optional.empty();
        }
        Node namedItem = attributes.getNamedItem(attribute);
        if (namedItem == null) {
            // A node might not have the required attribute
            return Optional.empty();
        }
        return Optional.of(namedItem.getNodeValue());
    }

    private WatchFaceDocuments() {}
}
