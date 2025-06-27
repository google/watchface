package com.google.wear.watchface.dfx.memory

import com.google.wear.watchface.dfx.memory.ImageProcessor.ImageData
import java.io.InputStream
import javax.imageio.ImageIO

/** JVM-specific implementation of ImageProcessor using AWT and ImageIO. */
class JvmImageProcessor : ImageProcessor {
    override fun createImageReader(stream: InputStream): ImageProcessor.ImageReader? {
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
                val bufferedImage = reader.read(imageIndex)

                return object : ImageData {
                    override val width: Int = bufferedImage.width

                    override val height: Int = bufferedImage.height

                    override fun getRgb(x: Int, y: Int): Int {
                        return bufferedImage.getRGB(x, y)
                    }
                }
            }
        }
    }
}
