package com.google.wear.watchface.validator

import kotlin.test.assertEquals
import org.junit.Test

class XmlReadersTest {

    @Test
    fun readFromFileReturnsValidXmlDocWithCorrectRootNode() {
        val doc = XmlReader.fromResource("/metaDataWatchFace.xml")

        val result = doc.documentElement.tagName

        assertEquals("WatchFace", result)
    }

    @Test
    fun readFromFileReturnsValidXmlDocWithCorrectChildren() {
        val doc = XmlReader.fromResource("/metaDataWatchFace.xml")

        val result = doc.documentElement.getElementsByTagName("Metadata").length

        assertEquals(3, result)
    }
}
