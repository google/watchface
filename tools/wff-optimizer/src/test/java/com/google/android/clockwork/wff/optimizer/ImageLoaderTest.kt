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

import com.google.common.truth.Truth.assertThat
import java.awt.image.BufferedImage
import java.io.File
import java.io.StringWriter
import java.util.HashSet
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.w3c.dom.Document

@RunWith(JUnit4::class)
class ImageLoaderTest {
    @Test
    fun computeNonTransparentBounds() {
        val imageLoader =
            ImageLoader(Settings(File("src/test/resources/imageTests").getAbsolutePath()))

        val image = imageLoader.loadImage("alpha")

        assertEquals(image.nonTransparentBounds!!.left, 244)
        assertEquals(image.nonTransparentBounds!!.right, 380)
        assertEquals(image.nonTransparentBounds!!.top, 330)
        assertEquals(image.nonTransparentBounds!!.bottom, 415)
    }

    @Test
    fun bitmapFontImageDeduplication() {
        val fixture =
            load(
                "src/test/resources/duplicateImageTest",
                "src/test/resources/duplicateImageTest/res/raw/watchface.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)
        assertTrue(fixture.imageLoader.dedupeAndWriteOptimizedImages())

        // This should deduplicate file_a1, file_a2,file_a3 which are all the same, likewise file_b1
        // and file_b2 are the same.
        assertEquals(
            getResource("duplicateImageTest/res/raw/watchface_expected.xml"),
            fixture.document.transformToString()
        )
    }

    @Test
    fun partImageDeduplication() {
        val fixture =
            load(
                "src/test/resources/duplicateImageTest",
                "src/test/resources/duplicateImageTest/res/raw/watchface2.xml"
            )

        fixture.optimizer.partImages.optimize(fixture.imageLoader)
        assertTrue(fixture.imageLoader.dedupeAndWriteOptimizedImages())

        // This should deduplicate file_a1, file_a2,file_a3 which are all the same, likewise file_b1
        // and file_b2 are the same.
        assertEquals(
            getResource("duplicateImageTest/res/raw/watchface2_expected.xml"),
            fixture.document.transformToString()
        )
    }

    @Test
    fun bitmapFontCropAndMargins() {
        val fixture =
            load(
                "src/test/resources/bitmapFontCropTest",
                "src/test/resources/bitmapFontCropTest/res/raw/watchface.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)
        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // The bitmap fonts should be cropped with margins added to correctly place the image.
        assertEquals(
            getResource("bitmapFontCropTest/res/raw/watchface_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly("a 40 x 44 cropped", "b 28 x 44 cropped")
    }

    @Test
    fun bitmapFontCropping2() {
        val fixture =
            load(
                "src/test/resources/bitmapFontCropTest2",
                "src/test/resources/bitmapFontCropTest2/res/raw/watchface.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)
        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // The bitmap fonts should be cropped with margins added to correctly place the image.
        assertEquals(
            getResource("bitmapFontCropTest2/res/raw/watchface_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly(
                "wfs_4_478cdc4e_a2d3_4a0b_a363_0141236a4fb2 60 x 109 cropped",
                "wfs_6_82227af2_ab99_4635_a282_f18e61f6a4e5 60 x 109 cropped",
                "wfs_1_7fd1e3ba_f9e5_4114_95d0_404fd95b0f1c 60 x 109 cropped",
                "wfs_9_142c8d9c_ac84_4358_8512_a0a23bafa6bc 60 x 109 cropped",
                "wfs__bcea73dd_7a37_467f_94c8_f57f413c1618 30 x 80 cropped",
                "wfs_0_84beb937_1dd8_4ca7_afc6_9d52ee68188b 60 x 109 cropped",
                "wfs_8_f5b0994f_24cc_43d3_823c_b4d12dbea6f8 60 x 109 cropped",
                "wfs_2_1cd08553_4def_4929_ae79_e4e95dc8e7d2 60 x 109 cropped",
                "wfs_3_f50b11ef_1120_43f1_8c23_ae8f4d968b77 60 x 109 cropped",
                "wfs_5_6060f0fa_b314_403c_8ef3_8b2127cdf8a5 60 x 109 cropped",
                "wfs_7_3bc978df_2037_4d89_b3bd_a52b01f702f0 60 x 109 cropped"
            )
    }

    @Test
    fun bitmapFontCropAndMargins_minimumWidth() {
        val fixture =
            load(
                "src/test/resources/narrowBitmapFont",
                "src/test/resources/narrowBitmapFont/res/raw/watchface.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)
        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // Check that we don't create a zero width image when downsizeing.
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly(
                "wfs_sec_decorative_3e6538b4_6b9c_4b20_9c09_0e7ae4babb1c 1 x 13 cropped scaled"
            )
    }

    @Test
    fun tooLargeBitmapFontCropAndMargins() {
        val fixture =
            load(
                "src/test/resources/bitmapFontCropTest",
                "src/test/resources/bitmapFontCropTest/res/raw/too_large.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)
        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // The source images are 100x100, however they're rendered at 200x200 and we don't want to
        // increace runtime memory usage so we should crop but not scale.
        assertEquals(
            getResource("bitmapFontCropTest/res/raw/too_large_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly("a 40 x 44 cropped", "b 28 x 44 cropped")
    }

    @Test
    fun tooSmallBitmapFontCropAndMargins() {
        val fixture =
            load(
                "src/test/resources/bitmapFontCropTest",
                "src/test/resources/bitmapFontCropTest/res/raw/too_small.xml"
            )

        fixture.optimizer.bitmapFonts.optimize(fixture.imageLoader)

        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // The source images are 100x100 but they're only ever rendered at 50x50 so we should scale
        // them down as well as crop to the visible pixels.
        assertEquals(
            getResource("bitmapFontCropTest/res/raw/too_small_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly("a 20 x 22 cropped scaled", "b 14 x 22 cropped scaled")
    }

    @Test
    fun partImageCrop() {
        val fixture =
            load(
                "src/test/resources/partImageCropTest",
                "src/test/resources/partImageCropTest/res/raw/watchface.xml"
            )

        fixture.optimizer.partImages.optimize(fixture.imageLoader)

        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // We should update the PartImages to apply the crop.
        assertEquals(
            getResource("partImageCropTest/res/raw/watchface_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary())
            .containsExactly("a 40 x 44", "b 28 x 44")
    }

    @Test
    fun partImageCropAfectsPivot() {
        val fixture =
            load(
                "src/test/resources/watchHandTest",
                "src/test/resources/watchHandTest/res/raw/watchface.xml"
            )

        fixture.optimizer.partImages.optimize(fixture.imageLoader)

        fixture.imageLoader.dedupeAndWriteOptimizedImages()

        // We should update the PartImages to apply the crop.
        assertEquals(
            getResource("watchHandTest/res/raw/watchface_expected.xml"),
            fixture.document.transformToString()
        )
        assertThat(fixture.imageLoader.optimizedImagesSummary()).containsExactly("hand 33 x 183")
    }

    private class TestFixture(
        val imageLoader: TestImageLoader,
        val document: Document,
        val optimizer: Optimizer
    )

    private fun load(settings: String, watchFace: String): TestFixture {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val settings = Settings(File(settings).getAbsolutePath())
        val document = builder.parse(File(watchFace))
        val imageLoader = TestImageLoader(settings)
        val optimizer = Optimizer(document, settings)
        optimizer.walkTree()
        return TestFixture(imageLoader, document, optimizer)
    }

    private fun getResource(name: String) =
        this::class.java.classLoader.getResource(name).readText().trimEnd()
}

class TestImageLoader(settings: Settings) : ImageLoader(settings) {
    val writtenOptimizedImages = HashSet<TestImage>()

    override fun loadImageInternal(name: String, file: File, image: BufferedImage) =
        TestImage(name, file, image, computeNonTransparentBounds(image), writtenOptimizedImages)

    fun optimizedImagesSummary() = writtenOptimizedImages.map { it.summary() }
}

class TestImage(
    name: String,
    file: File,
    bufferedImage: BufferedImage,
    nonTransparentBounds: Bounds?,
    val writtenOptimizedImages: HashSet<TestImage>,
) : Image(name, file, bufferedImage, nonTransparentBounds) {
    override fun maybeWriteOptimizedImage() {
        optimizedImage?.let { writtenOptimizedImages.add(this) }
    }

    fun summary(): String {
        val image = optimizedImage ?: bufferedImage
        var result = "$name ${image.width} x ${image.height}"
        if (cropped) {
            result = result + " cropped"
        }
        if (scaled) {
            result = result + " scaled"
        }
        return result
    }
}

fun Document.transformToString(): String {
    val transformer = TransformerFactory.newInstance().newTransformer()
    val source = DOMSource(this)
    val writer = StringWriter()
    val result = StreamResult(writer)
    transformer.transform(source, result)
    return writer.getBuffer().toString()
}
