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

package com.google.wear.watchface.dfx.memory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Centralized package-private XML DOM parser helpers.
 *
 * <p>This module runs both on standard JVMs (Java 17+, where {@code :validator} places Apache
 * {@code xercesImpl} on the classpath for XSD 1.1 validation) and inside the Android validation
 * library ({@code minSdk 28}, {@code compileSdk 34}). Centralizing XML setup in a package-private
 * Java class keeps these helpers internal in JVM bytecode while handling three platform differences
 * in one place:
 *
 * <ol>
 *   <li><b>Preferring the platform default {@code DocumentBuilderFactory}:</b> On standard JVMs
 *       (Java 9+), {@code DocumentBuilderFactory.newDefaultInstance()} selects the JDK's built-in
 *       DOM parser instead of Apache {@code xercesImpl} from {@code META-INF/services}, avoiding
 *       {@code xercesImpl}'s UTF-8 surrogate-pair buffer boundary bug. Android's SDK ({@code
 *       android.jar} / {@code libcore}) does not declare {@code newDefaultInstance()}, so {@code
 *       newDefaultInstance()} is invoked reflectively and falls back to {@code newInstance()},
 *       which on Android ignores {@code META-INF/services} and instantiates the platform {@code
 *       org.apache.harmony.xml} parser directly.
 *   <li><b>Consistent single-pass DTD and entity restrictions across JVM and Android:</b> Watch
 *       face, manifest, and resource XML files never use {@code <!DOCTYPE>} declarations or
 *       external entities. Standard JVM parsers reject {@code <!DOCTYPE>} and external entities
 *       directly via {@code disallow-doctype-decl} and {@code FEATURE_SECURE_PROCESSING}. Android's
 *       {@code org.apache.harmony.xml} DOM parser ({@code KXmlParser} with {@code processDocDecl ==
 *       false}) performs no external I/O or custom entity expansion, attaches a {@code
 *       DocumentType} node when a {@code <!DOCTYPE>} is present (rejected via {@link
 *       Document#getDoctype()}), and throws {@link DOMException} on duplicate {@code <!DOCTYPE>} or
 *       root nodes (normalized to {@link SAXException}).
 *   <li><b>Allowing high counts of built-in XML character entities:</b> Once {@code <!DOCTYPE>} and
 *       custom entities are disallowed on the factory, lifting JDK's built-in entity count limits
 *       ({@code maxGeneralEntitySizeLimit} and {@code totalEntitySizeLimit}) allows generated watch
 *       face layouts with &gt;100k {@code &quot;} references.
 * </ol>
 */
final class XmlParsers {
    private static final String ANDROID_DOCUMENT_BUILDER_FACTORY_CLASS =
            "org.apache.harmony.xml.parsers.DocumentBuilderFactoryImpl";
    private static final String DISALLOW_DOCTYPE_DECL_FEATURE =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES_FEATURE =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES_FEATURE =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD_FEATURE =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String JDK_MAX_GENERAL_ENTITY_SIZE_LIMIT =
            "jdk.xml.maxGeneralEntitySizeLimit";
    private static final String JDK_TOTAL_ENTITY_SIZE_LIMIT = "jdk.xml.totalEntitySizeLimit";

    private XmlParsers() {}

    private static DocumentBuilderFactory createDocumentBuilderFactory() {
        try {
            return (DocumentBuilderFactory)
                    DocumentBuilderFactory.class.getMethod("newDefaultInstance").invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            return DocumentBuilderFactory.newInstance();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        }
    }

    private static boolean isAndroidFactory(DocumentBuilderFactory factory) {
        return ANDROID_DOCUMENT_BUILDER_FACTORY_CLASS.equals(factory.getClass().getName());
    }

    private static boolean trySetFeature(DocumentBuilderFactory factory, String name, boolean value)
            throws ParserConfigurationException {
        try {
            factory.setFeature(name, value);
            return true;
        } catch (ParserConfigurationException e) {
            if (!isAndroidFactory(factory)) {
                throw e;
            }
            return false;
        }
    }

    static Document parseDocument(byte[] xmlBytes)
            throws ParserConfigurationException, SAXException, IOException {
        return parseDocument(xmlBytes, /* allowUnboundedBuiltinEntities= */ false);
    }

    /**
     * Parses {@code xmlBytes} into a namespace-aware DOM {@link Document}, rejecting any {@code
     * <!DOCTYPE>} or external entity declarations across both JVM and Android runtimes.
     */
    static Document parseDocument(byte[] xmlBytes, boolean allowUnboundedBuiltinEntities)
            throws ParserConfigurationException, SAXException, IOException {
        return parseDocument(
                xmlBytes, allowUnboundedBuiltinEntities, createDocumentBuilderFactory());
    }

    static Document parseDocument(
            byte[] xmlBytes, boolean allowUnboundedBuiltinEntities, DocumentBuilderFactory factory)
            throws ParserConfigurationException, SAXException, IOException {
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try {
            factory.setXIncludeAware(false);
        } catch (UnsupportedOperationException e) {
            if (!isAndroidFactory(factory)) {
                throw e;
            }
        }

        boolean secureProcessing =
                trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        boolean doctypeDisallowed = trySetFeature(factory, DISALLOW_DOCTYPE_DECL_FEATURE, true);
        boolean externalGeneralDisabled =
                trySetFeature(factory, EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
        boolean externalParameterDisabled =
                trySetFeature(factory, EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
        boolean loadExternalDtdDisabled = trySetFeature(factory, LOAD_EXTERNAL_DTD_FEATURE, false);
        boolean featuresConfigured =
                secureProcessing
                        && doctypeDisallowed
                        && externalGeneralDisabled
                        && externalParameterDisabled
                        && loadExternalDtdDisabled;

        if (allowUnboundedBuiltinEntities && featuresConfigured) {
            try {
                // Safe because DISALLOW_DOCTYPE_DECL_FEATURE succeeded above, preventing custom
                // entity declarations and leaving only non-amplifying built-in character entities
                // (such as &quot;).
                factory.setAttribute(JDK_MAX_GENERAL_ENTITY_SIZE_LIMIT, 0);
                factory.setAttribute(JDK_TOTAL_ENTITY_SIZE_LIMIT, 0);
            } catch (IllegalArgumentException ignored) {
                // Ignored on parsers that do not define JDK entity size limit attributes.
            }
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver(
                (publicId, systemId) -> {
                    throw new SAXException("External entity resolution is disallowed");
                });
        return parseWithBuilder(builder, xmlBytes);
    }

    static Document parseWithBuilder(DocumentBuilder builder, byte[] xmlBytes)
            throws SAXException, IOException {
        final Document document;
        try {
            document = builder.parse(new ByteArrayInputStream(xmlBytes));
        } catch (DOMException e) {
            throw new SAXException(e.getMessage(), e);
        }
        if (document.getDoctype() != null) {
            throw new SAXParseException("DOCTYPE is disallowed", null);
        }
        return document;
    }
}
