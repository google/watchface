package com.google.wear.watchface.dfx.memory

import com.android.aapt.Resources
import com.android.aapt.Resources.XmlNode
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk
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
class AndroidManifest private constructor(
    val wffVersion: Int,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
) {
    companion object {
        private fun Resources.XmlElement.getAndroidAttribute(attrName: String) =
            attributeList.firstOrNull { it.name == attrName && it.namespaceUri == "http://schemas.android.com/apk/res/android" }?.value

        private fun Resources.XmlElement.findChild(name: String): Resources.XmlElement? =
            childList.firstOrNull { it.element.name == name }?.element

        private fun Resources.XmlElement.findProperty(propertyName: String): Resources.XmlElement? =
            childList.asSequence()
                .filter { it.element.name == "property" }
                .map { it.element }
                .firstOrNull { it.getAndroidAttribute("name") == propertyName }

        @JvmStatic
        fun loadFromAab(zipFile: ZipFile): AndroidManifest {
            val baseManifestEntry = zipFile.entries().asSequence()
                .first { it.name.startsWith("base") && it.name.endsWith("AndroidManifest.xml") }
            val manifestXmlNode = zipFile.getInputStream(baseManifestEntry).use {
                XmlNode.parseFrom(it)
            }

            // Load resources.pb if present
            val resourcesEntry = zipFile.entries().asSequence()
                .firstOrNull { it.name.endsWith("resources.pb") }
            val resourceTable: Resources.ResourceTable? = resourcesEntry?.let { entry ->
                zipFile.getInputStream(entry).use { stream ->
                    Resources.ResourceTable.parseFrom(stream)
                }
            }

            val rootElement = manifestXmlNode.element
            val usesSdkNode = rootElement.findChild("uses-sdk")

            val minSdkStr = usesSdkNode?.getAndroidAttribute("minSdkVersion") ?: "1"
            val minSdk = AndroidResourceResolver.resolveProtoAttribute(resourceTable, minSdkStr).toIntOrNull() ?: 1

            val targetSdkStr = usesSdkNode?.getAndroidAttribute("targetSdkVersion") ?: minSdkStr
            val targetSdkVersion = AndroidResourceResolver.resolveProtoAttribute(resourceTable, targetSdkStr).toIntOrNull() ?: minSdk

            val wffVersionStr = rootElement.findChild("application")
                ?.findProperty(DWF_PROPERTY_NAME)
                ?.getAndroidAttribute("value")
                ?: throw java.lang.RuntimeException("Watch Face Manifest must specify format version")

            val resolvedWffVersion = AndroidResourceResolver.resolveProtoAttribute(resourceTable, wffVersionStr)
            val wffVersion = resolvedWffVersion.toIntOrNull()
                ?: throw NumberFormatException("Failed to parse wffVersion: '$resolvedWffVersion'")

            return AndroidManifest(
                wffVersion = wffVersion,
                minSdkVersion = minSdk,
                targetSdkVersion = targetSdkVersion
            )
        }

        @JvmStatic
        private fun loadFromBinaryXml(
            inputStream: InputStream,
            resourceTable: ResourceTableChunk? = null,
        ): AndroidManifest {
            // the axml loads into memory only the inputStream.available() bytes, so the manifest bytes must be fully
            // loaded into memory for parsing to work
            val buffer = inputStream.readBytes()
            val doc = CompressedXmlParser().parseDOM(ByteArrayInputStream(buffer))
            return loadFromDocument(doc, resourceTable)
        }

        @JvmStatic
        private fun loadFromPlainXml(bytes: ByteArray, resDir: Path? = null): AndroidManifest {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isValidating = false
            factory.isNamespaceAware = true
            factory.isIgnoringComments = false
            val docBuilder = factory.newDocumentBuilder()

            val doc = docBuilder.parse(ByteArrayInputStream(bytes))
            return loadFromDocument(doc, resDir = resDir)
        }

        private fun getResolvedAttribute(
            doc: Document,
            pathSpec: String,
            resourceTable: ResourceTableChunk?,
            resDir: Path?
        ): String? {
            val rawValue = getAttribute(doc, pathSpec)
            return AndroidResourceResolver.resolveAttributeValue(rawValue, resourceTable, resDir)
        }

        @JvmStatic
        internal fun loadFromDocument(
            doc: Document,
            resourceTable: ResourceTableChunk? = null,
            resDir: Path? = null,
        ): AndroidManifest {
            val minSdk = getResolvedAttribute(doc, "//uses-sdk/@android:minSdkVersion", resourceTable, resDir)
                ?.toIntOrNull() ?: 1
            val targetSdk = getResolvedAttribute(doc, "//uses-sdk/@android:targetSdkVersion", resourceTable, resDir)
                ?.toIntOrNull() ?: minSdk
            val resolvedWffVersion = getResolvedAttribute(doc, WFF_VERSION_PROP_NAME, resourceTable, resDir)
            val wffVersion = resolvedWffVersion?.toIntOrNull()
                ?: throw NumberFormatException("Failed to parse wffVersion: '$resolvedWffVersion'")
            return AndroidManifest(wffVersion, minSdk, targetSdk)
        }

        @JvmStatic
        fun loadFromApk(zipFile: ZipFile): AndroidManifest {
            val manifestEntry = zipFile.stream()
                .filter { f: ZipEntry -> f.name.endsWith(ANDROID_MANIFEST_FILE_NAME) }
                .findFirst()
                .get()

            val inputStream = zipFile.getInputStream(manifestEntry)
            val resourcesEntry = zipFile.getEntry("resources.arsc")
            val resourceTable = resourcesEntry?.let { entry ->
                zipFile.getInputStream(entry).use { stream ->
                    AndroidResourceLoader.loadResourceTable(stream)
                }
            }
            return loadFromBinaryXml(inputStream, resourceTable)
        }

        @JvmStatic
        fun loadFromAabDirectory(aabPath: Path): AndroidManifest? {
            val childrenFiles = aabPath.toFile().walk()
            val manifestFile =
                childrenFiles.firstOrNull { p -> p.toPath().endsWith(ANDROID_MANIFEST_FILE_NAME) }
                    ?: return null
            val resDir = manifestFile.toPath().parent.parent.resolve("res")
            return loadFromPlainXml(manifestFile.readBytes(), resDir)
        }

        @JvmStatic
        fun loadFromMokkaZip(baseSplitZipStream: ZipInputStream): AndroidManifest {
            var manifestBytes: ByteArray? = null
            var resourcesBytes: ByteArray? = null
            var entry = baseSplitZipStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(ANDROID_MANIFEST_FILE_NAME)) {
                    manifestBytes = baseSplitZipStream.readBytes()
                } else if (entry.name.endsWith("resources.arsc")) {
                    resourcesBytes = baseSplitZipStream.readBytes()
                }
                entry = baseSplitZipStream.nextEntry
            }
            if (manifestBytes == null) {
                throw java.lang.RuntimeException("Android Manifest not found")
            }
            val resourceTable = resourcesBytes?.let { bytes ->
                AndroidResourceLoader.loadResourceTable(ByteArrayInputStream(bytes))
            }
            return loadFromBinaryXml(ByteArrayInputStream(manifestBytes), resourceTable)
        }

        private fun getAttribute(doc: Document, pathSpec: String): String {
            val xPath = XPathFactory.newInstance().newXPath()
            xPath.namespaceContext = androidNamespace
            val expression = xPath.compile(pathSpec)
            return expression.evaluate(doc, XPathConstants.STRING) as String
        }
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