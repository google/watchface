package com.google.wear.watchface.validator

import kotlin.test.Test
import kotlin.test.assertEquals
import org.w3c.dom.parsing.DOMParser

class JsWatchFaceDocumentTest {

    @Test
    fun ofMethodCreatesADocumentWithSingleElement() {
        val doc =
            DOMParser()
                .parseFromString(
                    """
                      <WatchFace
                          width="450"
                          height="450"
                          clipShape="NONE">
                      </WatchFace>
                    """
                        .trimIndent(),
                    "application/xml",
                )

        val result = JsWatchFaceDocument.of(doc).rootElement

        assertEquals(
            WatchFaceElement(
                "WatchFace",
                mapOf("width" to "450", "height" to "450", "clipShape" to "NONE"),
                emptyList(),
            ),
            result,
        )
    }

    @Test
    fun ofMethodCreatesADocumentWithManyChildren() {
        val doc =
            DOMParser()
                .parseFromString(
                    """
                        <WatchFace
                          width="450"
                          height="450"
                          clipShape="NONE"
                        >
                            <Metadata
                                key="CLOCK_TYPE"
                                value="ANALOG"/>
                            <Metadata
                                key="USE_NOTIFICATION"
                                value="TRUE"/>
                            <Metadata
                                key="STEP_GOAL"
                                value="10000"/>
                      </WatchFace>
                    """
                        .trimIndent(),
                    "application/xml",
                )

        val result = JsWatchFaceDocument.of(doc).rootElement.children

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
