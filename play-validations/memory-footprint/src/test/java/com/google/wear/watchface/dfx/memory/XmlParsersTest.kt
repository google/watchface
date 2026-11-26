/*
 * Copyright 2026 Google LLC
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

import com.google.common.truth.Truth.assertThat
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.w3c.dom.DOMException
import org.w3c.dom.DOMImplementation
import org.w3c.dom.Document
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException

@RunWith(JUnit4::class)
class XmlParsersTest {

    @Test
    fun parseDocument_rejectsInternalEntities() {
        val xmlWithInternalEntities =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE WatchFace [
              <!ENTITY v1 "aaaaaaaaaa">
              <!ENTITY v2 "&v1;&v1;&v1;&v1;&v1;">
            ]>
            <WatchFace width="450" height="450"><Scene><Group name="&v2;"/></Scene></WatchFace>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        assertThrows(SAXParseException::class.java) {
            XmlParsers.parseDocument(xmlWithInternalEntities)
        }
    }

    @Test
    fun parseDocument_rejectsMultipleDoctypes() {
        val xmlWithMultipleDoctypes =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE WatchFace>
            <!DOCTYPE WatchFace>
            <WatchFace width="450" height="450"><Scene><Group name="main"/></Scene></WatchFace>
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)

        assertThrows(SAXException::class.java) {
            XmlParsers.parseDocument(xmlWithMultipleDoctypes)
        }
    }

    @Test
    fun parseWithBuilder_rejectsDocumentContainingDoctypeNode() {
        val domImpl = DocumentBuilderFactory.newInstance().newDocumentBuilder().domImplementation
        val docWithDoctype =
            domImpl.createDocument(
                null,
                "WatchFace",
                domImpl.createDocumentType("WatchFace", null, null),
            )
        val stubBuilder = stubDocumentBuilder { docWithDoctype }

        assertThrows(SAXParseException::class.java) {
            XmlParsers.parseWithBuilder(stubBuilder, "<WatchFace/>".toByteArray())
        }
    }

    @Test
    fun parseWithBuilder_wrapsDomExceptionIntoSaxException() {
        val stubBuilder = stubDocumentBuilder {
            throw DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Only one DOCTYPE element allowed")
        }

        assertThrows(SAXException::class.java) {
            XmlParsers.parseWithBuilder(stubBuilder, "<WatchFace/>".toByteArray())
        }
    }

    @Test
    fun parseDocument_preservesValidElementsAndComments() {
        val xml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- layout comment -->
            <WatchFace width="450" height="450">
              <Scene><Group name="main"/></Scene>
            </WatchFace>
            """.trimIndent()

        val doc = XmlParsers.parseDocument(xml.toByteArray(StandardCharsets.UTF_8))

        assertThat(doc.documentElement.tagName).isEqualTo("WatchFace")
        assertThat(doc.getElementsByTagName("Group").length).isEqualTo(1)
    }

    private fun stubDocumentBuilder(onParse: () -> Document): DocumentBuilder =
        object : DocumentBuilder() {
            override fun parse(inputSource: InputSource): Document = onParse()

            override fun isNamespaceAware(): Boolean = true

            override fun isValidating(): Boolean = false

            override fun setEntityResolver(er: EntityResolver?) {}

            override fun setErrorHandler(eh: ErrorHandler?) {}

            override fun newDocument(): Document = throw UnsupportedOperationException()

            override fun getDOMImplementation(): DOMImplementation =
                throw UnsupportedOperationException()
        }
}
