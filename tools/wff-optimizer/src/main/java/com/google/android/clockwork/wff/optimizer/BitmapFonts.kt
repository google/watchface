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

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import org.w3c.dom.Element

class BitmapFonts {
    val bitmapFonts = mutableMapOf<String, BitmapFont>()
    val bitmapFontMaxSize = mutableMapOf<String, Int>()

    /** Processes the BitmapFonts element and the child BitmapFont elements. */
    fun parseBitmapFonts(element: Element) {
        require(element.tagName == "BitmapFonts")
        val childCharacters = element.getElementsByTagName("BitmapFont")
        for (i in 0 until childCharacters.getLength()) {
            val childNode = childCharacters.item(i)
            if (childNode is Element) {
                val bitmapFont = BitmapFont.parse(childNode)
                bitmapFonts.put(bitmapFont.name, bitmapFont)
            }
        }
    }

    /**
     * Processes the use of a BitmapFont inside of a PartText node, note this is distinct from a
     * BitmapFont node nested within a BitmapFonts node which is handeled by [parseBitmapFonts].
     */
    fun parseBitmapFont(element: Element) {
        require(element.tagName == "BitmapFont")
        val size = element.getIntAttribute("size")!!

        // Update the max requested size for the bitmap font.
        bitmapFontMaxSize.compute(element.getAttribute("family")) { _, prevSize ->
            if (prevSize == null) {
                size
            } else {
                Math.max(size, prevSize)
            }
        }
    }

    /** Returns true if optimizations were applied. */
    fun optimize(imageLoader: ImageLoader): Boolean {
        var optimized = false
        for ((name, bitmapFont) in bitmapFonts) {
            optimized = optimized or bitmapFont.optimize(bitmapFontMaxSize[name]!!, imageLoader)
        }
        return optimized
    }
}

class BitmapFont(val name: String, val characters: Map<String, Character>) {
    class Character(
        val element: Element,
        val name: String,
        val resourceId: String,
        val width: Int,
        val height: Int,
        val marginLeft: Int,
        val marginTop: Int,
        val marginRight: Int,
        val marginBottom: Int,
    )

    /** Returns true if optimizations were applied. */
    fun optimize(maxRequestedHeight: Int, imageLoader: ImageLoader): Boolean {
        var maxHeight = maxRequestedHeight
        var optimizationApplied = false
        for ((_, character) in characters) {
            var image = imageLoader.loadImage(character.resourceId)
            image.referencingElements.add(character.element)
            var nonTransparentBounds = image.nonTransparentBounds
            if (nonTransparentBounds == null) {
                // TODO the image is completely transparent, deal with that.
                continue
            }

            // Crop if needed.
            var croppedImage = image.bufferedImage
            if (nonTransparentBounds.width() != croppedImage.width ||
                nonTransparentBounds.height() != croppedImage.height) {
                croppedImage =
                    croppedImage.getSubimage(
                        nonTransparentBounds.left,
                        nonTransparentBounds.top,
                        nonTransparentBounds.width(),
                        nonTransparentBounds.height())
                optimizationApplied = true

                if (imageLoader.settings.verbose) {
                    System.out.println(
                        "Cropping image ${character.resourceId}: " +
                          "${image.bufferedImage.width}x${image.bufferedImage.height} -> " +
                          "${nonTransparentBounds.width()}x${nonTransparentBounds.height()}")
                }
            }

            // Scale if needed, making sure we don't upscale the image.
            val aspectRatio =
                image.bufferedImage.width.toDouble() / image.bufferedImage.height.toDouble()

            if (maxHeight > image.bufferedImage.height) {
                maxHeight = image.bufferedImage.height
            }

            val maxWidth = maxHeight.toDouble() * aspectRatio
            val scaleX = maxWidth / image.bufferedImage.width.toDouble()
            val scaleY = maxHeight.toDouble() / image.bufferedImage.height.toDouble()
            val newWidth = (nonTransparentBounds.width().toDouble() * scaleX).toInt()
            val newHeight = (nonTransparentBounds.height().toDouble() * scaleY).toInt()
            var marginLeft = nonTransparentBounds.left
            var marginTop = nonTransparentBounds.top
            var marginRight = image.bufferedImage.width - nonTransparentBounds.right
            var marginBottom = image.bufferedImage.height - nonTransparentBounds.bottom

            // If the resized area is smaller, then scale image and bounds.
            if (newWidth * newHeight <
                nonTransparentBounds.width() * nonTransparentBounds.height()) {
                if (imageLoader.settings.verbose) {
                    System.out.println(
                        "Scaling image ${character.resourceId}: " +
                          "${croppedImage.getWidth()}x${croppedImage.getHeight()} -> " +
                          "${newWidth}x${newHeight}")
                }

                val scaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
                val graphics = scaledImage.createGraphics()
                graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                graphics.drawImage(
                    croppedImage,
                    0,
                    0,
                    newWidth,
                    newHeight,
                    0,
                    0,
                    croppedImage.getWidth(),
                    croppedImage.getHeight(),
                    null)
                graphics.dispose()

                marginLeft = (marginLeft.toDouble() * scaleX).toInt()
                marginTop = (marginTop.toDouble() * scaleY).toInt()
                marginRight = (marginRight.toDouble() * scaleX).toInt()
                marginBottom = (marginBottom.toDouble() * scaleY).toInt()
                croppedImage = scaledImage
                character.element.setAttribute(
                    "width", (newWidth + marginLeft + marginRight).toString())
                character.element.setAttribute(
                    "height", (newHeight + marginTop + marginBottom).toString())
                optimizationApplied = true
            }

            // Save margins.
            character.element.setAttribute("marginLeft", marginLeft.toString())
            character.element.setAttribute("marginTop", marginTop.toString())
            character.element.setAttribute("marginRight", marginRight.toString())
            character.element.setAttribute("marginBottom", marginBottom.toString())

            image.optimizedImage = croppedImage
        }

        return optimizationApplied
    }

    companion object {

        fun parse(element: Element): BitmapFont {
            val name = element.getAttribute("name")
            val characters = mutableMapOf<String, Character>()
            val childCharacters = element.getElementsByTagName("Character")

            for (i in 0 until childCharacters.getLength()) {
                val childNode = childCharacters.item(i)
                if (childNode is Element) {
                    val character = parseCharacter(childNode)
                    characters.put(character.name, character)
                }
            }

            return BitmapFont(name, characters)
        }

        private fun parseCharacter(element: Element): Character {
            return Character(
                element,
                element.getAttribute("name"),
                element.getAttribute("resource"),
                element.getIntAttribute("width")!!,
                element.getIntAttribute("height")!!,
                element.getIntAttribute("marginLeft") ?: 0,
                element.getIntAttribute("marginTop") ?: 0,
                element.getIntAttribute("marginRight") ?: 0,
                element.getIntAttribute("marginBottom") ?: 0)
        }
    }
}
