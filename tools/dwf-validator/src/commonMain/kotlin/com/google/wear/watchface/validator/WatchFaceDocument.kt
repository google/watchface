package com.google.wear.watchface.validator

/**
 * An interface for a Declarative Watchface XML Document.
 *
 * @property rootElement the root node of the XML element tree.
 */
interface WatchFaceDocument {
    val rootElement: WatchFaceElement
}
