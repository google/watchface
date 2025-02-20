package com.google.wear.watchface.dfx.memory

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.Path
import java.util.regex.Pattern
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@RunWith(JUnit4::class)
class AndroidManifestTest {
    private val SAMPLE_WF_BASE_ARTIFACTS_PATH = "test-samples/sample-wf/build/outputs"

    @Test
    fun loadFromAab_validVersionsTest() {
        val wffPackage =
            Path.of(SAMPLE_WF_BASE_ARTIFACTS_PATH, "bundle/release/sample-wf-release.aab")

        val manifest = AndroidManifest.loadFromAab(ZipFile(wffPackage.toFile()))

        assertThat(manifest.wffVersion).isEqualTo(1)
        assertThat(manifest.minSdkVersion).isEqualTo(33)
        assertThat(manifest.targetSdkVersion).isEqualTo(33)
    }

    @Test
    fun loadFromApk_validDebugVersionsTest() {
        val wffPackage = Path.of(SAMPLE_WF_BASE_ARTIFACTS_PATH, "apk/debug/sample-wf-debug.apk")

        val manifest = AndroidManifest.loadFromApk(ZipFile(wffPackage.toFile()))

        assertThat(manifest.wffVersion).isEqualTo(1)
        assertThat(manifest.minSdkVersion).isEqualTo(33)
        assertThat(manifest.targetSdkVersion).isEqualTo(33)
    }

    @Test
    fun loadFromApk_validReleaseVersionsTest() {
        val wffPackage = Path.of(SAMPLE_WF_BASE_ARTIFACTS_PATH, "apk/release/sample-wf-release.apk")

        val manifest = AndroidManifest.loadFromApk(ZipFile(wffPackage.toFile()))

        assertThat(manifest.wffVersion).isEqualTo(1)
        assertThat(manifest.minSdkVersion).isEqualTo(33)
        assertThat(manifest.targetSdkVersion).isEqualTo(33)
    }

    @Test
    fun loadFromAabDirectory_validVersionsTest() {
        val wffDirectory = Path.of(SAMPLE_WF_BASE_ARTIFACTS_PATH, "unpackedBundle/release")

        val manifest = AndroidManifest.loadFromAabDirectory(wffDirectory)

        assertThat(manifest?.wffVersion).isEqualTo(1)
        // The unbundled manifest does not specify min and target SDKs, which according to specs
        // should then default to: minSdk -> 1, targetSdk -> minSdk.
        assertThat(manifest?.minSdkVersion).isEqualTo(1)
        assertThat(manifest?.targetSdkVersion).isEqualTo(1)
    }

    @Test
    fun loadFromMokkaZip_validVersionsTest() {
        val wffPath = Path.of(
            SAMPLE_WF_BASE_ARTIFACTS_PATH,
            "zipApk/com.google.wear.watchface.memory.sample.zip"
        )
        val wffZip = ZipFile(wffPath.toFile())

        val baseSplitPattern = Pattern.compile(".*base[-_]split.*")
        val baseSplitApk = wffZip.stream()
            .filter { x -> baseSplitPattern.matcher(x!!.name).matches() }
            .findFirst()
        if (!baseSplitApk.isPresent) {
            throw InvalidTestRunException("Zip file does not contain a base split apk")
        }
        val baseSplitApkZip = ZipInputStream(wffZip.getInputStream(baseSplitApk.get()))

        val manifest = AndroidManifest.loadFromMokkaZip(baseSplitApkZip)

        assertThat(manifest.wffVersion).isEqualTo(1)
        assertThat(manifest.minSdkVersion).isEqualTo(33)
        assertThat(manifest.targetSdkVersion).isEqualTo(33)
    }
}