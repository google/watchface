package com.google.wear.watchface.dfx.memory;

import static com.google.wear.watchface.dfx.memory.EvaluationSettings.parseFromArguments;
import static com.google.wear.watchface.dfx.memory.ResourceMemoryEvaluator.evaluateMemoryFootprint;
import static com.google.wear.watchface.dfx.memory.ResourceMemoryEvaluator.evaluateWatchFaceForLayout;
import static com.google.wear.watchface.dfx.memory.WatchFaceData.SYSTEM_DEFAULT_FONT;
import static com.google.wear.watchface.dfx.memory.WatchFaceData.SYSTEM_DEFAULT_FONT_SIZE;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.Parameterized.Parameter;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.samsung.watchface.WatchFaceXmlValidator;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;

@RunWith(Enclosed.class)
public class ResourceMemoryEvaluatorTest {
    private static final String SAMPLE_WF_BASE_ARTIFACTS_PATH =
            "test-samples/sample-wf/build/outputs";

    /**
     * Applies the resource memory evaluator to the sample DWFs from this project to ensure expected
     * memory footprints do not change unintentionally.
     */
    @RunWith(Parameterized.class)
    public static class GoldenTest {

        private static class TestParams {
            final String watchFace;

            final long expectedActiveFootprintBytes;

            final long expectedAmbientFootprintBytes;

            final int expectedLayouts;

            private TestParams(
                    String watchFace,
                    long expectedActiveFootprintBytes,
                    long expectedAmbientFootprintBytes,
                    int expectedLayouts) {
                this.watchFace = watchFace;
                this.expectedActiveFootprintBytes = expectedActiveFootprintBytes;
                this.expectedAmbientFootprintBytes = expectedAmbientFootprintBytes;
                this.expectedLayouts = expectedLayouts;
            }

            @Override
            public String toString() {
                return watchFace;
            }
        }

        @Parameter public TestParams testParams;

        // define all the test instances. The name invokes the first argument, which calls on the
        // TestParameters::toString method to pretty print the test name.
        @Parameters(name = "{0}")
        public static Collection<Object> data() {

            return Stream.of(
                            "unpackedBundle/release",
                            "apk/release/sample-wf-release.apk",
                            "apk/debug/sample-wf-debug.apk",
                            "bundle/release/sample-wf-release.aab",
                            "zipApk/com.google.wear.watchface.memory.sample.zip")
                    .map(
                            artifactRelativePath ->
                                    new TestParams(
                                            /* watchFace= */ Paths.get(
                                                            SAMPLE_WF_BASE_ARTIFACTS_PATH,
                                                            artifactRelativePath)
                                                    .toString(),
                                            /* expectedActiveFootprintBytes= */ 4565100
                                                    + SYSTEM_DEFAULT_FONT_SIZE,
                                            /* expectedAmbientFootprintBytes= */ 2540100,
                                            /* expectedLayouts= */ 1))
                    .collect(Collectors.toList());
        }

        @Test
        public void validate_hasExpectedFootprint() {
            List<MemoryFootprint> multiShapesFootprint =
                    evaluateMemoryFootprint(new EvaluationSettings(testParams.watchFace, "1"));

            assertEquals(testParams.expectedLayouts, multiShapesFootprint.size());
            assertEquals(
                    testParams.expectedActiveFootprintBytes,
                    multiShapesFootprint.get(0).getMaxActiveBytes());
            assertEquals(
                    testParams.expectedAmbientFootprintBytes,
                    multiShapesFootprint.get(0).getMaxAmbientBytes());
        }
    }

    /** Applies the resource memory evaluator to various test layouts. */
    @RunWith(Parameterized.class)
    public static class EvaluateForLayoutTest {

        @Parameter public TestParams testParams;

        static class TestParams {
            private final String layoutPath;

            private final Map<String, DrawableResourceDetails> memoryFootprintForImages;

            private final long expectedActiveFootprint;

            private final long expectedAmbientFootprint;

            private final long expectedTotalFootprint;

