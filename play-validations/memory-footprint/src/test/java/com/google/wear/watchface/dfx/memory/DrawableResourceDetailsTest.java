package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.DrawableResourceDetails.fromPackageFile;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.Test;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class DrawableResourceDetailsTest {
    private static final String TEST_PACKAGE_FILES_ROOT =
            "test-samples/sample-wf/build/outputs/unpackedBundle/release";

    private static final DrawableResourceDetails expectedRobotoRegular =
            DrawableResourceDetails.builder()
                    .setName("roboto_regular")
                    .setBiggestFrameFootprintBytes(515100)
                    .setNumberOfImages(1)
                    .build();

    private static final DrawableResourceDetails expectedPng =
            DrawableResourceDetails.builder()
                    .setName("dial")
                    .setBiggestFrameFootprintBytes(810000)
                    .setNumberOfImages(1)
                    .setWidth(450)
                    .setHeight(450)
                    .setBounds(new DrawableResourceDetails.Bounds(9, 14, 443, 432))
                    .setSha1("954f5884424465ce39114eb61173700ebf8209b8")
                    .setCanUseRGB565(true)
                    .build();

    @Test
    public void fromPackageFile_parsesTtfWithExtension() throws Exception {
        AndroidResource ttfFile = readPackageFile("base/res/font/roboto_regular.ttf");

        Optional<DrawableResourceDetails> fontDetails =
                DrawableResourceDetails.fromPackageResource(ttfFile);

        assertThat(fontDetails.isPresent()).isTrue();
        assertThat(fontDetails.get()).isEqualTo(expectedRobotoRegular);
    }

    @Test
    public void fromPackageFile_parsesTtfWithoutExtension() throws Exception {
        AndroidResource ttfFile =
                changePath(
                        readPackageFile("base/res/font/roboto_regular.ttf"),
                        Paths.get("base/res/font/roboto_regular"));

        Optional<DrawableResourceDetails> fontDetails =
                DrawableResourceDetails.fromPackageResource(ttfFile);

        assertThat(fontDetails.isPresent()).isTrue();
        assertThat(fontDetails.get()).isEqualTo(expectedRobotoRegular);
    }

    @Test
    public void fromPackageFile_parsesPngWithExtension() throws Exception {
        AndroidResource pngFile = readPackageFile("base/res/drawable-nodpi/dial.png");

        Optional<DrawableResourceDetails> pngDetails =
                DrawableResourceDetails.fromPackageResource(pngFile);

        assertThat(pngDetails.isPresent()).isTrue();
        assertThat(pngDetails.get()).isEqualTo(expectedPng);
    }

    @Test
    public void fromPackageFile_parsesPngWithoutExtension() throws Exception {
        AndroidResource pngFile =
                changePath(
                        readPackageFile("base/res/drawable-nodpi/dial.png"),
                        Paths.get("base/res/drawable-nodpi/dial"));

        Optional<DrawableResourceDetails> pngDetails =
                DrawableResourceDetails.fromPackageResource(pngFile);

        assertThat(pngDetails.isPresent()).isTrue();
        assertThat(pngDetails.get()).isEqualTo(expectedPng);
    }

    @Test
    public void fromPackageFile_parsesPngFromDifferentQualifierDrawable() throws Exception {
        ImmutableList<String> testFilePaths =
                ImmutableList.of("base/res/drawable-xhdpi/dial.png", "base/res/drawable/dial.png");
        AndroidResource originalPackageFile =
                readPackageFile("base/res/drawable-nodpi/dial.png");
        for (String testFilePath : testFilePaths) {
            AndroidResource testPackageFile =
                    changePath(originalPackageFile, Paths.get(testFilePath));

            Optional<DrawableResourceDetails> testDrawableDetails =
                    DrawableResourceDetails.fromPackageResource(testPackageFile);

            assertThat(testDrawableDetails.isPresent()).isTrue();
        }
    }

    @Test
    public void fromPackageFile_parsesResourceFromApkRoot() throws Exception {
        // the parsing should work for both AAB-style paths (containing the base module name as the
        // first component) and APK-style paths, where the module name does not exist
        ImmutableList<String> testFilePaths =
                ImmutableList.of(
                        "base/res/drawable-nodpi/dial.png", "base/res/font/roboto_regular.ttf");
        for (String testFilePath : testFilePaths) {
            AndroidResource testPackageFile =
                    changePath(
                            readPackageFile(testFilePath),
                            // drop the module name from the paths
                            dropSections(Paths.get(testFilePath), 1));

            Optional<DrawableResourceDetails> testDrawableDetails =
                    DrawableResourceDetails.fromPackageResource(testPackageFile);

            assertThat(testDrawableDetails.isPresent()).isTrue();
        }
    }

    @Test
    public void fromPackageFile_parsesResourceFromWindowsPath() throws Exception {
        // the parsing should work for both AAB-style paths (containing the base module name as the
        // first component) and APK-style paths, where the module name does not exist
        try (FileSystem windowsFileSystem = Jimfs.newFileSystem(Configuration.windows())) {
            ImmutableList<String> testFilePaths =
                ImmutableList.of(
                     "base/res/font/roboto_regular.ttf", "base/res/drawable-nodpi/dial.png");
            for (String testFilePath : testFilePaths) {
                AndroidResource testPackageFile =
                    changePath(readPackageFile(testFilePath),
                    makeWindowsPath(testFilePath, windowsFileSystem));

                Optional<DrawableResourceDetails> testDrawableDetails =
                        DrawableResourceDetails.fromPackageResource(testPackageFile);

                assertThat(testDrawableDetails.isPresent()).isTrue();
            }
        }
    }

    @Test
    public void fromPackageFile_parsesResourceFromAssetsAndRaw() throws Exception {
        ImmutableList<String> testFilePaths =
                ImmutableList.of("base/res/drawable-xhdpi/dial.png", "base/res/drawable/dial.png");
        AndroidResource originalFile = readPackageFile("base/res/drawable-nodpi/dial.png");
        for (String testFilePath : testFilePaths) {
            AndroidResource testPackageFile =
                    changePath(originalFile, Paths.get(testFilePath));

            Optional<DrawableResourceDetails> testDrawableDetails =
                    DrawableResourceDetails.fromPackageResource(testPackageFile);

            assertThat(testDrawableDetails.isPresent()).isTrue();
            assertThat(testDrawableDetails.get()).isEqualTo(expectedPng);
        }
    }

    @Test
    public void fromPackageFile_returnsNoneOnNonDrawableResourceFileUnderDrawables()
            throws Exception {
        AndroidResource unexpectedFile =
                changePath(
                        readPackageFile("base/res/xml/watch_face_info.xml"),
                        Paths.get("base/res/drawable/non-image"));

        Optional<DrawableResourceDetails> resourceDetailsOptional =
                DrawableResourceDetails.fromPackageResource(unexpectedFile);

        assertThat(resourceDetailsOptional.isPresent()).isFalse();
    }

    @Test
    public void fromPackageFile_returnsNoneOnNonDrawableResourceFile() throws Exception {
        AndroidResource unexpectedFile = readPackageFile("base/res/xml/watch_face_info.xml");

        Optional<DrawableResourceDetails> resourceDetailsOptional =
                DrawableResourceDetails.fromPackageResource(unexpectedFile);

        assertThat(resourceDetailsOptional.isPresent()).isFalse();
    }

    @Test
    public void computeBounds() throws Exception {
        DrawableResourceDetails drawableResourceDetails =
                getDrawableResourceDetails("alpha.png").get();

        // We expect the bounds to cover the bottom right hand corner only.
        assertThat(drawableResourceDetails.getBounds().left).isEqualTo(244);
        assertThat(drawableResourceDetails.getBounds().right).isEqualTo(380);
        assertThat(drawableResourceDetails.getBounds().top).isEqualTo(330);
        assertThat(drawableResourceDetails.getBounds().bottom).isEqualTo(415);
    }

    @Test
    public void canUseRGB565() throws Exception {
        DrawableResourceDetails fourBppOK = getDrawableResourceDetails("4bpp_ok.png").get();
        DrawableResourceDetails eightBppNeeded =
                getDrawableResourceDetails("8bpp_needed.png").get();

        assertThat(fourBppOK.canUseRGB565()).isTrue();
        assertThat(eightBppNeeded.canUseRGB565()).isFalse();
    }

    private AndroidResource readPackageFile(String originFilePath) throws Exception {
        Path filePath = Paths.get(TEST_PACKAGE_FILES_ROOT, originFilePath);
        byte[] bytes = Files.readAllBytes(filePath);
        return  AndroidResource.fromPath(filePath, bytes);
    }

    private AndroidResource changePath(AndroidResource origin, Path newPath) {
        return AndroidResource.fromPath(newPath, origin.getData());
    }

    private Path dropSections(Path path, int count) {
        return path.subpath(count, path.getNameCount());
    }

    private Path makeWindowsPath(String originalPath, FileSystem fileSystem) {
        String[] pathSplits = originalPath.split("/");
        return fileSystem.getPath(
                pathSplits[0], Arrays.copyOfRange(pathSplits, 1, pathSplits.length));
    }

    Optional<DrawableResourceDetails> getDrawableResourceDetails(String name) throws Exception {
        String path = String.format("/res/drawable/%s", name);
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return fromPackageFile(
                    new InputPackage.PackageFile(
                            FileSystems.getDefault().getPath("res", "drawable", name),
                            AndroidResourceTable.readAllBytes(is)));
        }
    }
}
