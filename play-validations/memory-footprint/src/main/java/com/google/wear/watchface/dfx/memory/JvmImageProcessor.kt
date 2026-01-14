package com.google.wear.watchface.dfx.memory

import com.google.wear.watchface.dfx.memory.ImageProcessor.ImageData
import java.awt.Color
import java.io.InputStream
import javax.imageio.ImageIO

/** JVM-specific implementation of ImageProcessor using AWT and ImageIO. */
class JvmImageProcessor : ImageProcessor {
    override fun createImageReader(stream: InputStream, resName: String): ImageProcessor.ImageReader? {
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

            override fun read(imageIndex: Int): ImageData  {
                try {
                    val bufferedImage = reader.read(imageIndex)

                    return object : ImageData {
                        override val width: Int = bufferedImage.width

                        override val height: Int = bufferedImage.height

                        override fun getRgb(x: Int, y: Int): Int {
                            return bufferedImage.getRGB(x, y)
                        }
                    }
                } catch (e: Exception) {
                    return object : ImageData {
                        override val width: Int = reader.getWidth(imageIndex)

                        override val height: Int = reader.getHeight(imageIndex)

                        override fun getRgb(x: Int, y: Int): Int {
                            return Color.BLACK.rgb
                        }
                    }
                }
            } 
        }
    }
}
