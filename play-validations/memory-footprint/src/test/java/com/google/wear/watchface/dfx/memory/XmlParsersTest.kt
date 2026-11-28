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
import javax.xml.parsers.ParserConfigurationException
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

    @Test
    fun parseDocument_nonAndroidFactoryWithoutSecureFeatures_rethrowsParserConfigurationException() {
        val unsupportedJvmFactory =
            object : DocumentBuilderFactory() {
                override fun newDocumentBuilder(): DocumentBuilder =
                    newDefaultInstance().newDocumentBuilder()

                override fun setAttribute(name: String, value: Any) {
                    throw IllegalArgumentException(name)
                }

                override fun getAttribute(name: String): Any =
                    throw IllegalArgumentException(name)

                override fun setFeature(name: String, value: Boolean) {
                    throw ParserConfigurationException(name)
                }

                override fun getFeature(name: String): Boolean =
                    throw ParserConfigurationException(name)
            }

        assertThrows(ParserConfigurationException::class.java) {
            XmlParsers.parseDocument("<WatchFace/>".toByteArray(), false, unsupportedJvmFactory)
        }
    }

    @Test
    fun parseDocument_nonAndroidFactoryThrowingOnSetXIncludeAware_rethrowsUnsupportedOperationException() {
        val xIncludeUnsupportedFactory =
            object : DocumentBuilderFactory() {
                override fun newDocumentBuilder(): DocumentBuilder =
                    newDefaultInstance().newDocumentBuilder()

                override fun setAttribute(name: String, value: Any) {
                    throw IllegalArgumentException(name)
                }

                override fun getAttribute(name: String): Any =
                    throw IllegalArgumentException(name)

                override fun setFeature(name: String, value: Boolean) {}

                override fun getFeature(name: String): Boolean = false

                override fun setXIncludeAware(state: Boolean) {
                    throw UnsupportedOperationException("XInclude unsupported")
                }
            }

        assertThrows(UnsupportedOperationException::class.java) {
            XmlParsers.parseDocument("<WatchFace/>".toByteArray(), false, xIncludeUnsupportedFactory)
        }
    }

    @Test
    fun parseDocument_factoryIgnoringFeatures_disablesExpandEntitiesAndRejectsExternalEntityResolution() {
        val domImpl = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().domImplementation
        val configuredFeatures = mutableMapOf<String, Boolean>()
        var xIncludeAwareSet: Boolean? = null
        val permissiveFactory =
            object : DocumentBuilderFactory() {
                override fun newDocumentBuilder(): DocumentBuilder =
                    object : DocumentBuilder() {
                        private var resolver: EntityResolver? = null

                        override fun parse(inputSource: InputSource): Document {
                            resolver!!.resolveEntity(null, "http://127.0.0.1:9/ext.dtd")
                            return domImpl.createDocument(null, "WatchFace", null)
                        }

                        override fun isNamespaceAware(): Boolean = true

                        override fun isValidating(): Boolean = false

                        override fun setEntityResolver(er: EntityResolver?) {
                            resolver = er
                        }

                        override fun setErrorHandler(eh: ErrorHandler?) {}

                        override fun newDocument(): Document = throw UnsupportedOperationException()

                        override fun getDOMImplementation(): DOMImplementation = domImpl
                    }

                override fun setAttribute(name: String, value: Any) {}

                override fun getAttribute(name: String): Any =
                    throw IllegalArgumentException(name)

                override fun setFeature(name: String, value: Boolean) {
                    configuredFeatures[name] = value
                }

                override fun getFeature(name: String): Boolean =
                    configuredFeatures[name] ?: false

                override fun setXIncludeAware(state: Boolean) {
                    xIncludeAwareSet = state
                }
            }

        val ex =
            assertThrows(SAXException::class.java) {
                XmlParsers.parseDocument("<WatchFace/>".toByteArray(), false, permissiveFactory)
            }

        assertThat(ex).hasMessageThat().isEqualTo("External entity resolution is disallowed")
        assertThat(permissiveFactory.isExpandEntityReferences).isFalse()
        assertThat(xIncludeAwareSet).isFalse()
        assertThat(configuredFeatures["http://apache.org/xml/features/nonvalidating/load-external-dtd"])
            .isFalse()
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
