package com.google.wear.watchface.validator

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

/** A [[WatchFaceDocument]] implementation for the JVM source set. */
class JvmWatchFaceDocument private constructor(override val rootElement: WatchFaceElement) :
    WatchFaceDocument {

    companion object {
        /** Factory method for creating a WatchFaceDocument from a w3c Document. */
        fun of(document: Document): WatchFaceDocument =
            JvmWatchFaceDocument(toWatchFaceElement(document.documentElement))

        /** Traverses the element tree and returns the corresponding WatchFaceElement tree. */
        private fun toWatchFaceElement(current: Element): WatchFaceElement =
            WatchFaceElement(
                current.tagName,
                current.getProperties(),
                current.getChildElements().map { toWatchFaceElement(it) }.toList(),
                current.textContent,
            )

        /**
         * Gets an elements children as a List of Elements.
         *
         * Traverses through the 'siblings' of the first child and collects the nodes which are
         * elements into a sequence.
         */
        private fun Element.getChildElements(): Sequence<Element> = sequence {
            var currentNode: Node? = firstChild

            while (currentNode != null) {
                if (currentNode is Element) {
                    yield(currentNode)
                }
                currentNode = currentNode.nextSibling
            }
        }

        /** Gets an elements properties as a list of key value pairs. */
        private fun Element.getProperties(): Map<String, String> =
            (0 until attributes.length)
                .map { attributes.item(it) }
                .filterNot { it.nodeName.startsWith("xmlns:") || it.nodeName.startsWith("xsi:") }
                .associate { it.nodeName to it.nodeValue }
    }
}