            TestParams(
                    String layoutPath,
                    Map<String, DrawableResourceDetails> memoryFootprintForImages,
                    long expectedActiveFootprint,
                    long expectedAmbientFootprint,
                    long expectedTotalFootprint) {
                this.layoutPath = layoutPath;
                this.memoryFootprintForImages = memoryFootprintForImages;
                this.expectedActiveFootprint = expectedActiveFootprint;
                this.expectedAmbientFootprint = expectedAmbientFootprint;
                this.expectedTotalFootprint = expectedTotalFootprint;
            }

            @Override
            public String toString() {
                return layoutPath;
            }

            static class Builder {
                private String layoutPath;
                private long expectedActiveFootprint;
                private long expectedAmbientFootprint;
                private long expectedTotalFootprint;

                private final Map<String, DrawableResourceDetails> memoryFootprintForImages =
                        new HashMap<>();

                public Builder setLayoutPath(String layoutPath) {
                    this.layoutPath = layoutPath;
                    return this;
                }

                public Builder addImageFootprints(
                        Consumer<Map<String, DrawableResourceDetails>> imageAdder) {
                    imageAdder.accept(memoryFootprintForImages);
                    return this;
                }

                public Builder setExpectedActiveFootprint(long activeFootprint) {
                    this.expectedActiveFootprint = activeFootprint;
                    return this;
                }

                public Builder setExpectedAmbientFootprint(long ambientFootprint) {
                    this.expectedAmbientFootprint = ambientFootprint;
                    return this;
                }

                public Builder setExpectedTotalFootprint(long totalFootprint) {
                    this.expectedTotalFootprint = totalFootprint;
                    return this;
                }

                public TestParams build() {
                    return new TestParams(
                            layoutPath,
                            memoryFootprintForImages,
                            expectedActiveFootprint,
                            expectedAmbientFootprint,
                            expectedTotalFootprint);
                }
            }
        }

