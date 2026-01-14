package com.google.wear.watchface.dfx.memory

import java.io.IOException
import java.io.InputStream
import java.util.Optional

/** An interface for processing image data.  */
interface ImageProcessor {
    /** Holds the dimensions and pixel data of an image.  */
    interface ImageData {
        /** Width of the image in pixels.  */
        val width: Int

        /** Height of the image in pixels.  */
        val height: Int

        /** Returns the RGB color of the pixel at the specified coordinates.  */
        fun getRgb(x: Int, y: Int): Int
    }

    /** A reader for decoding image files, including animated formats.  */
    interface ImageReader {
        /** Returns the width of a specific image frame.  */
        fun getWidth(imageIndex: Int): Int

        /** Returns the height of a specific image frame.  */
        fun getHeight(imageIndex: Int): Int

        /** Total number of frames in the source. */
        val numImages: Int

        /** Reads and decodes a specific image frame into an [ImageData] object.  */
        fun read(imageIndex: Int): ImageData?
    }

    /**
     * Creates an [ImageReader] for the given image input stream.
     *
     * @param stream The input stream of the image data.
     */
    fun createImageReader(stream: InputStream, resName: String): ImageReader?

    fun createImageReader(stream: InputStream): ImageReader? = createImageReader(stream, "N/A")
}
