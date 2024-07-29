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

class PartImage(
    val element: Element,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

class PartImages {
    val imageUsage = mutableMapOf<String, ArrayList<PartImage>>()

    fun parsePartImage(element: Element) {
        val childImages = element.getElementsByTagName("Image")
        if (childImages.getLength() != 1) {
            return
        }

        val childImage = childImages.item(0)
        if (childImage is Element) {
            val resourceId = childImage.getAttribute("resource")
            if (resourceId == "") {
                return
            }
            imageUsage
                .getOrPut(resourceId) { ArrayList<PartImage>() }
                .add(
                    PartImage(
                        element,
                        element.getDoubleAttribute("x")!!,
                        element.getDoubleAttribute("y")!!,
                        element.getDoubleAttribute("width")!!,
                        element.getDoubleAttribute("height")!!
                    )
                )
        }
    }

    fun optimize(imageLoader: ImageLoader): Boolean {
        var optimizationApplied = false
        for ((resourceId, partImages) in imageUsage) {
            val image =
                try {
                    imageLoader.loadImage(resourceId)
                } catch (e: Exception) {
                    System.out.println("Skipping image $resourceId which could not be loaded")
                    continue
                }

            var maxWidth = 0
            var maxHeight = 0
            for (partImage in partImages) {
                maxWidth = Math.max(maxWidth, partImage.width.toInt())
                maxHeight = Math.max(maxHeight, partImage.height.toInt())
                image.referencingElements.add(
                    partImage.element.getElementsByTagName("Image").item(0) as Element
                )
            }

            var nonTransparentBounds = image.nonTransparentBounds
            if (nonTransparentBounds == null) {
                // TODO the image is completely transparent, deal with that.
                continue
            }

            // Crop if needed.
            var croppedImage = image.bufferedImage
            if (
                nonTransparentBounds.width() != croppedImage.width ||
                    nonTransparentBounds.height() != croppedImage.height
            ) {
                croppedImage =
                    croppedImage.getSubimage(
                        nonTransparentBounds.left,
                        nonTransparentBounds.top,
                        nonTransparentBounds.width(),
                        nonTransparentBounds.height()
                    )
                optimizationApplied = true

                if (imageLoader.settings.verbose) {
                    System.out.println(
                        "Cropping image ${resourceId}: " +
                            "${image.bufferedImage.width}x${image.bufferedImage.height} -> " +
                            "${nonTransparentBounds.width()}x${nonTransparentBounds.height()}"
                    )
                }
            }

            // Scale if needed, making sure we don't upscale the image.
            if (maxHeight > image.bufferedImage.height) {
                maxHeight = image.bufferedImage.height
            }
            val scaleX = maxWidth.toDouble() / image.bufferedImage.width.toDouble()
            val scaleY = maxHeight.toDouble() / image.bufferedImage.height.toDouble()
            var scaledUncroppedWidth = image.bufferedImage.width.toDouble()
            var scaledUncroppedHeight = image.bufferedImage.height.toDouble()

            // If the resized area is smaller, then scale image and bounds.
            if (
                maxWidth * maxHeight < nonTransparentBounds.width() * nonTransparentBounds.height()
            ) {
                if (imageLoader.settings.verbose) {
                    System.out.println(
                        "Scaling image ${resourceId}: " +
                            "${croppedImage.getWidth()}x${croppedImage.getHeight()} -> " +
                            "${maxWidth}x${maxHeight}"
                    )
                }

                val scaledImage = BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB)
                val graphics = scaledImage.createGraphics()
                graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )
                graphics.drawImage(
                    croppedImage,
                    0,
                    0,
                    maxWidth,
                    maxHeight,
                    0,
                    0,
                    croppedImage.getWidth(),
                    croppedImage.getHeight(),
                    null
                )
                graphics.dispose()

                nonTransparentBounds =
                    Bounds(
                        (nonTransparentBounds.left.toDouble() * scaleX).toInt(),
                        (nonTransparentBounds.top.toDouble() * scaleY).toInt(),
                        (nonTransparentBounds.right.toDouble() * scaleX).toInt(),
                        (nonTransparentBounds.bottom.toDouble() * scaleY).toInt(),
                    )
                scaledUncroppedWidth *= scaleX
                scaledUncroppedHeight *= scaleY

                croppedImage = scaledImage
                optimizationApplied = true
            }

            for (partImage in partImages) {
                val invScaleX = partImage.width.toDouble() / scaledUncroppedWidth
                val invScaleY = partImage.height.toDouble() / scaledUncroppedHeight
                val newX = partImage.x + nonTransparentBounds.left.toDouble() * invScaleX
                val newY = partImage.y + nonTransparentBounds.top.toDouble() * invScaleY
                val newWidth = nonTransparentBounds.width().toDouble() * invScaleX
                val newHeight = nonTransparentBounds.height().toDouble() * invScaleY

                partImage.element.setAttribute("x", newX.toInt().toString())
                partImage.element.setAttribute("y", newY.toInt().toString())
                partImage.element.setAttribute("width", newWidth.toInt().toString())
                partImage.element.setAttribute("height", newHeight.toInt().toString())

                partImage.element.getDoubleAttribute("pivotX")?.let {
                    val oldPivotXPixel = partImage.x + partImage.width.toDouble() * it
                    val newPivotXFraction = (oldPivotXPixel - newX) / newWidth
                    partImage.element.setAttribute("pivotX", newPivotXFraction.toString())
                }

                partImage.element.getDoubleAttribute("pivotY")?.let {
                    val oldPivotYPixel = partImage.y + partImage.height.toDouble() * it
                    val newPivotYFraction = (oldPivotYPixel - newY) / newHeight
                    partImage.element.setAttribute("pivotY", newPivotYFraction.toString())
                }
            }

            image.optimizedImage = croppedImage
        }

        return optimizationApplied
    }
}