        // define all the test instances. The name invokes the first argument, which calls on the
        // TestParameters::toString method to pretty print the test name.
        @Parameters(name = "{0}")
        public static Collection<Object> data() {
            return Arrays.asList(
                    new TestParams.Builder()
                            .setLayoutPath("/AlphaIsExpression.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "always-rendered-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("always-rendered-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "always-rendered-image-2",
                                                DrawableResourceDetails.builder()
                                                        .setName("always-rendered-image-2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(3)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(3)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ImageHiddenInActive.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "ambient-only-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("ambient-only-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "always-rendered-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("always-rendered-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(2)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(3)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ImageHiddenInAmbient.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "active-only-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("active-only-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "always-rendered-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("always-rendered-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(3)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(3)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/LinearCombinations.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "list1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "list1-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "list1-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "list2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "list2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "list2-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "boolean-image-true",
                                                DrawableResourceDetails.builder()
                                                        .setName("boolean-image-true")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "boolean-image-false",
                                                DrawableResourceDetails.builder()
                                                        .setName("boolean-image-false")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(4 + 32 + 128)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(255)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/LinearCombinationsWithVariant.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "list1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "list1-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "list1-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "list2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "list2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "list2-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "boolean-image-true",
                                                DrawableResourceDetails.builder()
                                                        .setName("boolean-image-true")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "boolean-image-false",
                                                DrawableResourceDetails.builder()
                                                        .setName("boolean-image-false")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                        images.put(
                                                "image-active",
                                                DrawableResourceDetails.builder()
                                                        .setName("image-active")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(256)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(4 + 32 + 128 + 256)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(511)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/NestedVariantWithList.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "active-list1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("active-list1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "active-list1-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("active-list1-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "active-list1-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("active-list1-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "ambient-list2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("ambient-list2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "ambient-list2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("ambient-list2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "ambient-list2-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("ambient-list2-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(4)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(63)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/NestedLists.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "list1-option1-list2-option1-list1-option1",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option1-list2-option1-list1-option1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "list1-option1-list2-option1-list1-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option1-list2-option1-list1-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "list1-option1-list2-option2-list1-option1",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option1-list2-option2-list1-option1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "list1-option1-list2-option2-list1-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option1-list2-option2-list1-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "list1-option2-list2-option1-list1-option1",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option2-list2-option1-list1-option1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "list1-option2-list2-option1-list1-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option2-list2-option1-list1-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "list1-option2-list2-option2-list1-option1",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option2-list2-option2-list1-option1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "list1-option2-list2-option2-list1-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName(
                                                                "list1-option2-list2-option2-list1-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(128)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(255)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/SameListWithDifferentImages.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "option1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("option1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                        images.put(
                                                "option2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("option2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "option2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("option2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "option2-image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("option2-image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "option3-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("option3-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "option3-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("option3-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(32 + 64 + 64)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(128 + 32 + 64 + 64 + 16 + 128)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/MultipleListsReferenceSameImage.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "list1-option1-list2-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-option1-list2-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                        images.put(
                                                "list1-option2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-option2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "list1-option3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-option3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "list2-option1",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-option1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "list2-option2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-option2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "list2-option3",
                                                DrawableResourceDetails.builder()
                                                        .setName("list2-option3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(64 + 128 + 2)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(128 + 64 + 32 + 8 + 2 + 4)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/AnimatedImages.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "webp1",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "webp1-thumbnail",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1-thumbnail")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "sequence1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "sequence1-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence1-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "webp2",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "sequence2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "sequence2-image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence2-image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(127)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(127)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/AnimatedImagesWithThumbnails.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "webp1",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "webp1-thumbnail-node",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1-thumbnail-node")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "webp2",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "webp2-thumbnail",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp2-thumbnail")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "webp2-thumbnail-node",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp2-thumbnail-node")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "animated-images-webp1",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-images-webp1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "animated-images-webp1-thumbnail",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-images-webp1-thumbnail")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "animated-images-webp2",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-images-webp2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                        images.put(
                                                "animated-images-webp2-thumbnail",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-images-webp2-thumbnail")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(256)
                                                        .build());
                                    })
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedActiveFootprint(511)
                            .setExpectedTotalFootprint(511)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ExpressionImagesAreIgnored.xml")
                            .setExpectedActiveFootprint(0)
                            .setExpectedAmbientFootprint(0)
                            .setExpectedTotalFootprint(0)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/AnimatedImagesWithDuplicateBiggestFrame.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "webp1",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1")
                                                        .setNumberOfImages(2)
                                                        .setBiggestFrameFootprintBytes(256)
                                                        .build());
                                        images.put(
                                                "sequence1-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence1-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "sequences-shared-frame",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequences-shared-frame")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                        images.put(
                                                "sequence2-image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("sequence2-image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                    })
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedActiveFootprint(512 + 16 + 128 + 32)
                            .setExpectedTotalFootprint(512 + 16 + 128 + 32)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/PartsWithVariant.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "animated-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "webp1-thumbnail",
                                                DrawableResourceDetails.builder()
                                                        .setName("webp1-thumbnail")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "static-image",
                                                DrawableResourceDetails.builder()
                                                        .setName("animated-image")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                    })
                            .setExpectedAmbientFootprint(0)
                            .setExpectedActiveFootprint(64 + 32 + 16)
                            .setExpectedTotalFootprint(64 + 32 + 16)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/PartialListOptions.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "image1",
                                                DrawableResourceDetails.builder()
                                                        .setName("image1")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "image2",
                                                DrawableResourceDetails.builder()
                                                        .setName("image2")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "image3",
                                                DrawableResourceDetails.builder()
                                                        .setName("image3")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(1)
                                                        .build());
                                        images.put(
                                                "image4",
                                                DrawableResourceDetails.builder()
                                                        .setName("image4")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "image5",
                                                DrawableResourceDetails.builder()
                                                        .setName("image5")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "image6",
                                                DrawableResourceDetails.builder()
                                                        .setName("image6")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(128 + 4)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(1 + 2 + 4 + 32 + 64 + 128)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/TTFFont.xml")
                            .addImageFootprints(
                                    (images) -> {
                                        images.put(
                                                "list1-font1.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-font1.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "list1-font2.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-font2.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "list1-font3.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("list1-font3.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                        images.put(
                                                "bool1-font1.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("bool1-font1.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(16)
                                                        .build());
                                        images.put(
                                                "bool1-font2.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("bool1-font2.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(32)
                                                        .build());
                                        images.put(
                                                "font-in-ambient.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("font-in-ambient.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(64)
                                                        .build());
                                        images.put(
                                                "font-in-active.ttf",
                                                DrawableResourceDetails.builder()
                                                        .setName("font-in-active.ttf")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(128)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(8 + 32 + 128)
                            .setExpectedAmbientFootprint(450 * 450 * 4)
                            .setExpectedTotalFootprint(2 + 4 + 8 + 16 + 32 + 64 + 128)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ResourceImageInComplication.xml")
                            .addImageFootprints(
                                    images -> {
                                        images.put(
                                                "image-in-complication",
                                                DrawableResourceDetails.builder()
                                                        .setName("image-in-complication")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(4)
                            .setExpectedAmbientFootprint(4)
                            .setExpectedTotalFootprint(4)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ActiveAnalogClock.xml")
                            .addImageFootprints(
                                    images -> {
                                        images.put(
                                                "hour-hand",
                                                DrawableResourceDetails.builder()
                                                        .setName("hour-hand")
                                                        .setSha1("hour-hand")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(2)
                                                        .build());
                                        images.put(
                                                "minute-hand",
                                                DrawableResourceDetails.builder()
                                                        .setName("minute-hand")
                                                        .setSha1("minute-hand")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(4)
                                                        .build());
                                        images.put(
                                                "second-hand",
                                                DrawableResourceDetails.builder()
                                                        .setName("second-hand")
                                                        .setSha1("second-hand")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(8)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(14)
                            .setExpectedAmbientFootprint(6)
                            .setExpectedTotalFootprint(14)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ActiveDigitalClockBitmapFont.xml")
                            .addImageFootprints(
                                    images -> {
                                        for (int digit = 0; digit <= 9; digit++) {
                                            images.put(
                                                    "char-" + digit,
                                                    DrawableResourceDetails.builder()
                                                            .setName("char-" + digit)
                                                            .setNumberOfImages(1)
                                                            .setSha1("asd" + digit)
                                                            .setBiggestFrameFootprintBytes(2)
                                                            .build());
                                        }
                                    })
                            .setExpectedActiveFootprint(20)
                            .setExpectedAmbientFootprint(20)
                            .setExpectedTotalFootprint(20)
                            .build(),
                    new TestParams.Builder()
                            .setLayoutPath("/ActiveDigitalClockTtfFont.xml")
                            .addImageFootprints(
                                    images -> {
                                        images.put(
                                                "custom_font",
                                                DrawableResourceDetails.builder()
                                                        .setName("custom_font")
                                                        .setNumberOfImages(1)
                                                        .setBiggestFrameFootprintBytes(24)
                                                        .build());
                                    })
                            .setExpectedActiveFootprint(24)
                            .setExpectedAmbientFootprint(24)
                            .setExpectedTotalFootprint(24)
                            .build());
        }

        @Test
        public void evaluateWatchFaceForLayout_evaluatesToExpectedFootprint() throws Exception {
            try (InputStream is = getClass().getResourceAsStream(testParams.layoutPath)) {
                Document document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                MemoryFootprint memoryFootprint =
                        evaluateWatchFaceForLayout(
                                testParams.memoryFootprintForImages,
                                document,
                                getTestEvaluationSettings());

                MemoryFootprint expectedFootprint =
                        new MemoryFootprint(
                                testParams.expectedTotalFootprint,
                                testParams.expectedActiveFootprint,
                                testParams.expectedAmbientFootprint);

                assertEquals(expectedFootprint, memoryFootprint);
            }
        }

        @Before
        public void xmlIsValid() throws Exception {
            WatchFaceXmlValidator xmlValidator = new WatchFaceXmlValidator();
            try (InputStream is = getClass().getResourceAsStream(testParams.layoutPath)) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setNamespaceAware(true);
                Document document = docFactory.newDocumentBuilder().parse(is);
                assertTrue(xmlValidator.validate(document, "1"));
            }
        }
    }

    /** Tests for the ResourceMemoryEvaluator that are not parameterized */
    @RunWith(JUnit4.class)
    public static class SingleTests {
        @Test
        public void optimizationEstimateGoldenTest() {
            String watchFacePath =
                    Paths.get(SAMPLE_WF_BASE_ARTIFACTS_PATH, "bundle/release/sample-wf-release.aab")
                            .toString();

            List<MemoryFootprint> multiShapesFootprint =
                    evaluateMemoryFootprint(
                            parseFromArguments(
                                            "--watch-face",
                                            watchFacePath,
                                            "--schema-version",
                                            "1",
                                            "--estimate-optimization")
                                    .get());

            assertEquals(4575388, multiShapesFootprint.get(0).getMaxActiveBytes());
            assertEquals(2179278, multiShapesFootprint.get(0).getMaxAmbientBytes());
        }

        @Test
        public void evaluateWatchFaceForLayout_failsOnMissingResource() throws Exception {
            try (InputStream is = getClass().getResourceAsStream("/FailsOnMissingResource.xml")) {
                Document document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);

                assertThrows(
                        TestFailedException.class,
                        () ->
                                evaluateWatchFaceForLayout(
                                        Collections.emptyMap(),
                                        document,
                                        getTestEvaluationSettings()));
            }
        }

        @Test
        public void evaluateWatchFaceForLayout_greedyEvaluationCountsTTFs() throws Exception {
            // arrange
            try (InputStream is = getClass().getResourceAsStream("/TTFFont.xml")) {
                Document document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                Map<String, DrawableResourceDetails> fonts = new HashMap<>();
                fonts.put(
                        "list1-font1.ttf",
                        DrawableResourceDetails.builder()
                                .setName("list1-font1.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(2)
                                .build());
                fonts.put(
                        "list1-font2.ttf",
                        DrawableResourceDetails.builder()
                                .setName("list1-font2.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(4)
                                .build());
                fonts.put(
                        "list1-font3.ttf",
                        DrawableResourceDetails.builder()
                                .setName("list1-font3.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(8)
                                .build());
                fonts.put(
                        "bool1-font1.ttf",
                        DrawableResourceDetails.builder()
                                .setName("bool1-font1.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(16)
                                .build());
                fonts.put(
                        "bool1-font2.ttf",
                        DrawableResourceDetails.builder()
                                .setName("bool1-font2.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(32)
                                .build());
                fonts.put(
                        "font-in-ambient.ttf",
                        DrawableResourceDetails.builder()
                                .setName("font-in-ambient.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(64)
                                .build());
                fonts.put(
                        "font-in-active.ttf",
                        DrawableResourceDetails.builder()
                                .setName("font-in-active.ttf")
                                .setNumberOfImages(1)
                                .setBiggestFrameFootprintBytes(128)
                                .build());

                // act
                MemoryFootprint memoryFootprint =
                        evaluateWatchFaceForLayout(
                                fonts, document, new EvaluationSettings("", "", 0));

                // assert
                assertEquals(
                        new MemoryFootprint(
                                /* totalBytes= */ 254,
                                /* maxActiveBytes= */ 8 + 32 + 128,
                                /* maxAmbientBytes= */ 450 * 450 * 4),
                        memoryFootprint);
            }
        }

        @Test
        public void evaluateWatchFaceForLayout_switchesToGreedyEvaluationOnTooManyConfigs()
                throws Exception {
            // arrange
            try (InputStream is = getClass().getResourceAsStream("/TooManyConfigs.xml")) {
                Document document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                Map<String, DrawableResourceDetails> testDrawableMap = new HashMap<>();
                // two lists, each having 10 options, resulting in 100 total combinations
                for (int list = 1; list <= 2; list++) {
                    for (int image = 2; image <= 10; image++) {
                        String imageName = String.format("list%s-image%s", list, image);
                        testDrawableMap.put(
                                imageName,
                                DrawableResourceDetails.builder()
                                        .setName(imageName)
                                        .setBiggestFrameFootprintBytes(100)
                                        .setNumberOfImages(1)
                                        .build());
                    }
                }
                testDrawableMap.put(
                        "list1-image1",
                        DrawableResourceDetails.builder()
                                .setName("list1-image1")
                                .setBiggestFrameFootprintBytes(1024)
                                .setNumberOfImages(1)
                                .build());
                // act
                MemoryFootprint memoryFootprint =
                        evaluateWatchFaceForLayout(
                                testDrawableMap, document, new EvaluationSettings("", "", 99));

                // assert
                assertEquals(
                        new MemoryFootprint(
                                /* totalBytes= */ 18 * 100 + 1024,
                                /* maxActiveBytes= */ 2048,
                                /* maxAmbientBytes= */ 450 * 450 * 4),
                        memoryFootprint);
            }
        }

        @Test
        public void evaluateWatchFaceForLayout_handlesHoneyfaceWatchFaces() throws Exception {
            EvaluationSettings settings = new EvaluationSettings("", "honeyface");
            try (InputStream is = getClass().getResourceAsStream("/Honeyface.xml")) {
                Document document =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                Map<String, DrawableResourceDetails> resourceDetails = new HashMap<>();
                putSimpleResource(resourceDetails, SYSTEM_DEFAULT_FONT, SYSTEM_DEFAULT_FONT_SIZE);
                putSimpleResource(resourceDetails, "image_1", 2);
                putSimpleResource(resourceDetails, "image_2", 4);
                putSimpleResource(resourceDetails, "image_3", 8);
                putSimpleResource(resourceDetails, "image_4", 16);
                putSimpleResource(resourceDetails, "image_5", 32);
                putSimpleResource(resourceDetails, "image_6", 64);
                putSimpleResource(resourceDetails, "image_minute_hand_1", 128);
                putSimpleResource(resourceDetails, "image_minute_hand_2", 256);
                putSimpleResource(resourceDetails, "image_second_hand", 512);
                putSimpleResource(resourceDetails, "image_ambient_1", 1024);
                putSimpleResource(resourceDetails, "image_hour_hand_ambient", 2048);
                putSimpleResource(resourceDetails, "bitmap_font_0", 11);
                putSimpleResource(resourceDetails, "bitmap_font_1", 11);
                putSimpleResource(resourceDetails, "bitmap_font_2", 11);
                putSimpleResource(resourceDetails, "bitmap_font_3", 11);
                putSimpleResource(resourceDetails, "bitmap_font_4", 11);
                putSimpleResource(resourceDetails, "bitmap_font_5", 11);
                putSimpleResource(resourceDetails, "bitmap_font_6", 11);
                putSimpleResource(resourceDetails, "bitmap_font_7", 11);
                putSimpleResource(resourceDetails, "bitmap_font_8", 11);
                putSimpleResource(resourceDetails, "bitmap_font_9", 11);

                MemoryFootprint memoryFootprint =
                        evaluateWatchFaceForLayout(resourceDetails, document, settings);

                assertEquals(
                        new MemoryFootprint(
                                /* totalBytes= */ 2048 * 2 - 2 + 10 * 11 + SYSTEM_DEFAULT_FONT_SIZE,
                                /* maxActiveBytes= */ 8
                                        + 64
                                        + 256
                                        + 512
                                        + 10 * 11
                                        + SYSTEM_DEFAULT_FONT_SIZE,
                                // one layer + hour hand ambient + bitmap font
                                /* maxAmbientBytes= */ 450 * 450 * 4 + 2048 + 10 * 11),
                        memoryFootprint);
            }
        }

        private static void putSimpleResource(
                Map<String, DrawableResourceDetails> map, String resourceName, long size) {
            map.put(
                    resourceName,
                    DrawableResourceDetails.builder()
                            .setName(resourceName)
                            .setBiggestFrameFootprintBytes(size)
                            .setNumberOfImages(1)
                            .setSha1(
                                    Hashing.sha1()
                                            .hashString(resourceName, Charsets.UTF_8)
                                            .toString())
                            .build());
        }
    }

    /**
     * Utility function that resolves the given watch face module name to its path in the built
     * artifacts. These artifacts are built in gradle by making the "test" task depend on the watch
     * face module's "assembleRelease" task.
     */
    private static String resolveWatchFaceModuleAabPath(String watchFaceModule) {
        return new File("")
                .getAbsoluteFile()
                .toPath()
                .resolve(
                        String.format(
                                "../../samples/%1$s/build/outputs/bundle/release/%1$s-release.aab",
                                watchFaceModule))
                .toString();
    }

    private static EvaluationSettings getTestEvaluationSettings() {
        return new EvaluationSettings("", "");
    }
}
