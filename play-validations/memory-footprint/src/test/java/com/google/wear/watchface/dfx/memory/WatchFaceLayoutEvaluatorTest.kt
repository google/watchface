package com.google.wear.watchface.dfx.memory;

import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import com.google.common.truth.Truth.assertThat
import com.samsung.watchface.WatchFaceXmlValidator
import junit.framework.TestCase
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Enclosed::class)
class WatchFaceLayoutEvaluatorTest {
    @RunWith(JUnit4::class)
    class SingleTests {
        @Test
        fun evaluateWatchFaceForLayout_failsOnMissingResource() {
            val document =
                parseTestDocument("/FailsOnMissingResource.xml")

            assertThrows(
                TestFailedException::class.java
            ) {
                WatchFaceLayoutEvaluator.evaluate(
                    document,
                    emptyMap(),
                    getTestEvaluationSettings()
                )
            }
        }

        @Test
        @Throws(Exception::class)
        fun evaluateWatchFaceForLayout_greedyEvaluationCountsTTFs() {
            // arrange
            val document = parseTestDocument("/TTFFont.xml")
            val fonts: MutableMap<String, DrawableResourceDetails> = HashMap()
            fonts["list1-font1.ttf"] = DrawableResourceDetails.builder()
                .setName("list1-font1.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(2)
                .build()
            fonts["list1-font2.ttf"] = DrawableResourceDetails.builder()
                .setName("list1-font2.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(4)
                .build()
            fonts["list1-font3.ttf"] = DrawableResourceDetails.builder()
                .setName("list1-font3.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(8)
                .build()
            fonts["bool1-font1.ttf"] = DrawableResourceDetails.builder()
                .setName("bool1-font1.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(16)
                .build()
            fonts["bool1-font2.ttf"] = DrawableResourceDetails.builder()
                .setName("bool1-font2.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(32)
                .build()
            fonts["font-in-ambient.ttf"] = DrawableResourceDetails.builder()
                .setName("font-in-ambient.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(64)
                .build()
            fonts["font-in-active.ttf"] = DrawableResourceDetails.builder()
                .setName("font-in-active.ttf")
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(128)
                .build()

            // act
            val memoryFootprint =
                WatchFaceLayoutEvaluator.evaluate(document, fonts, EvaluationSettings("", 0))

            // assert
            TestCase.assertEquals(
                MemoryFootprint( /* totalBytes= */
                    254,  /* maxActiveBytes= */
                    (8 + 32 + 128).toLong(),  /* maxAmbientBytes= */
                    (450 * 450 * 4).toLong()
                ),
                memoryFootprint
            )
        }

        @Test
        fun evaluateWatchFaceForLayout_switchesToGreedyEvaluationOnTooManyConfigs() {
            val document = parseTestDocument("/TooManyConfigs.xml")

            val testDrawableMap: MutableMap<String, DrawableResourceDetails> = java.util.HashMap()
            // two lists, each having 10 options, resulting in 100 total combinations
            for (list in 1..2) {
                for (image in 2..10) {
                    val imageName = String.format("list%s-image%s", list, image)
                    testDrawableMap[imageName] = DrawableResourceDetails.builder()
                        .setName(imageName)
                        .setBiggestFrameFootprintBytes(100)
                        .setNumberOfImages(1)
                        .build()
                }
            }
            testDrawableMap["list1-image1"] = DrawableResourceDetails.builder()
                .setName("list1-image1")
                .setBiggestFrameFootprintBytes(1024)
                .setNumberOfImages(1)
                .build()
            // act
            val memoryFootprint =
                WatchFaceLayoutEvaluator.evaluate(
                    document, testDrawableMap, EvaluationSettings("", 99)
                )

            // assert
            assertThat(
                MemoryFootprint( /* totalBytes= */
                    (18 * 100 + 1024).toLong(),  /* maxActiveBytes= */
                    2048,  /* maxAmbientBytes= */
                    (450 * 450 * 4).toLong()
                )
            ).isEqualTo(
                memoryFootprint
            )
        }

        @Test
        fun evaluateWatchFaceForLayout_handlesHoneyfaceWatchFaces() {
            val settings = EvaluationSettings("", "honeyface")
            val document = parseTestDocument("/Honeyface.xml")
            val resourceDetails: MutableMap<String, DrawableResourceDetails> = java.util.HashMap()
            putSimpleResource(
                resourceDetails,
                WatchFaceData.SYSTEM_DEFAULT_FONT,
                WatchFaceData.SYSTEM_DEFAULT_FONT_SIZE
            )
            putSimpleResource(resourceDetails, "image_1", 2)
            putSimpleResource(resourceDetails, "image_2", 4)
            putSimpleResource(resourceDetails, "image_3", 8)
            putSimpleResource(resourceDetails, "image_4", 16)
            putSimpleResource(resourceDetails, "image_5", 32)
            putSimpleResource(resourceDetails, "image_6", 64)
            putSimpleResource(resourceDetails, "image_minute_hand_1", 128)
            putSimpleResource(resourceDetails, "image_minute_hand_2", 256)
            putSimpleResource(resourceDetails, "image_second_hand", 512)
            putSimpleResource(resourceDetails, "image_ambient_1", 1024)
            putSimpleResource(
                resourceDetails,
                "image_hour_hand_ambient",
                2048
            )
            putSimpleResource(resourceDetails, "bitmap_font_0", 11)
            putSimpleResource(resourceDetails, "bitmap_font_1", 11)
            putSimpleResource(resourceDetails, "bitmap_font_2", 11)
            putSimpleResource(resourceDetails, "bitmap_font_3", 11)
            putSimpleResource(resourceDetails, "bitmap_font_4", 11)
            putSimpleResource(resourceDetails, "bitmap_font_5", 11)
            putSimpleResource(resourceDetails, "bitmap_font_6", 11)
            putSimpleResource(resourceDetails, "bitmap_font_7", 11)
            putSimpleResource(resourceDetails, "bitmap_font_8", 11)
            putSimpleResource(resourceDetails, "bitmap_font_9", 11)

            val memoryFootprint =
                WatchFaceLayoutEvaluator.evaluate(document, resourceDetails, settings)

            assertThat(
                MemoryFootprint( /* totalBytes= */
                    2048 * 2 - 2 + 10 * 11 + WatchFaceData.SYSTEM_DEFAULT_FONT_SIZE,  /* maxActiveBytes= */
                    (8
                            + 64
                            + 256
                            + 512
                            + 10 * 11 + WatchFaceData.SYSTEM_DEFAULT_FONT_SIZE),  // one layer + hour hand ambient + bitmap font
                    /* maxAmbientBytes= */
                    (450 * 450 * 4 + 2048 + 10 * 11).toLong()
                )
            ).isEqualTo(
                memoryFootprint
            )
        }
    }


    /** Applies the resource memory evaluator to various test layouts. */
    @RunWith(Parameterized::class)
    class LayoutParameterizedTests {
        data class TestParams(
            val layoutPath: String,
            val resourceMemoryFootprints: Map<String, DrawableResourceDetails>,
            val expectedActiveFootprint: Long,
            val expectedAmbientFootprint: Long,
            val expectedTotalFootprint: Long
        )

        @Parameterized.Parameter
        internal lateinit var testParams: TestParams

        @Before
        fun validateLayout() {
            val document = parseTestDocument(testParams.layoutPath)
            assertThat(WatchFaceXmlValidator().validate(document, "1"))
                .isTrue()
        }

        @Test
        fun evaluateWatchFaceForLayout_evaluatesToExpectedFootprint() {
            val document = parseTestDocument(testParams.layoutPath)

            val memoryFootprint =
                WatchFaceLayoutEvaluator.evaluate(
                    document,
                    testParams.resourceMemoryFootprints,
                    getTestEvaluationSettings()
                )

            val expectedFootprint =
                MemoryFootprint(
                    testParams.expectedTotalFootprint,
                    testParams.expectedActiveFootprint,
                    testParams.expectedAmbientFootprint
                )
            TestCase.assertEquals(expectedFootprint, memoryFootprint)
        }

        companion object {
            // define all the test instances. The name invokes the first argument, which calls on the
            // TestParameters::toString method to pretty print the test name.
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun data() = listOf(
                TestParams(
                    layoutPath = "/AlphaIsExpression.xml",
                    resourceMemoryFootprints = mapOf(
                        "always-rendered-image" to DrawableResourceDetails.Builder()
                            .setName("always-rendered-image")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1)
                            .build(),
                        "always-rendered-image-2" to DrawableResourceDetails.Builder()
                            .setName("always-rendered-image-2")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2)
                            .build()
                    ),
                    expectedActiveFootprint = 3,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 3
                ),
                TestParams(
                    layoutPath = "/ImageHiddenInActive.xml",
                    resourceMemoryFootprints = mapOf(
                        "ambient-only-image" to DrawableResourceDetails.Builder()
                            .setName("ambient-only-image")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1)
                            .build(),
                        "always-rendered-image" to DrawableResourceDetails.Builder()
                            .setName("always-rendered-image")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2)
                            .build()
                    ),
                    expectedActiveFootprint = 2,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 3
                ),
                TestParams(
                    layoutPath = "/ImageHiddenInAmbient.xml",
                    resourceMemoryFootprints = mapOf(
                        "active-only-image" to DrawableResourceDetails.Builder()
                            .setName("active-only-image")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1)
                            .build(),
                        "always-rendered-image" to DrawableResourceDetails.Builder()
                            .setName("always-rendered-image")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2)
                            .build()
                    ),
                    expectedActiveFootprint = 3,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 3
                ),
                TestParams(
                    layoutPath = "/LinearCombinations.xml",
                    resourceMemoryFootprints = mapOf(
                        "list1-image1" to DrawableResourceDetails.Builder()
                            .setName("list1-image1")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "list1-image2" to DrawableResourceDetails.Builder().setName("list1-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2).build(),
                        "list1-image3" to DrawableResourceDetails.Builder().setName("list1-image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build(),
                        "list2-image1" to DrawableResourceDetails.Builder().setName("list2-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(8).build(),
                        "list2-image2" to DrawableResourceDetails.Builder().setName("list2-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(16).build(),
                        "list2-image3" to DrawableResourceDetails.Builder().setName("list2-image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "boolean-image-true" to DrawableResourceDetails.Builder().setName("boolean-image-true")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(64).build(),
                        "boolean-image-false" to DrawableResourceDetails.Builder().setName("boolean-image-false")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(128).build()
                    ),
                    expectedActiveFootprint = 4 + 32 + 128,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 255
                ),
                TestParams(
                    layoutPath = "/LinearCombinationsWithVariant.xml",
                    resourceMemoryFootprints = mapOf(
                        "list1-image1" to DrawableResourceDetails.Builder().setName("list1-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "list1-image2" to DrawableResourceDetails.Builder().setName("list1-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2).build(),
                        "list1-image3" to DrawableResourceDetails.Builder().setName("list1-image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build(),
                        "list2-image1" to DrawableResourceDetails.Builder().setName("list2-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(8).build(),
                        "list2-image2" to DrawableResourceDetails.Builder().setName("list2-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(16).build(),
                        "list2-image3" to DrawableResourceDetails.Builder().setName("list2-image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "boolean-image-true" to DrawableResourceDetails.Builder().setName("boolean-image-true")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(64).build(),
                        "boolean-image-false" to DrawableResourceDetails.Builder().setName("boolean-image-false")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(128).build(),
                        "image-active" to DrawableResourceDetails.Builder().setName("image-active").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(256).build()
                    ),
                    expectedActiveFootprint = 4 + 32 + 128 + 256,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 511
                ),
                TestParams(
                    layoutPath = "/NestedVariantWithList.xml",
                    resourceMemoryFootprints = mapOf(
                        "active-list1-image1" to DrawableResourceDetails.Builder().setName("active-list1-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(1).build(),
                        "active-list1-image2" to DrawableResourceDetails.Builder().setName("active-list1-image2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "active-list1-image3" to DrawableResourceDetails.Builder().setName("active-list1-image3")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(4).build(),
                        "ambient-list2-image1" to DrawableResourceDetails.Builder().setName("ambient-list2-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(8).build(),
                        "ambient-list2-image2" to DrawableResourceDetails.Builder().setName("ambient-list2-image2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(16).build(),
                        "ambient-list2-image3" to DrawableResourceDetails.Builder().setName("ambient-list2-image3")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(32).build()
                    ),
                    expectedActiveFootprint = 4,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 63
                ),
                TestParams(
                    layoutPath = "/NestedLists.xml",
                    resourceMemoryFootprints = mapOf(
                        "list1-option1-list2-option1-list1-option1" to DrawableResourceDetails.Builder()
                            .setName("list1-option1-list2-option1-list1-option1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "list1-option1-list2-option1-list1-option2" to DrawableResourceDetails.Builder()
                            .setName("list1-option1-list2-option1-list1-option2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2).build(),
                        "list1-option1-list2-option2-list1-option1" to DrawableResourceDetails.Builder()
                            .setName("list1-option1-list2-option2-list1-option1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build(),
                        "list1-option1-list2-option2-list1-option2" to DrawableResourceDetails.Builder()
                            .setName("list1-option1-list2-option2-list1-option2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(8).build(),
                        "list1-option2-list2-option1-list1-option1" to DrawableResourceDetails.Builder()
                            .setName("list1-option2-list2-option1-list1-option1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(16).build(),
                        "list1-option2-list2-option1-list1-option2" to DrawableResourceDetails.Builder()
                            .setName("list1-option2-list2-option1-list1-option2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "list1-option2-list2-option2-list1-option1" to DrawableResourceDetails.Builder()
                            .setName("list1-option2-list2-option2-list1-option1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "list1-option2-list2-option2-list1-option2" to DrawableResourceDetails.Builder()
                            .setName("list1-option2-list2-option2-list1-option2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(128).build()
                    ),
                    expectedActiveFootprint = 128,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 255
                ),
                TestParams(
                    layoutPath = "/SameListWithDifferentImages.xml",
                    resourceMemoryFootprints = mapOf(
                        "option1-image1" to DrawableResourceDetails.Builder().setName("option1-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(128).build(),
                        "option2-image1" to DrawableResourceDetails.Builder().setName("option2-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "option2-image2" to DrawableResourceDetails.Builder().setName("option2-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "option2-image3" to DrawableResourceDetails.Builder().setName("option2-image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "option3-image1" to DrawableResourceDetails.Builder().setName("option3-image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(16).build(),
                        "option3-image2" to DrawableResourceDetails.Builder().setName("option3-image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(128).build()
                    ),
                    expectedActiveFootprint = 32 + 64 + 64,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 128 + 32 + 64 + 64 + 16 + 128
                ),
                TestParams(
                    layoutPath = "/MultipleListsReferenceSameImage.xml",
                    resourceMemoryFootprints = mapOf(
                        "list1-option1-list2-option2" to DrawableResourceDetails.Builder()
                            .setName("list1-option1-list2-option2").setNumberOfImages(1).setBiggestFrameFootprintBytes(128)
                            .build(),
                        "list1-option2" to DrawableResourceDetails.Builder().setName("list1-option2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "list1-option3" to DrawableResourceDetails.Builder().setName("list1-option3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "list2-option1" to DrawableResourceDetails.Builder().setName("list2-option1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(8).build(),
                        "list2-option2-image2" to DrawableResourceDetails.Builder().setName("list2-option2-image2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "list2-option3" to DrawableResourceDetails.Builder().setName("list2-option3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build()
                    ),
                    expectedActiveFootprint = 64 + 128 + 2,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 128 + 64 + 32 + 8 + 2 + 4
                ),
                TestParams(
                    layoutPath = "/AnimatedImages.xml",
                    resourceMemoryFootprints = mapOf(
                        "webp1" to DrawableResourceDetails.Builder().setName("webp1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "webp1-thumbnail" to DrawableResourceDetails.Builder().setName("webp1-thumbnail")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "sequence1-image1" to DrawableResourceDetails.Builder().setName("sequence1-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(4).build(),
                        "sequence1-image2" to DrawableResourceDetails.Builder().setName("sequence1-image2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(8).build(),
                        "webp2" to DrawableResourceDetails.Builder().setName("webp2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(16).build(),
                        "sequence2-image1" to DrawableResourceDetails.Builder().setName("sequence2-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(32).build(),
                        "sequence2-image2" to DrawableResourceDetails.Builder().setName("sequence2-image2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(64).build()
                    ),
                    expectedActiveFootprint = 127,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 127
                ),
                TestParams(
                    layoutPath = "/AnimatedImagesWithThumbnails.xml",
                    resourceMemoryFootprints = mapOf(
                        "webp1" to DrawableResourceDetails.Builder().setName("webp1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "webp1-thumbnail-node" to DrawableResourceDetails.Builder().setName("webp1-thumbnail-node")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "webp2" to DrawableResourceDetails.Builder().setName("webp2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build(),
                        "webp2-thumbnail" to DrawableResourceDetails.Builder().setName("webp2-thumbnail")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(8).build(),
                        "webp2-thumbnail-node" to DrawableResourceDetails.Builder().setName("webp2-thumbnail-node")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(16).build(),
                        "animated-images-webp1" to DrawableResourceDetails.Builder().setName("animated-images-webp1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(32).build(),
                        "animated-images-webp1-thumbnail" to DrawableResourceDetails.Builder()
                            .setName("animated-images-webp1-thumbnail").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "animated-images-webp2" to DrawableResourceDetails.Builder().setName("animated-images-webp2")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(128).build(),
                        "animated-images-webp2-thumbnail" to DrawableResourceDetails.Builder()
                            .setName("animated-images-webp2-thumbnail").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(256).build()
                    ),
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedActiveFootprint = 511,
                    expectedTotalFootprint = 511
                ),
                TestParams(
                    layoutPath = "/ExpressionImagesAreIgnored.xml",
                    resourceMemoryFootprints = emptyMap(),
                    expectedActiveFootprint = 0,
                    expectedAmbientFootprint = 0,
                    expectedTotalFootprint = 0
                ),
                TestParams(
                    layoutPath = "/AnimatedImagesWithDuplicateBiggestFrame.xml",
                    resourceMemoryFootprints = mapOf(
                        "webp1" to DrawableResourceDetails.Builder().setName("webp1").setNumberOfImages(2)
                            .setBiggestFrameFootprintBytes(256).build(),
                        "sequence1-image1" to DrawableResourceDetails.Builder().setName("sequence1-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(16).build(),
                        "sequences-shared-frame" to DrawableResourceDetails.Builder().setName("sequences-shared-frame")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(128).build(),
                        "sequence2-image1" to DrawableResourceDetails.Builder().setName("sequence2-image1")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(32).build()
                    ),
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedActiveFootprint = 512 + 16 + 128 + 32,
                    expectedTotalFootprint = 512 + 16 + 128 + 32
                ),
                TestParams(
                    layoutPath = "/PartsWithVariant.xml",
                    resourceMemoryFootprints = mapOf(
                        "animated-image" to DrawableResourceDetails.Builder().setName("animated-image").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "webp1-thumbnail" to DrawableResourceDetails.Builder().setName("webp1-thumbnail")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(16).build(),
                        "static-image" to DrawableResourceDetails.Builder().setName("animated-image").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build()
                    ),
                    expectedAmbientFootprint = 0,
                    expectedActiveFootprint = 64 + 32 + 16,
                    expectedTotalFootprint = 64 + 32 + 16
                ),
                TestParams(
                    layoutPath = "/PartialListOptions.xml",
                    resourceMemoryFootprints = mapOf(
                        "image1" to DrawableResourceDetails.Builder().setName("image1").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4).build(),
                        "image2" to DrawableResourceDetails.Builder().setName("image2").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(2).build(),
                        "image3" to DrawableResourceDetails.Builder().setName("image3").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(1).build(),
                        "image4" to DrawableResourceDetails.Builder().setName("image4").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(32).build(),
                        "image5" to DrawableResourceDetails.Builder().setName("image5").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(64).build(),
                        "image6" to DrawableResourceDetails.Builder().setName("image6").setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(128).build()
                    ),
                    expectedActiveFootprint = 128 + 4,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 1 + 2 + 4 + 32 + 64 + 128
                ),
                TestParams(
                    layoutPath = "/TTFFont.xml",
                    resourceMemoryFootprints = mapOf(
                        "list1-font1.ttf" to DrawableResourceDetails.Builder().setName("list1-font1.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "list1-font2.ttf" to DrawableResourceDetails.Builder().setName("list1-font2.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(4).build(),
                        "list1-font3.ttf" to DrawableResourceDetails.Builder().setName("list1-font3.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(8).build(),
                        "bool1-font1.ttf" to DrawableResourceDetails.Builder().setName("bool1-font1.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(16).build(),
                        "bool1-font2.ttf" to DrawableResourceDetails.Builder().setName("bool1-font2.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(32).build(),
                        "font-in-ambient.ttf" to DrawableResourceDetails.Builder().setName("font-in-ambient.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(64).build(),
                        "font-in-active.ttf" to DrawableResourceDetails.Builder().setName("font-in-active.ttf")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(128).build()
                    ),
                    expectedActiveFootprint = 8 + 32 + 128,
                    expectedAmbientFootprint = 450 * 450 * 4,
                    expectedTotalFootprint = 2 + 4 + 8 + 16 + 32 + 64 + 128
                ),
                TestParams(
                    layoutPath = "/ResourceImageInComplication.xml",
                    resourceMemoryFootprints = mapOf(
                        "image-in-complication" to DrawableResourceDetails.Builder()
                            .setName("image-in-complication")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(4)
                            .build()
                    ),
                    expectedActiveFootprint = 4,
                    expectedAmbientFootprint = 4,
                    expectedTotalFootprint = 4
                ),
                TestParams(
                    layoutPath = "/ActiveAnalogClock.xml",
                    resourceMemoryFootprints = mapOf(
                        "hour-hand" to DrawableResourceDetails.Builder().setName("hour-hand").setSha1("hour-hand")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(2).build(),
                        "minute-hand" to DrawableResourceDetails.Builder().setName("minute-hand").setSha1("minute-hand")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(4).build(),
                        "second-hand" to DrawableResourceDetails.Builder().setName("second-hand").setSha1("second-hand")
                            .setNumberOfImages(1).setBiggestFrameFootprintBytes(8).build()
                    ),
                    expectedActiveFootprint = 14,
                    expectedAmbientFootprint = 6,
                    expectedTotalFootprint = 14
                ),
                TestParams(
                    layoutPath = "/ActiveDigitalClockBitmapFont.xml",
                    resourceMemoryFootprints = (0..9).associate {
                        "char-$it" to DrawableResourceDetails.Builder()
                            .setName("char-$it")
                            .setNumberOfImages(1)
                            .setSha1("asd$it")
                            .setBiggestFrameFootprintBytes(2)
                            .build()
                    },
                    expectedActiveFootprint = 20,
                    expectedAmbientFootprint = 20,
                    expectedTotalFootprint = 20
                ),
                TestParams(
                    layoutPath = "/ActiveDigitalClockTtfFont.xml",
                    resourceMemoryFootprints = mapOf(
                        "custom_font" to DrawableResourceDetails.Builder()
                            .setName("custom_font")
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(24)
                            .build()
                    ),
                    expectedActiveFootprint = 24,
                    expectedAmbientFootprint = 24,
                    expectedTotalFootprint = 24
                )
            )
        }
    }

    @Ignore
    companion object {
        private fun getTestEvaluationSettings() =
            EvaluationSettings("", applyV1OffloadLimitations = false, estimateOptimization = false);

        private fun parseTestDocument(documentResourcePath: String): Document {
            return WatchFaceLayoutEvaluatorTest::class.java
                .getResourceAsStream(documentResourcePath).use {
                    DocumentBuilderFactory.newInstance().apply {
                        isNamespaceAware = true
                    }.newDocumentBuilder().parse(it)
                }
        }

        const val SAMPLE_WF_BASE_ARTIFACTS_PATH = "test-samples/sample-wf/build/outputs"

        private fun putSimpleResource(
            map: MutableMap<String, DrawableResourceDetails>, resourceName: String, size: Long
        ) {
            map[resourceName] = DrawableResourceDetails.builder()
                .setName(resourceName)
                .setBiggestFrameFootprintBytes(size)
                .setNumberOfImages(1)
                .setSha1(
                    Hashing.sha1()
                        .hashString(resourceName, Charsets.UTF_8)
                        .toString()
                )
                .build()
        }
    }
}