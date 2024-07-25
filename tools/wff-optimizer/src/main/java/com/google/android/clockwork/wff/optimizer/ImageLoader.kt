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

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.security.MessageDigest
import java.util.ArrayList
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import org.w3c.dom.Element

open class ImageLoader(val settings: Settings) {
    private val images = mutableMapOf<String, Image>()
    private val resourceNameToFile = mutableMapOf<String, File>()

    init {
        findAllImageFiles()
    }

    private fun findAllImageFiles() {
        val resDirectory = File(settings.sourcePath, "res")
        val drawableDirectories = resDirectory.listFiles { f -> f.name.startsWith("drawable") }
        for (dir in drawableDirectories) {
            for (file in dir.listFiles()) {
                resourceNameToFile.put(file.nameWithoutExtension, file)
            }
        }
    }

    fun loadImage(name: String): Image {
        try {
            val image =
                images.getOrPut(name) {
                    val file = resourceNameToFile[name]!!
                    FileImageInputStream(file).use { imageInputStream ->
                        val imageReaders = ImageIO.getImageReaders(imageInputStream)
                        val reader = imageReaders.next()
                        reader.setInput(imageInputStream)
                        val image = reader.read(0)
                        loadImageInternal(name, file, image)
                    }
                }
            return image
        } catch (e: Exception) {
            throw IOException("Error could not load resource '$name'.", e)
        }
    }

    protected open fun loadImageInternal(name: String, file: File, image: BufferedImage) =
        Image(name, file, image, computeNonTransparentBounds(image))

    /** Attempts to quantize images to RGB565 if theres almost no loss of visual fidelity. */
    fun maybeQuantizeImagesToRGB565() {
        for ((_, image) in images) {
            image.maybeQuantizeToRGB565(settings)
        }
    }

    /** Returns true if any deduplication was done. */
    fun dedupeAndWriteOptimizedImages(): Boolean {
        var deduped = false
        val sha1ToImages = mutableMapOf<String, ArrayList<Image>>()
        for ((_, image) in images) {
            sha1ToImages.getOrPut(image.computeSha1()) { ArrayList<Image>() }.add(image)
        }

        for ((_, imageList) in sha1ToImages) {
            imageList[0].maybeWriteOptimizedImage()

            // Make any elements that referenced duplicate resources point to the first one.
            for (i in 1 until imageList.size) {
                for (element in imageList[i].referencingElements) {
                    element.setAttribute("resource", imageList[0].name)
                    deduped = true
                }
            }

            if (settings.verbose && imageList.size > 1) {
                System.out.println("Deduped: " + imageList.joinToString(", ") { it.file.name })
            }
        }

        return deduped
    }
}

/**
 * A lookup table used for computing the loss of precision when an 8bit value is quantized to a 5bit
 * value.
 */
private val QUANTIZATION_ERROR_LUT5 = create8bppToNbppQuantizationErrorLookUpTable(5)

/**
 * A lookup table used for computing the loss of precision when an 8bit value is quantized to a 6bit
 * value.
 */
private val QUANTIZATION_ERROR_LUT6 = create8bppToNbppQuantizationErrorLookUpTable(6)

/** Constructs a table of the error introduced by quantizing an 8 bit value to a N bit value. */
private fun create8bppToNbppQuantizationErrorLookUpTable(n: Int): IntArray {
    val table = IntArray(256)
    val bitsLost = 8 - n
    val twoPowN = 1 shl bitsLost
    val halfTwoPowN = 1 shl bitsLost - 1
    for (i in 0..255) {
        // This rounds i to the nearest n-bit value before converting back to an 8 bit value.
        val quantizedValue = Math.min((i + halfTwoPowN) / twoPowN * twoPowN, 255)

        // Record the error due to qualtization in the table. This has a saw-tooth pattern where
        // n-bit values that correspond directly to 8 bit ones have an error of 0, rising to a
        // maximum error of halfPlusOne in between.
        table[i] = Math.abs(i - quantizedValue)
    }
    return table
}

/** This corresponds to an average difference in luminosity of 5/10th of an 8bit value. */
const val MAX_ACCEPTIABLE_QUANTIZATION_ERROR = 0.5

