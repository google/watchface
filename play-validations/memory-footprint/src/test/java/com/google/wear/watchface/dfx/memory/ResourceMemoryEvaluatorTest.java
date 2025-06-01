package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.EvaluationSettings.parseFromArguments;
import static com.google.wear.watchface.dfx.memory.ResourceMemoryEvaluator.evaluateMemoryFootprint;
import static com.google.wear.watchface.dfx.memory.WatchFaceData.SYSTEM_DEFAULT_FONT_SIZE;
import static junit.framework.TestCase.assertEquals;
import static org.junit.runners.Parameterized.Parameter;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

            final String cliSchemaVersion;

            private TestParams(
                    String watchFace,
                    long expectedActiveFootprintBytes,
                    long expectedAmbientFootprintBytes,
                    int expectedLayouts,
                    String cliSchemaVersion) {
                this.watchFace = watchFace;
                this.expectedActiveFootprintBytes = expectedActiveFootprintBytes;
                this.expectedAmbientFootprintBytes = expectedAmbientFootprintBytes;
                this.expectedLayouts = expectedLayouts;
                this.cliSchemaVersion = cliSchemaVersion;
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
                            "resDirectory",
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
                                            /* expectedActiveFootprintBytes= */ 4712628
                                                    + SYSTEM_DEFAULT_FONT_SIZE,
                                            /* expectedAmbientFootprintBytes= */ 2687628,
                                            /* expectedLayouts= */ 1,
                                            /* cliSchemaVersion= */ artifactRelativePath.equals(
                                                            "resDirectory")
                                                    ? "1"
                                                    : null))
                    .collect(Collectors.toList());
        }

        @Test
        public void validate_hasExpectedFootprint() {
            List<MemoryFootprint> multiShapesFootprint =
                    evaluateMemoryFootprint(
                            testParams.cliSchemaVersion == null
                                    ? new EvaluationSettings(testParams.watchFace)
                                    : new EvaluationSettings(
                                            testParams.watchFace, testParams.cliSchemaVersion));

            assertEquals(testParams.expectedLayouts, multiShapesFootprint.size());
            assertEquals(
                    testParams.expectedActiveFootprintBytes,
                    multiShapesFootprint.get(0).getMaxActiveBytes());
            assertEquals(
                    testParams.expectedAmbientFootprintBytes,
                    multiShapesFootprint.get(0).getMaxAmbientBytes());
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
                                            "--estimate-optimization")
                                    .get());

            assertEquals(4722916, multiShapesFootprint.get(0).getMaxActiveBytes());
            assertEquals(2326806, multiShapesFootprint.get(0).getMaxAmbientBytes());
        }

        @Test
        public void main_writesJsonInReportMode() {
            ByteArrayOutputStream outStreamCaptor = new ByteArrayOutputStream();
            PrintStream old = System.out;
            System.setOut(new PrintStream(outStreamCaptor));
            String watchFacePath =
                    Paths.get(SAMPLE_WF_BASE_ARTIFACTS_PATH, "bundle/release/sample-wf-release.aab")
                            .toString();

            ResourceMemoryEvaluator.main(new String[] {"--watch-face", watchFacePath, "--report"});
            System.out.flush();
            System.setOut(old);

            JsonElement jsonElement = JsonParser.parseString(outStreamCaptor.toString());
            assertThat(jsonElement.isJsonObject()).isTrue();
        }
    }

    private static EvaluationSettings getTestEvaluationSettings() {
        return new EvaluationSettings("", false, false);
    }
}
