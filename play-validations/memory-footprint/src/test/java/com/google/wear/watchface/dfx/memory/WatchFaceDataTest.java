package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class WatchFaceDataTest {
    private static final EvaluationSettings TEST_EVALUATION_SETTINGS =
            new EvaluationSettings("", "");

    private static final String TEST_PACKAGE_FILES_ROOT =
            "test-samples/sample-wf/build/outputs/unpackedBundle/release";

    private static final List<String> TEST_FILES =
            ImmutableList.of(
                    "base/res/drawable-nodpi/bg.png",
                    "base/res/raw/watchface.xml",
                    "base/res/font/roboto_regular.ttf");

    private static final ImmutableMap<String, DrawableResourceDetails> expectedDetails =
            ImmutableMap.of(
                    "bg",
                    DrawableResourceDetails.builder()
                            .setName("bg")
                            .setBiggestFrameFootprintBytes(810000)
                            .setNumberOfImages(1)
                            .setWidth(450)
                            .setHeight(450)
                            .setBounds(new DrawableResourceDetails.Bounds(0, 0, 450, 450))
                            .setSha1("44e531f06d502942fa79aacfa45fc370662e885b")
                            .setCanUseRGB565(true)
                            .build(),
                    "roboto_regular",
                    DrawableResourceDetails.builder()
                            .setName("roboto_regular")
                            .setBiggestFrameFootprintBytes(515100)
                            .setNumberOfImages(1)
                            .setWidth(0)
                            .setHeight(0)
                            .build(),
                    "Roboto",
                    DrawableResourceDetails.builder()
                            .setName("Roboto")
                            .setBiggestFrameFootprintBytes(2371712)
                            .setNumberOfImages(1)
                            .build());

    @Test
    public void fromResourcesStream_createsPackageFromLinuxPaths() {
        Stream<InputPackage.PackageFile> packageFileStream =
                TEST_FILES.stream().map(this::readPackageFile);

        WatchFaceData watchFaceData =
                WatchFaceData.fromResourcesStream(packageFileStream, TEST_EVALUATION_SETTINGS);

        assertThat(watchFaceData.watchFaceDocuments).hasSize(1);
        assertThat(watchFaceData.resourceDetailsMap).isEqualTo(expectedDetails);
    }

    @Test
    public void fromResourcesStream_createsPackageFromWindowsPaths() throws Exception {
        try (FileSystem windowsFs = Jimfs.newFileSystem(Configuration.windows())) {
            Stream<InputPackage.PackageFile> packageFileStream =
                    TEST_FILES.stream().map(x -> readWindowsPackageFile(x, windowsFs));

            WatchFaceData watchFaceData =
                    WatchFaceData.fromResourcesStream(packageFileStream, TEST_EVALUATION_SETTINGS);

            assertThat(watchFaceData.watchFaceDocuments).hasSize(1);
            assertThat(watchFaceData.resourceDetailsMap).isEqualTo(expectedDetails);
        }
    }

    private InputPackage.PackageFile readPackageFile(String path) throws RuntimeException {
        Path rootPath = Paths.get(TEST_PACKAGE_FILES_ROOT);
        Path filePath = Paths.get(TEST_PACKAGE_FILES_ROOT, path);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new InputPackage.PackageFile(rootPath.relativize(filePath), bytes);
    }

    private InputPackage.PackageFile readWindowsPackageFile(String path, FileSystem fileSystem) {
        Path filePath = Paths.get(TEST_PACKAGE_FILES_ROOT, path);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] pathSplits = path.split("/");
        Path windowsPath =
                fileSystem.getPath(
                        pathSplits[0], Arrays.copyOfRange(pathSplits, 1, pathSplits.length));
        return new InputPackage.PackageFile(windowsPath, bytes);
    }
}
