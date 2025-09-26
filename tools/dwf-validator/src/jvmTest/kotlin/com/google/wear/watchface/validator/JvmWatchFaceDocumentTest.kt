package com.google.wear.watchface.validator

import kotlin.test.assertEquals
import org.junit.Test

class JvmWatchFaceDocumentTest {
    @Test
    fun ofMethodCreatesADocumentWithSingleElement() {
        val doc = XmlReader.fromResource("/emptyWatchFace.xml")

        val result = JvmWatchFaceDocument.of(doc).rootElement

        assertEquals(
            WatchFaceElement(
                "WatchFace",
                mapOf("clipShape" to "NONE", "height" to "450", "width" to "450"),
                emptyList(),
            ),
            result,
        )
    }

    @Test
    fun ofMethodCreatesADocumentWithManyChildren() {
        val doc = XmlReader.fromResource("/metaDataWatchFace.xml")

        val result = JvmWatchFaceDocument.of(doc).rootElement.children

        assertEquals(
            listOf(
                WatchFaceElement(
                    "Metadata",
                    mapOf("key" to "CLOCK_TYPE", "value" to "ANALOG"),
                    emptyList(),
                ),
                WatchFaceElement(
                    "Metadata",
                    mapOf("key" to "USE_NOTIFICATION", "value" to "TRUE"),
                    emptyList(),
                ),
                WatchFaceElement(
                    "Metadata",
                    mapOf("key" to "STEP_GOAL", "value" to "10000"),
                    emptyList(),
                ),
            ),
            result,
        )
    }
}