open class Image(
    val name: String,
    val file: File,
    val bufferedImage: BufferedImage,
    val nonTransparentBounds: Bounds?,
) {
    val referencingElements = ArrayList<Element>()
    var optimizedImage: BufferedImage? = null

    open fun maybeWriteOptimizedImage() {
        optimizedImage?.let { ImageIO.write(it, "png", file) }
    }

    fun computeSha1(): String {
        var md = MessageDigest.getInstance("SHA-1")
        val baos = ByteArrayOutputStream()
        ImageIO.write(optimizedImage ?: bufferedImage, "png", baos)
        return byteArray2Hex(md.digest(baos.toByteArray()))
    }

    /** Error statistics for quantizing the image to 565. */
    private class QuantizeToRGB565Result {
        var visiblePixels: Long = 0
        var visiblePixelQuantizationErrorSum: Long = 0
        val visibleError: Double
            get() = visiblePixelQuantizationErrorSum.toDouble() / visiblePixels.toDouble()
    }

    /** Quantizes the image to RGB565. */
    private fun quantizeToRGB565(): QuantizeToRGB565Result {
        val image = optimizedImage ?: bufferedImage
        val width = image.width
        val height = image.height
        val result = QuantizeToRGB565Result()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb: Int = image.getRGB(x, y)
                val a = argb shr 24 and 0xff
                if (a < 255) {
                    // Don't quantize if the alpha channel is used.
                    result.visiblePixels = 1
                    result.visiblePixelQuantizationErrorSum = 1
                    return result
                }
                result.visiblePixels++
                val r = argb shr 16 and 0xff
                val g = argb shr 8 and 0xff
                val b = argb and 0xff
                result.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT5.get(r).toLong()
                result.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT6.get(g).toLong()
                result.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT5.get(b).toLong()
            }
        }
        return result
    }

    fun maybeQuantizeToRGB565(settings: Settings) {
        val quantizeResult = quantizeToRGB565()
        if (quantizeResult.visibleError < MAX_ACCEPTIABLE_QUANTIZATION_ERROR) {
            val image = optimizedImage ?: bufferedImage
            val quantizedImage =
                BufferedImage(image.width, image.height, BufferedImage.TYPE_USHORT_565_RGB)
            val graphics = quantizedImage.createGraphics()
            graphics.drawImage(
                image,
                0,
                0,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                null
            )
            graphics.dispose()

            optimizedImage = quantizedImage

            if (settings.verbose) {
                System.out.println("Converted image to RGB565: $name")
            }
        }
    }
}

class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun width() = right - left

    fun height() = bottom - top
}

/**
 * Computes the [Bounds] of the visible pixels within the supplied [image].
 *
 * @param image the [BufferedImage]
 * @return The [Bounds] of the visible pixels, or `null` if the whole image is fully transparent.
 */
fun computeNonTransparentBounds(image: BufferedImage): Bounds? {
    var top = 0
    var bottom = 0
    var left = 0
    var right = 0

    // Scan from the top down to find the first non-transparent row.
    val height = image.getHeight()
    var y = 0
    while (y < height) {
        if (!isRowFullyTransparent(image, y)) {
            top = y
            break
        }
        y++
    }
    if (y == height) {
        // The image is fully transparent!
        return null
    }

    // Scan from the bottum up to find the first non-transparent row.
    y = height
    while (y > 0) {
        y--
        if (!isRowFullyTransparent(image, y)) {
            bottom = y + 1
            break
        }
    }

    // Scan from left to right to find the first non-transparent column.
    val width = image.getWidth()
    var x = 0
    while (x < width) {
        if (!isColumnFullyTransparent(image, x, top, bottom)) {
            left = x
            break
        }
        x++
    }
    x = width
    while (x > 0) {
        x--
        if (!isColumnFullyTransparent(image, x, top, bottom)) {
            right = x + 1
            break
        }
    }

    return Bounds(left, top, right, bottom)
}

private fun isRowFullyTransparent(image: BufferedImage, y: Int): Boolean {
    val width = image.getWidth()
    for (x in 0 until width) {
        if (!isFullyTransparent(image.getRGB(x, y))) {
            return false
        }
    }
    return true
}

private fun isColumnFullyTransparent(image: BufferedImage, x: Int, top: Int, bottom: Int): Boolean {
    for (y in top until bottom) {
        if (!isFullyTransparent(image.getRGB(x, y))) {
            return false
        }
    }
    return true
}

private const val CHANNEL_MASK_A = -0x1000000

private fun isFullyTransparent(argb: Int) = argb and CHANNEL_MASK_A == 0
