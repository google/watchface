package com.google.wear.watchface.dfx.memory

import com.google.common.truth.Truth.assertThat
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
        val resourceTable = AndroidResourceLoader.loadResourceTable(ByteArrayInputStream(arscBytes))

        val appNameResId = getResourceId(resourceTable, "string", "app_name")
        val appNameHex = "0x" + Integer.toHexString(appNameResId).uppercase()

        val factory = DocumentBuilderFactory.newInstance()
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
            fail("Expected NumberFormatException")
        } catch (e: NumberFormatException) {
            assertThat(e.message).contains("Failed to parse wffVersion: 'MemorySample'")
        }
    }

    @Test
    fun loadFromAabDirectory_rejectsManifestWithDoctype() {
        val tempDir = Files.createTempDirectory("manifest-doctype-test")
        try {
            val manifestDir = tempDir.resolve("base/manifest")
            Files.createDirectories(manifestDir)
            Files.writeString(
                manifestDir.resolve("AndroidManifest.xml"),
                "<!DOCTYPE manifest><manifest/>"
            )

            assertThrows(Exception::class.java) {
                AndroidManifest.loadFromAabDirectory(tempDir)
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun loadFromAabDirectory_rejectsManifestWithExternalEntities() {
        val tempDir = Files.createTempDirectory("manifest-external-dtd-test")
        val connected = AtomicBoolean(false)
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val listener = Thread {
                try {
                    server.accept().close()
                    connected.set(true)
                } catch (_: Exception) {}
            }.apply {
                isDaemon = true
                start()
            }

            try {
                val localFile = tempDir.resolve("local.txt")
                Files.writeString(localFile, "sample")
                val manifestDir = tempDir.resolve("base/manifest")
                Files.createDirectories(manifestDir)
                val xml = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE manifest [
                      <!ENTITY % local SYSTEM "${localFile.toUri()}">
                      <!ENTITY % remote SYSTEM "http://127.0.0.1:${server.localPort}/schema.dtd">
                      %remote;
                    ]>
                    <manifest/>
                """.trimIndent()
                Files.writeString(manifestDir.resolve("AndroidManifest.xml"), xml)

                assertThrows(Exception::class.java) {
                    AndroidManifest.loadFromAabDirectory(tempDir)
                }
                assertThat(connected.get()).isFalse()
            } finally {
                server.close()
                listener.join(1000)
                tempDir.toFile().deleteRecursively()
            }
        }
    }

    @Test
    fun resolvePlainXmlResource_ignoresValuesXmlWithDoctype() {
        val tempDir = Files.createTempDirectory("values-doctype-test")
        val connected = AtomicBoolean(false)
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            val listener = Thread {
                try {
                    server.accept().close()
                    connected.set(true)
                } catch (_: Exception) {}
            }.apply {
                isDaemon = true
                start()
            }

            try {
                val valuesDir = tempDir.resolve("values")
                Files.createDirectories(valuesDir)
                val xml = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <!DOCTYPE resources [
                      <!ENTITY ext SYSTEM "http://127.0.0.1:${server.localPort}/strings.dtd">
                    ]>
                    <resources>
                      <string name="wff_version">&ext;</string>
                    </resources>
                """.trimIndent()
                Files.writeString(valuesDir.resolve("strings.xml"), xml)

                val resolved =
                    AndroidResourceResolver.resolvePlainXmlResource(tempDir, "@string/wff_version")
                assertThat(resolved).isEqualTo("@string/wff_version")
                assertThat(connected.get()).isFalse()
            } finally {
                server.close()
                listener.join(1000)
                tempDir.toFile().deleteRecursively()
            }
        }
    }

    private fun getResourceId(table: ResourceTableChunk, typeName: String, entryName: String): Int {
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