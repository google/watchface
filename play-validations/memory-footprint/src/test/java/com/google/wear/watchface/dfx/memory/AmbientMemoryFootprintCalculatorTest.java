package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.EvaluationSettings.parseFromArguments;

import com.google.common.collect.ImmutableMap;
import java.io.InputStream;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;

public class AmbientMemoryFootprintCalculatorTest {

    private static final int TEST_WIDTH = 300;
    private static final int TEST_HEIGHT = 300;
    private static final int TEST_LAYER_SIZE = TEST_WIDTH * TEST_HEIGHT * 4;

    private static final EvaluationSettings TEST_SETTINGS =
            parseFromArguments(
                            "--watch-face",
                            "path/to/watchface.apk",
                            "--schema-version",
                            "1",
                            "--disable-ambient-deduplication")
                    .get();
    private static final EvaluationSettings TEST_SETTINGS_VERBOSE =
            parseFromArguments(
                            "--watch-face",
                            "path/to/watchface.apk",
                            "--schema-version",
                            "1",
                            "--disable-ambient-deduplication",
                            "--verbose")
                    .get();
    private static final EvaluationSettings TEST_SETTINGS_NO_OLD_CLOCKS =
            parseFromArguments(
                            "--watch-face",
                            "path/to/watchface.apk",
                            "--schema-version",
                            "1",
                            "--disable-ambient-deduplication",
                            "--disable-old-style-clocks")
                    .get();
    private static final EvaluationSettings TEST_SETTINGS_DEDUPLICATE =
            parseFromArguments("--watch-face", "path/to/watchface.apk", "--schema-version", "1")
                    .get();
    private static final EvaluationSettings TEST_SETTINGS_V1_LIMITATIONS =
            parseFromArguments(
                            "--watch-face",
                            "path/to/watchface.apk",
                            "--schema-version",
                            "1",
                            "--disable-ambient-deduplication",
                            "--apply-v1-offload-limitations")
                    .get();

