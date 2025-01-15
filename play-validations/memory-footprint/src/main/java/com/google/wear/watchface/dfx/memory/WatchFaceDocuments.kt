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

import org.w3c.dom.Document
import org.w3c.dom.Node
import java.util.Optional
import java.util.stream.IntStream
import java.util.stream.Stream

object WatchFaceDocuments {
    @JvmStatic
    fun getWatchFaceRootNode(evaluationSettings: EvaluationSettings): String {
        return if (evaluationSettings.isHoneyfaceMode) "Watchface" else "WatchFace"
    }

    @JvmStatic
    fun isPartAnimatedImage(currentNode: Node): Boolean {
        return currentNode.nodeName == "PartAnimatedImage"
    }

    @JvmStatic
    fun isPartImage(currentNode: Node): Boolean {
        return currentNode.nodeName == "PartImage"
    }

    @JvmStatic
    fun isBitmapFont(currentNode: Node): Boolean {
        return currentNode.nodeName == "BitmapFont"
    }

    @JvmStatic
    fun isFont(currentNode: Node): Boolean {
        return currentNode.nodeName == "Font"
    }

    @JvmStatic
    fun isClock(currentNode: Node): Boolean {
        return currentNode.nodeName == "DigitalClock" || currentNode.nodeName == "AnalogClock"
    }

    @JvmStatic
    fun isDrawableNode(currentNode: Node): Boolean {
        return isPartImage(currentNode) ||
                isPartAnimatedImage(currentNode) ||
                isBitmapFont(currentNode) ||
                isFont(currentNode) ||
                isClock(currentNode)
    }

    @JvmStatic
    fun findSceneNode(document: Document): Node {
        return document.getElementsByTagName("Scene").item(0)
    }

    @JvmStatic
    fun findBitmapFontsNode(document: Document): Node? {
        return document.getElementsByTagName("BitmapFonts")?.item(0)
    }

    /** Stream all children of an XML node. */
    @JvmStatic
    fun childrenStream(node: Node): Stream<Node> {
        return IntStream.range(0, node.childNodes.length)
            .mapToObj { index -> node.childNodes.item(index) }
    }

    /** Safely get an attribute from the given node. Returns null if no such attribute exists. */
    @JvmStatic
    fun getNodeAttribute(currentNode: Node, attribute: String): Optional<String> {
        val attributes = currentNode.attributes ?: return Optional.empty()
        val namedItem = attributes.getNamedItem(attribute) ?: return Optional.empty()
        return Optional.of(namedItem.nodeValue)
    }
}