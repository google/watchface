/*
 * Copyright 2024 Google LLC
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

package com.google.android.clockwork.wff.optimizer

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

class Optimizer(val document: Document, val settings: Settings) {
    val bitmapFonts = BitmapFonts()
    val imageLoader = ImageLoader(settings)
    val partImages = PartImages()

    fun walkTree() {
        visit(document.getDocumentElement())
    }

    private fun visit(element: Element) {
        when (element.getTagName()) {
            "BitmapFonts" -> bitmapFonts.parseBitmapFonts(element)
            "BitmapFont" -> bitmapFonts.parseBitmapFont(element)
            "PartImage" -> partImages.parsePartImage(element)
            else -> {
                val children = element.getChildNodes()
                for (i in 0 until children.getLength()) {
                    val child = children.item(i)
                    if (child is Element) {
                        visit(child)
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Applies the following optimizations:
         * 1. Crops and resizes BitmapFonts, adding margins to the Character tag to ensure
         *    alignment.
         * 2. Crops and resizes PartImage nodes.
         * 3. Attempts to qualitze images to RGB565 where there will be no noticeable loss of
         *    fidelity.
         * 4. Deduplicates image resources
         */
        fun optimize(xmlFile: File, settings: Settings) {
            val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document = builder.parse(xmlFile)
            document.getDocumentElement().normalize()
            val optimizer = Optimizer(document, settings)
            optimizer.walkTree()
            var saveNeed = optimizer.bitmapFonts.optimize(optimizer.imageLoader)
            if (optimizer.partImages.optimize(optimizer.imageLoader)) {
                saveNeed = true
            }
            optimizer.imageLoader.maybeQuantizeImagesToRGB565()
            if (optimizer.imageLoader.dedupeAndWriteOptimizedImages()) {
                saveNeed = true
            }
            if (saveNeed) {
                val transformer = TransformerFactory.newInstance().newTransformer()
                val source = DOMSource(document)
                val result = StreamResult(xmlFile)
                transformer.transform(source, result)
            }
        }
    }
}
