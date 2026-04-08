package com.google.wear.watchface.dfx.memory

import com.google.wear.watchface.dfx.memory.ImageProcessor.ImageData
import java.io.InputStream
import javax.imageio.ImageIO

/** JVM-specific implementation of ImageProcessor using AWT and ImageIO. */
class JvmImageProcessor(private val evaluationSettings: EvaluationSettings) : ImageProcessor {
    override fun createImageReader(
        stream: InputStream,
        imageName: String,
    ): ImageProcessor.ImageReader? {
        val imageInputStream = ImageIO.createImageInputStream(stream)
        val imageReaders = ImageIO.getImageReaders(imageInputStream)

        if (!imageReaders.hasNext()) {
            return null
        }

        val reader = imageReaders.next()
        reader.input = imageInputStream

        return object : ImageProcessor.ImageReader {
            override fun getWidth(imageIndex: Int) = reader.getWidth(imageIndex)

            override fun getHeight(imageIndex: Int) = reader.getHeight(imageIndex)

            override val numImages: Int = reader.getNumImages(true)

            override fun read(imageIndex: Int): ImageData {
                val bufferedImage = try {
                    reader.read(imageIndex)
                } catch (_: NullPointerException) {
                    // The ImageIO WebP plug in throws a NPE when parsing a webp that is malformed,
                    // but android can recover and render the image. In that case, we switch to an
                    // overestimated ImageData object, representing a full white image
                    if (evaluationSettings.verbose) {
                        println("Failed parsing image resource $imageName. This may lead to an overestimated memory footprint.")
                    }
                    null
                }

                return object : ImageData {
                    override val width: Int = bufferedImage?.width ?: reader.getWidth(imageIndex)

                    override val height: Int = bufferedImage?.height ?: reader.getHeight(imageIndex)

                    override fun getRgb(x: Int, y: Int): Int {
                        // -1 corresponds to a full white pixel
                        return bufferedImage?.getRGB(x, y) ?: -1
                    }
                }
            }
        }
    }
}
