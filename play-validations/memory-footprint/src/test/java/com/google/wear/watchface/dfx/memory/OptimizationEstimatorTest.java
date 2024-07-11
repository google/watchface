package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.DrawableResourceDetails.Bounds;
import static com.google.wear.watchface.dfx.memory.EvaluationSettings.parseFromArguments;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

public class OptimizationEstimatorTest {
    private static final EvaluationSettings TEST_SETTINGS =
            parseFromArguments(
                            "--watch-face",
                            "path/to/watchface.apk",
                            "--schema-version",
                            "1",
                            "--estimate-optimization")
                    .get();

    @Test
    public void imageCropping() throws Exception {
        Document document = readDocument("/ImageReusedAtMultipleSizes.xml");

        DrawableResourceDetails image = resDetails("image", 200, 200, new Bounds(50, 50, 150, 175));
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder().put("image", image).build();

        new OptimizationEstimator(document, map, TEST_SETTINGS).estimateOptimizations();

        assertThat(image.getWidth()).isEqualTo(100);
        assertThat(image.getHeight()).isEqualTo(125);
    }

    @Test
    public void imageScalingAndCropping() throws Exception {
        Document document = readDocument("/ImageReusedAtMultipleSizes.xml");

        DrawableResourceDetails image = resDetails("image", 400, 400, new Bounds(50, 50, 250, 350));
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder().put("image", image).build();

        new OptimizationEstimator(document, map, TEST_SETTINGS).estimateOptimizations();

        assertThat(image.getWidth()).isEqualTo(100);
        assertThat(image.getHeight()).isEqualTo(150);
    }

    @Test
    public void imageScaling() throws Exception {
        Document document = readDocument("/SmallImage.xml");

        DrawableResourceDetails image = resDetails("image", 700, 700, new Bounds(0, 0, 700, 700));
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder().put("image", image).build();

        new OptimizationEstimator(document, map, TEST_SETTINGS).estimateOptimizations();

        assertThat(image.getWidth()).isEqualTo(70);
        assertThat(image.getHeight()).isEqualTo(70);
    }

    @Test
    public void bitmapFontCropping() throws Exception {
        Document document = readDocument("/layer-split/SimpleDigitalClock.xml");

        DrawableResourceDetails image1 = resDetails("image1", 200, 200);
        DrawableResourceDetails image2 = resDetails("image2", 200, 200);
        DrawableResourceDetails char0 = resDetails("char-0", 20, 32, new Bounds(4, 4, 18, 29));
        DrawableResourceDetails char1 = resDetails("char-1", 20, 32, new Bounds(3, 6, 16, 20));
        DrawableResourceDetails char2 = resDetails("char-2", 20, 32, new Bounds(1, 3, 18, 25));
        DrawableResourceDetails char3 = resDetails("char-3", 20, 32, new Bounds(5, 5, 19, 22));
        DrawableResourceDetails char4 = resDetails("char-4", 20, 32, new Bounds(6, 4, 18, 28));
        DrawableResourceDetails char5 = resDetails("char-5", 20, 32, new Bounds(7, 7, 17, 27));
        DrawableResourceDetails char6 = resDetails("char-6", 20, 32, new Bounds(3, 3, 18, 25));
        DrawableResourceDetails char7 = resDetails("char-7", 20, 32, new Bounds(5, 5, 14, 24));
        DrawableResourceDetails char8 = resDetails("char-8", 20, 32, new Bounds(4, 6, 17, 26));
        DrawableResourceDetails char9 = resDetails("char-9", 20, 32, new Bounds(2, 8, 18, 28));

        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", image1)
                        .put("image2", image2)
                        .put("char-0", char0)
                        .put("char-1", char1)
                        .put("char-2", char2)
                        .put("char-3", char3)
                        .put("char-4", char4)
                        .put("char-5", char5)
                        .put("char-6", char6)
                        .put("char-7", char7)
                        .put("char-8", char8)
                        .put("char-9", char9)
                        .build();

        new OptimizationEstimator(document, map, TEST_SETTINGS).estimateOptimizations();

        assertThat(char0.getWidth()).isEqualTo(14);
        assertThat(char0.getHeight()).isEqualTo(25);
        assertThat(char9.getWidth()).isEqualTo(16);
        assertThat(char9.getHeight()).isEqualTo(20);
    }

    @Test
    public void bitmapFontScalingAndCropping() throws Exception {
        Document document = readDocument("/layer-split/SimpleDigitalClockBigFont.xml");

        DrawableResourceDetails image1 = resDetails("image1", 200, 200);
        DrawableResourceDetails image2 = resDetails("image2", 200, 200);
        DrawableResourceDetails char0 =
                resDetails("char-0", 200, 320, new Bounds(40, 40, 180, 290));
        DrawableResourceDetails char1 =
                resDetails("char-1", 200, 320, new Bounds(30, 60, 160, 200));
        DrawableResourceDetails char2 =
                resDetails("char-2", 200, 320, new Bounds(10, 30, 180, 250));
        DrawableResourceDetails char3 =
                resDetails("char-3", 200, 320, new Bounds(50, 50, 190, 220));
        DrawableResourceDetails char4 =
                resDetails("char-4", 200, 320, new Bounds(60, 40, 180, 280));
        DrawableResourceDetails char5 =
                resDetails("char-5", 200, 320, new Bounds(70, 70, 170, 270));
        DrawableResourceDetails char6 =
                resDetails("char-6", 200, 320, new Bounds(30, 30, 180, 250));
        DrawableResourceDetails char7 =
                resDetails("char-7", 200, 320, new Bounds(50, 50, 140, 240));
        DrawableResourceDetails char8 =
                resDetails("char-8", 200, 320, new Bounds(40, 60, 170, 260));
        DrawableResourceDetails char9 =
                resDetails("char-9", 200, 320, new Bounds(20, 80, 180, 280));

        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", image1)
                        .put("image2", image2)
                        .put("char-0", char0)
                        .put("char-1", char1)
                        .put("char-2", char2)
                        .put("char-3", char3)
                        .put("char-4", char4)
                        .put("char-5", char5)
                        .put("char-6", char6)
                        .put("char-7", char7)
                        .put("char-8", char8)
                        .put("char-9", char9)
                        .build();

        new OptimizationEstimator(document, map, TEST_SETTINGS).estimateOptimizations();

        // Note the image is 10x oversized so the results can be much smaller than the input.
        assertThat(char0.getWidth()).isEqualTo(14);
        assertThat(char0.getHeight()).isEqualTo(25);
        assertThat(char9.getWidth()).isEqualTo(16);
        assertThat(char9.getHeight()).isEqualTo(20);
    }

    DrawableResourceDetails resDetails(String name, int width, int height) {
        return DrawableResourceDetails.builder()
                .setName(name)
                .setNumberOfImages(1)
                .setWidth(width)
                .setHeight(height)
                .setBounds(new Bounds(0, 0, width, height))
                .setBiggestFrameFootprintBytes(width * height * 4)
                .build();
    }

    DrawableResourceDetails resDetails(String name, int width, int height, Bounds bounds) {
        return DrawableResourceDetails.builder()
                .setName(name)
                .setNumberOfImages(1)
                .setWidth(width)
                .setHeight(height)
                .setBounds(bounds)
                .setBiggestFrameFootprintBytes(width * height * 4)
                .build();
    }

    private Document readDocument(String documentName) throws Exception {
        try (InputStream is =
                getClass().getResourceAsStream(String.format(documentName, documentName))) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }
}
