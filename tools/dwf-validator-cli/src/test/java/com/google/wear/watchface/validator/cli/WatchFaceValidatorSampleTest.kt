package com.google.wear.watchface.validator.cli

import com.google.wear.watchface.validator.error.ValidationResult
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.w3c.dom.Document

internal const val DEFAULT_FORMAT_VERSION = "1"
internal const val MAX_WFF_VERSION = 4
internal const val DWF_SAMPLES = "../samples"

@RunWith(Parameterized::class)
class WatchFaceValidatorSampleTest(val expectedVersions: Set<Int>, val filePath: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: \"{1}\" = \"{0}\"")
        fun testCases(): List<Array<Any>> = findAllSamplesWatchfaceXml()

        private fun findAllSamplesWatchfaceXml(): List<Array<Any>> {
            val samplesDir = Path.of(DWF_SAMPLES)
            Files.walk(samplesDir).use { allFilesInSamples ->
                val watchfaceXmlMatcher =
                    FileSystems.getDefault().getPathMatcher("glob:**/res/raw/watchface*.xml")

                return allFilesInSamples
                    .filter { watchfaceXmlMatcher.matches(it) }
                    .map { path ->
                        val associatedManifestPath =
                            path.resolve("../../../AndroidManifest.xml").normalize()
                        val minVersion = getDwfVersionFromManifest(associatedManifestPath)
                        val expectedVersions = (minVersion..MAX_WFF_VERSION).toSet()

                        arrayOf(expectedVersions, path.toAbsolutePath().normalize().toString())
                    }
                    .collect(Collectors.toList())
            }
        }

        private fun getDwfVersionFromManifest(manifestPath: Path): Int {
            var version: String = DEFAULT_FORMAT_VERSION
            if (Files.exists(manifestPath)) {
                val manifest: Document? =
                    DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(manifestPath.toFile())

                val dwfVersionXPath =
                    XPathFactory.newInstance()
                        .newXPath()
                        .compile(
                            "//property[@name='com.google.wear.watchface.format.version']/@value"
                        )

                val formatVersionOption =
                    dwfVersionXPath.evaluate(manifest, XPathConstants.STRING) as String?
                if (!formatVersionOption.isNullOrEmpty()) {
                    version = formatVersionOption
                }
            }

            return version.toInt()
        }
    }

    @Test
    fun test() {
        val result = App.validateRawXml(filePath)
        assertTrue(result !is ValidationResult.Failure)
        /* Each expected version must have passed. However it is possible for a versioned dwf to be naturally backwards compatible.*/
        assertTrue(
            "Versions did not match. Expected: $expectedVersions, Found: ${result.validVersions}",
            result.validVersions.containsAll(expectedVersions),
        )
    }
}
