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
    @Test
    fun loadFromDocument_resolvesResourceReference() {
        val apkZip = ZipFile(Path.of(SAMPLE_WF_BASE_ARTIFACTS_PATH, "apk/release/sample-wf-release.apk").toFile())
        val arscEntry = apkZip.getEntry("resources.arsc")
        val arscBytes = apkZip.getInputStream(arscEntry).use { it.readBytes() }
        val resourceTable = AndroidResourceLoader.loadResourceTable(java.io.ByteArrayInputStream(arscBytes))

        val appNameResId = getResourceId(resourceTable, "string", "app_name")
        val appNameHex = "0x" + Integer.toHexString(appNameResId).uppercase()

        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.newDocument()
        
        val manifest = doc.createElement("manifest")
        manifest.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        doc.appendChild(manifest)
        
        val usesSdk = doc.createElement("uses-sdk")
        usesSdk.setAttributeNS("http://schemas.android.com/apk/res/android", "android:minSdkVersion", "33")
        usesSdk.setAttributeNS("http://schemas.android.com/apk/res/android", "android:targetSdkVersion", "33")
        manifest.appendChild(usesSdk)
        
        val application = doc.createElement("application")
        manifest.appendChild(application)
        
        val property = doc.createElement("property")
        property.setAttributeNS("http://schemas.android.com/apk/res/android", "android:name", "com.google.wear.watchface.format.version")
        property.setAttributeNS("http://schemas.android.com/apk/res/android", "android:value", "@id/$appNameHex")
        application.appendChild(property)
        
        try {
            AndroidManifest.loadFromDocument(doc, resourceTable)
            org.junit.Assert.fail("Expected NumberFormatException")
        } catch (e: NumberFormatException) {
            assertThat(e.message).contains("Failed to parse wffVersion: 'MemorySample'")
        }
    }

    private fun getResourceId(table: com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk, typeName: String, entryName: String): Int {
        for (pkg in table.packages) {
            for (typeChunk in pkg.typeChunks) {
                if (typeChunk.typeName == typeName) {
                    for ((entryId, entry) in typeChunk.entries) {
                        if (entry.key() == entryName) {
                            return (pkg.id shl 24) or (typeChunk.id shl 16) or entryId
                        }
                    }
                }
            }
        }
        throw RuntimeException("Resource not found: $typeName/$entryName")
    }
}