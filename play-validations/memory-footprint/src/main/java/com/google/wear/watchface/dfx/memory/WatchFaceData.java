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

package com.google.wear.watchface.dfx.memory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Contains the information about a watch face necessary to compute the memory footprint of that
 * watch face.
 */
class WatchFaceData {
    /** The system default font on android is Roboto. */
    public static final String SYSTEM_DEFAULT_FONT = "Roboto";

    /** The size of Roboto-Regular.ttf in bytes. */
    public static final int SYSTEM_DEFAULT_FONT_SIZE = 2371712;

    /**
     * The parsed watchface xml documents. A watch face can have multiple layout files for different
     * screen shapes and resolutions.
     */
    List<Document> watchFaceDocuments = new LinkedList<>();

    /**
     * The details for each drawable resource in the watch face package. Each
     * DrawableResourceDetails object contains the maximum size for that specific resource. A
     * resource can be found in different resource sets, with different resolutions for different
     * screen densities. We keep the maximum values for each resource.
     */
    Map<String, DrawableResourceDetails> resourceDetailsMap = new HashMap<>();

    private WatchFaceData() {
        resourceDetailsMap.put(
                SYSTEM_DEFAULT_FONT,
                DrawableResourceDetails.builder()
                        .setName(SYSTEM_DEFAULT_FONT)
                        .setBiggestFrameFootprintBytes(SYSTEM_DEFAULT_FONT_SIZE)
                        .setNumberOfImages(1)
                        .build());
    }

    /**
     * Records a memory footprint for a specific resource in the resourceMemoryMap. The maximum
     * memoryFootprint for any given resource will be kept.
     */
    private void recordResourceDetails(DrawableResourceDetails resourceDetails) {
        if (resourceDetailsMap.containsKey(resourceDetails.getName())) {
            DrawableResourceDetails currentDetailsForResource =
                    resourceDetailsMap.get(resourceDetails.getName());
            resourceDetailsMap.put(
                    resourceDetails.getName(),
                    resourceDetails.maxResourceDetails(currentDetailsForResource));
        } else {
            resourceDetailsMap.put(resourceDetails.getName(), resourceDetails);
        }
    }

    /** Creates a WatchFaceData object from a stream of watch face package resources. */
    static WatchFaceData fromResourcesStream(
            Stream<ArscResource> resources, EvaluationSettings evaluationSettings) {
        WatchFaceData watchFaceData = new WatchFaceData();

        resources.forEach(
                resource -> {
                    if (resource.isWatchFaceXml()) {
                        Document document = parseXmlResource(resource.getData());
                        if (isWatchFaceDocument(document, evaluationSettings)) {
                            watchFaceData.watchFaceDocuments.add(document);
                            return;
                        }
                    }
                    DrawableResourceDetails.fromPackageResource(resource)
                            .ifPresent(watchFaceData::recordResourceDetails);
                });

        if (watchFaceData.watchFaceDocuments.isEmpty()) {
            // this case should be caught by the bundle validation step. If we get here,
            // the issue is on our side.
            throw new InvalidTestRunException("Watch Face does not contain a watchface.xml file");
        }

        return watchFaceData;
    }

    private static Document parseXmlResource(byte[] xmlData) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            return docFactory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlData));
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isWatchFaceDocument(
            Document document, EvaluationSettings evaluationSettings) {
        try {
            String rootNode = WatchFaceDocuments.getWatchFaceRootNode(evaluationSettings);
            XPathExpression xPath =
                    XPathFactory.newInstance().newXPath().compile(String.format("/%s", rootNode));
            return (Boolean) xPath.evaluate(document, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }
}
