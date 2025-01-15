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

package com.google.wear.watchface.dfx.memory

import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.streams.asSequence

internal class WatchFaceData private constructor() {

    /** Mutable backing field for [watchFaceDocuments]. */
    private val _watchFaceDocuments = mutableListOf<Document>()

    /**
     * The parsed watchface xml documents. A watch face can have multiple layout files for different
     * screen shapes and resolutions.
     */
    val watchFaceDocuments: List<Document> = _watchFaceDocuments

    /** Mutable backing field for [resourceDetailsMap]. */
    private val _resourceDetailsMap = mutableMapOf<String, DrawableResourceDetails>()

    /**
     * The details for each drawable resource in the watch face package. Each
     * DrawableResourceDetails object contains the maximum size for that specific resource. A
     * resource can be found in different resource sets, with different resolutions for different
     * screen densities. We keep the maximum values for each resource.
     */
    val resourceDetailsMap: MutableMap<String, DrawableResourceDetails> = _resourceDetailsMap

    init {
        // Add the default font, since a watch face doesn't have to include it in its resources.
        _resourceDetailsMap[SYSTEM_DEFAULT_FONT] =
            DrawableResourceDetails.Builder()
                .setName(SYSTEM_DEFAULT_FONT)
                .setBiggestFrameFootprintBytes(SYSTEM_DEFAULT_FONT_SIZE)
                .setNumberOfImages(1)
                .build()
    }

    /**
     * Records a memory footprint for a specific resource in the resourceMemoryMap. The maximum
     * memoryFootprint for any given resource will be kept.
     */
    private fun recordResourceDetails(resourceDetails: DrawableResourceDetails) {
        val nextResourceDetails =
            if (_resourceDetailsMap.containsKey(resourceDetails.name)) {
                resourceDetails.maxResourceDetails(_resourceDetailsMap[resourceDetails.name])
            } else {
                resourceDetails
            }
        _resourceDetailsMap[resourceDetails.name] = nextResourceDetails
    }

    companion object {
        /** The system default font on android is Roboto. */
        const val SYSTEM_DEFAULT_FONT: String = "Roboto"

        /** The size of Roboto-Regular.ttf in bytes. */
        const val SYSTEM_DEFAULT_FONT_SIZE: Long = 2371712

        private fun parseXmlResource(xmlData: ByteArray): Document {
            val docFactory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            return docFactory.newDocumentBuilder().parse(ByteArrayInputStream(xmlData))
        }

        @JvmStatic
        fun fromResourcesStream(
            resources: Stream<AndroidResource>,
            evaluationSettings: EvaluationSettings
        ) = fromResourcesStream(resources.asSequence(), evaluationSettings)

        /** Creates a WatchFaceData object from a stream of watch face package resources. */
        @JvmStatic
        fun fromResourcesStream(
            resources: Sequence<AndroidResource>,
            evaluationSettings: EvaluationSettings,
        ): WatchFaceData {
            val watchFaceData = WatchFaceData()

            for (resource in resources) {
                if (resource.isWatchFaceXml()) {
                    val document = parseXmlResource(resource.data)
                    if (isWatchFaceDocument(document, evaluationSettings)) {
                        watchFaceData._watchFaceDocuments.add(document)
                        continue
                    }
                }
                DrawableResourceDetails.fromPackageResource(resource).ifPresent {
                    watchFaceData.recordResourceDetails(it)
                }
            }

            return watchFaceData
        }

        private fun isWatchFaceDocument(
            document: Document,
            evaluationSettings: EvaluationSettings
        ): Boolean {
            val rootNode = WatchFaceDocuments.getWatchFaceRootNode(evaluationSettings)
            val xPath =
                XPathFactory.newInstance().newXPath().compile(String.format("/%s", rootNode))
            return xPath.evaluate(document, XPathConstants.BOOLEAN) as Boolean
        }
    }
}