    @Test
    public void computeAmbientMemoryFootprint_computesTwoLayersWithSimpleDigitalClock()
            throws Exception {
        Document document = readDocument("SimpleDigitalClock");
        int singleCharSize = 300;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("char-0", resDetails("char-0", singleCharSize))
                        .put("char-1", resDetails("char-1", singleCharSize))
                        .put("char-2", resDetails("char-2", singleCharSize))
                        .put("char-3", resDetails("char-3", singleCharSize))
                        .put("char-4", resDetails("char-4", singleCharSize))
                        .put("char-5", resDetails("char-5", singleCharSize))
                        .put("char-6", resDetails("char-6", singleCharSize))
                        .put("char-7", resDetails("char-7", singleCharSize))
                        .put("char-8", resDetails("char-8", singleCharSize))
                        .put("char-9", resDetails("char-9", singleCharSize))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_NO_OLD_CLOCKS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 10 * singleCharSize);
    }

    @Test
    public void computeAmbientMemoryFootprint_computesTwoLayersWithSimpleAnalogClock()
            throws Exception {
        Document document = readDocument("SimpleAnalogClock");
        int singleHandSize = 300;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("hour-hand", resDetails("hour-hand", singleHandSize))
                        .put("minute-hand", resDetails("minute-hand", singleHandSize))
                        .put("second-hand", resDetails("second-hand", singleHandSize))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_NO_OLD_CLOCKS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // NB the second hand should not be displayed in ambient.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 2 * singleHandSize);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleAnalogClock_supported() throws Exception {
        Document document = readDocument("OldStyleAnalogClock");
        int singleHandSize = 300;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("hour-hand", resDetails("hour-hand", singleHandSize))
                        .put("minute-hand", resDetails("minute-hand", singleHandSize))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // NB the second hand should not be displayed in ambient.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 2 * singleHandSize);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleAnalogClock_notSupported() throws Exception {
        Document document = readDocument("OldStyleAnalogClock");
        int singleHandSize = 300;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("hour-hand", resDetails("hour-hand", singleHandSize))
                        .put("minute-hand", resDetails("minute-hand", singleHandSize))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_NO_OLD_CLOCKS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // The example flattens down to a single layer.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockType1_supported()
            throws Exception {
        Document document = readDocument("OldStyleDigitalClockType1");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("ariel.ttf", resDetails("ariel.ttf", 100))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // We expect two layers due to the digital clock in the middle, and ariel.ttf.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 100);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockType1_notSupported()
            throws Exception {
        Document document = readDocument("OldStyleDigitalClockType1");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("ariel.ttf", resDetails("ariel.ttf", 100))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_NO_OLD_CLOCKS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // The example flattens down to a single layer.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockType2_supported()
            throws Exception {
        Document document = readDocument("OldStyleDigitalClockType2");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("ariel.ttf", resDetails("ariel.ttf", 100))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // We expect two layers due to the digital clock in the middle, and ariel.ttf.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 100);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockType2_notSupported()
            throws Exception {
        Document document = readDocument("OldStyleDigitalClockType2");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("ariel.ttf", resDetails("ariel.ttf", 100))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_NO_OLD_CLOCKS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // The example flattens down to a single layer.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockWithSeconds() throws Exception {
        Document document = readDocument("OldStyleDigitalClockWithSeconds");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("ariel.ttf", resDetails("ariel.ttf", 100))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // We expect only one layer since seconds are not offloaed since we update once per minute.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void computeAmbientMemoryFootprint_oldStyleDigitalClockTypeHours_noLayerForDayHour()
            throws Exception {
        Document document = readDocument("OldStyleDigitalClockDayHour");
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .build();

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // We expect only one layer because [DAY_HOUR] changes too slowly to offload.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void computeAmbientMemoryFootprint() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("digital-clock-1", resDetails("digital-clock-1", 300))
                        .put("digital-clock-2", resDetails("digital-clock-2", 200))
                        .put("digital-clock-3", resDetails("digital-clock-3", 100))
                        .build();
        Document document = readDocument("DigitalClocksWithOverlappingResources");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // 2 layers + first list option chooses clock 2 and second chooses clock 1
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 500);
    }

    @Test
    public void allImageNodesHiddenInAmbient() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("digital-clock-1", resDetails("digital-clock-1", 300))
                        .build();
        Document document = readDocument("AllImageNodesHiddenInAmbient");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // 0 layers and one digital clock.
        assertThat(result).isEqualTo(300);
    }

    @Test
    public void someImageNodesHiddenInAmbient() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("image3", resDetails("image1", 12))
                        .put("digital-clock-1", resDetails("digital-clock-1", 300))
                        .put("digital-clock-2", resDetails("digital-clock-2", 300))
                        .build();
        Document document = readDocument("SomeImageNodesHiddenInAmbient");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // 1 layer and two clocks.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE + 600);
    }

    @Test
    public void complicationLayers() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("image3", resDetails("image1", 12))
                        .build();
        Document document = readDocument("Complications");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        assertThat(result).isEqualTo(3 * TEST_LAYER_SIZE);
    }

    @Test
    public void complicationLayers_v1_limitations() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("image3", resDetails("image1", 12))
                        .build();
        Document document = readDocument("Complications");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_V1_LIMITATIONS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // Only one layer because cimplications are not offloaded.
        assertThat(result).isEqualTo(1 * TEST_LAYER_SIZE);
    }

    @Test
    public void multipleClocks() throws Exception {
        int singleCharSize = 300;
        int singleHandSize = 400;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("image3", resDetails("image1", 12))
                        .put("image4", resDetails("image1", 12))
                        .put("char-0", resDetails("char-0", singleCharSize))
                        .put("char-1", resDetails("char-1", singleCharSize))
                        .put("char-2", resDetails("char-2", singleCharSize))
                        .put("char-3", resDetails("char-3", singleCharSize))
                        .put("char-4", resDetails("char-4", singleCharSize))
                        .put("char-5", resDetails("char-5", singleCharSize))
                        .put("char-6", resDetails("char-6", singleCharSize))
                        .put("char-7", resDetails("char-7", singleCharSize))
                        .put("char-8", resDetails("char-8", singleCharSize))
                        .put("char-9", resDetails("char-9", singleCharSize))
                        .put("hour-hand", resDetails("hour-hand", singleHandSize))
                        .put("minute-hand", resDetails("minute-hand", singleHandSize))
                        .build();
        Document document = readDocument("MultipleClocks");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // Four layers due to interleaving of clocks and other elements, and various assets.
        assertThat(result).isEqualTo(4 * TEST_LAYER_SIZE + 3800);
    }

    @Test
    public void multipleClocks_v1_limitations() throws Exception {
        int singleCharSize = 300;
        int singleHandSize = 400;
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("image3", resDetails("image1", 12))
                        .put("image4", resDetails("image1", 12))
                        .put("char-0", resDetails("char-0", singleCharSize))
                        .put("char-1", resDetails("char-1", singleCharSize))
                        .put("char-2", resDetails("char-2", singleCharSize))
                        .put("char-3", resDetails("char-3", singleCharSize))
                        .put("char-4", resDetails("char-4", singleCharSize))
                        .put("char-5", resDetails("char-5", singleCharSize))
                        .put("char-6", resDetails("char-6", singleCharSize))
                        .put("char-7", resDetails("char-7", singleCharSize))
                        .put("char-8", resDetails("char-8", singleCharSize))
                        .put("char-9", resDetails("char-9", singleCharSize))
                        .put("hour-hand", resDetails("hour-hand", singleHandSize))
                        .put("minute-hand", resDetails("minute-hand", singleHandSize))
                        .build();
        Document document = readDocument("MultipleClocks");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_V1_LIMITATIONS)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // Two layers due to V1 limitations, and various assets.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 3800);
    }

    @Test
    public void resourceDeduplication() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("image1", resDetails("image1", 12))
                        .put("image2", resDetails("image1", 12))
                        .put("digital-clock-1", resDetails("digital-clock-1", 300))
                        .put("digital-clock-2", resDetails("digital-clock-2", 200))
                        .put("digital-clock-3", resDetails("digital-clock-3", 100))
                        .build();
        Document document = readDocument("DigitalClocksWithOverlappingResources");

        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_DEDUPLICATE)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        // 2 layers + one image since all of them share the same sha-1.
        assertThat(result).isEqualTo(2 * TEST_LAYER_SIZE + 12);
    }

    @Test
    public void emptyTest() throws Exception {
        Map<String, DrawableResourceDetails> map =
                ImmutableMap.<String, DrawableResourceDetails>builder().build();
        Document document = readDocument("Empty");

        // This should not crash.
        long result =
                new AmbientMemoryFootprintCalculator(document, map, TEST_SETTINGS_VERBOSE)
                        .computeAmbientMemoryFootprint(TEST_WIDTH, TEST_HEIGHT);

        assertThat(result).isEqualTo(0);
    }

    DrawableResourceDetails resDetails(String name, long size) {
        return DrawableResourceDetails.builder()
                .setName(name)
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(size)
                .build();
    }

    private Document readDocument(String documentName) throws Exception {
        try (InputStream is =
                getClass()
                        .getResourceAsStream(String.format("/layer-split/%s.xml", documentName))) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }
}
