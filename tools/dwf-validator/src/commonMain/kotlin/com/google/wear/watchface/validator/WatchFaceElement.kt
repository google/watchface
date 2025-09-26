package com.google.wear.watchface.validator

/**
 * A data class for storing an abstract XML element.
 *
 * @property tagName the elements name.
 * @property attributes key value pairs stored in the element's tag.
 * @property children a list of immediately nested elements.
 */
data class WatchFaceElement(
    val tagName: String,
    val attributes: Map<String, String>,
    val children: List<WatchFaceElement>,
    val textContent: String = "",
)
