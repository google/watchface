package com.google.wear.watchface.dfx.memory

import com.android.aapt.Resources
import com.android.aapt.Resources.XmlNode
import fr.xgouchet.axml.CompressedXmlParser
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"
private const val DWF_PROPERTY_NAME = "com.google.wear.watchface.format.version"
private const val WFF_VERSION_PROP_NAME =
    "//property[@android:name=\"$DWF_PROPERTY_NAME\"]/@android:value"

/**
 * Class that represents important properties of the AndroidManifest.xml file, for use in working
 * with watch face packages.
 */
object AndroidManifestParser {
    private fun Resources.XmlElement.getAndroidAttribute(attrName: String) =
        attributeList.firstOrNull { it.name == attrName && it.namespaceUri == "http://schemas.android.com/apk/res/android" }?.value

    @JvmStatic
    fun loadFromAab(zipFile: ZipFile): AndroidManifest {
        val baseManifestEntry = zipFile.entries().asSequence()
            .first { it.name.startsWith("base") && it.name.endsWith("AndroidManifest.xml") }
        val manifestXmlNode = zipFile.getInputStream(baseManifestEntry).use {
            XmlNode.parseFrom(it)
        }

        val usesSdkNode =
            manifestXmlNode.element.childList.firstOrNull { it.element.name == "uses-sdk" }?.element
        val minSdk = usesSdkNode?.getAndroidAttribute("minSdkVersion")?.toIntOrNull() ?: 1
        val targetSdkVersion =
            usesSdkNode?.getAndroidAttribute("targetSdkVersion")?.toIntOrNull() ?: minSdk

        val wffVersion =
            manifestXmlNode
                .element
                .childList
                .firstOrNull { it.element.name == "application" }
                ?.element
                ?.childList
                ?.firstOrNull {
                    it.element.name == "property" && it.element.getAndroidAttribute("name") == DWF_PROPERTY_NAME
                }?.element
                ?.getAndroidAttribute("value")
                ?.toIntOrNull()

            requireNotNull(wffVersion) {
                "Watch Face Manifest must have a property with name \"$DWF_PROPERTY_NAME\" that specifies the version of the Watch Face Format to be used."
            }
            return AndroidManifest(
                wffVersion = wffVersion,
                minSdkVersion = minSdk,
                targetSdkVersion = targetSdkVersion
            )
        }

    @JvmStatic
    private fun loadFromBinaryXml(inputStream: InputStream): AndroidManifest {
        // the axml loads into memory only the inputStream.available() bytes, so the manifest bytes must be fully
        // loaded into memory for parsing to work
        val buffer = inputStream.readAllBytes()
        val doc = CompressedXmlParser().parseDOM(ByteArrayInputStream(buffer))
        return loadFromDocument(doc)
    }

    @JvmStatic
    private fun loadFromPlainXml(bytes: ByteArray): AndroidManifest {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isValidating = false
        factory.isNamespaceAware = true
        factory.isIgnoringComments = false
        val docBuilder = factory.newDocumentBuilder()

        val doc = docBuilder.parse(ByteArrayInputStream(bytes))
        return loadFromDocument(doc)
    }

        @JvmStatic
        private fun loadFromDocument(doc: Document): AndroidManifest {
            val minSdk = getAttribute(doc, "//uses-sdk/@android:minSdkVersion").toIntOrNull() ?: 1
            val targetSdk =
                getAttribute(doc, "//uses-sdk/@android:targetSdkVersion").toIntOrNull() ?: minSdk
            val wffVersion = getAttribute(doc, WFF_VERSION_PROP_NAME).toInt()
            return AndroidManifest(wffVersion, minSdk, targetSdk)
        }

    @JvmStatic
    fun loadFromApk(zipFile: ZipFile): AndroidManifest {
        val manifestEntry = zipFile.stream()
            .filter { f: ZipEntry -> f.name.endsWith(ANDROID_MANIFEST_FILE_NAME) }
            .findFirst()
            .get()

        val inputStream = zipFile.getInputStream(manifestEntry)
        return loadFromBinaryXml(inputStream)
    }

    @JvmStatic
    fun loadFromAabDirectory(aabPath: Path): AndroidManifest? {
        val childrenFiles = aabPath.toFile().walk()
        val manifestFile = childrenFiles
            .filter { p -> p.toPath().endsWith(ANDROID_MANIFEST_FILE_NAME) }.firstOrNull()
        return manifestFile?.let { loadFromPlainXml(it.readBytes()) }
    }

    @JvmStatic
    fun loadFromMokkaZip(baseSplitZipStream: ZipInputStream): AndroidManifest {
        var entry = baseSplitZipStream.nextEntry
        while (entry != null) {
            if (entry.name.endsWith(ANDROID_MANIFEST_FILE_NAME)) {
                return loadFromBinaryXml(baseSplitZipStream)
            }
            entry = baseSplitZipStream.nextEntry
        }
        throw java.lang.RuntimeException("Android Manifest not found")
    }

    private fun getAttribute(doc: Document, pathSpec: String): String {
        val xPath = XPathFactory.newInstance().newXPath()
        xPath.namespaceContext = androidNamespace
        val expression = xPath.compile(pathSpec)
        return expression.evaluate(doc, XPathConstants.STRING) as String
    }
}

/**
 * Android namespace used for XPath querying.
 */
private val androidNamespace = object : NamespaceContext {
    val namespaces = mapOf("android" to "http://schemas.android.com/apk/res/android")

    override fun getNamespaceURI(prefix: String?) = namespaces[prefix]

    override fun getPrefix(uri: String?) = namespaces.entries.firstOrNull { it.value == uri }?.key

    override fun getPrefixes(uri: String?) = namespaces.entries
        .filter { it.value == uri }
        .map { it.key }
        .iterator()
}