package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Streams;
import com.google.common.truth.Correspondence;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class InputPackageTest {

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<?> parameters() {
        Path folderPath =
                new File("test-samples/sample-wf/build/outputs/unpackedBundle/release").toPath();
        Object[][] p =
                new Object[][] {
                    {"absolute", folderPath.toAbsolutePath().toString()},
                    {"relative", folderPath.toString()}
                };
        return Arrays.asList(p);
    }

    @Parameterized.Parameter(0)
    public String label;

    @Parameterized.Parameter(1)
    public String testAabDirectory;

    private static final Correspondence<AndroidResource, String> VERIFY_PACKAGE_NAME_ONLY =
            Correspondence.transforming(
                    packageFile -> packageFile.getFilePath().toString(),
                    "has the same file path as");

    @Test
    public void open_handlesFolder() {
        List<AndroidResource> packageFiles;
        AndroidManifest manifest;
        try (InputPackage inputPackage = InputPackage.open(testAabDirectory)) {
            packageFiles =
                    Streams.stream(inputPackage.getWatchFaceFiles().iterator())
                            // remove this file, which is automatically created on MacOS
                            .filter(
                                    x ->
                                            !x.getFilePath()
                                                    .getFileName()
                                                    .toString()
                                                    .equals(".DS_Store"))
                            .collect(Collectors.toList());

            manifest = inputPackage.getManifest();
        }

        assertThat(packageFiles)
                .comparingElementsUsing(VERIFY_PACKAGE_NAME_ONLY)
                .containsExactly(
                        "base/res/drawable-nodpi/bg.png",
                        "base/res/drawable-nodpi/dial.png",
                        "base/res/drawable-nodpi/preview.png",
                        "base/res/drawable-nodpi/shape_1.png",
                        "base/res/drawable-nodpi/shape_2.png",
                        "base/res/drawable-nodpi/hand_1.png",
                        "base/res/drawable-nodpi/hand_2.png",
                        "base/res/font/roboto_regular.ttf",
                        "base/res/font/open_sans_regular.ttf",
                        "base/res/raw/watchface.xml",
                        "base/res/values/strings.xml",
                        "base/res/xml/watch_face_info.xml",
                        "base/manifest/AndroidManifest.xml");

        assertThat(manifest.getWffVersion()).isEqualTo(1);
        // Unpacked bundle doesn't specify min or target sdk, these default to 1 according to specs.
        assertThat(manifest.getMinSdkVersion()).isEqualTo(1);
        assertThat(manifest.getTargetSdkVersion()).isEqualTo(1);
    }
}
