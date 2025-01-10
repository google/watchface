package com.google.wear.watchface.dfx.memory

import com.android.aapt.Resources.XmlNode
import com.android.tools.apk.analyzer.BinaryXmlParser
import com.android.tools.build.bundletool.commands.DumpManagerUtils
import com.android.tools.build.bundletool.model.BundleModule
import com.android.tools.build.bundletool.model.BundleModuleName
import com.android.tools.build.bundletool.model.ZipPath
import com.android.tools.build.bundletool.model.utils.xmlproto.XmlProtoNode
import com.android.tools.build.bundletool.xml.XPathResolver
import com.android.tools.build.bundletool.xml.XmlNamespaceContext
import com.android.tools.build.bundletool.xml.XmlProtoToXmlConverter
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.walk

private const val ANDROID_MANIFEST_FILE_NAME = "AndroidManifest.xml"

/**
 * Class that represents important properties of the AndroidManifest.xml file, for use in working
 * with watch face packages.
 */
class AndroidManifest private constructor(
    val wffVersion: Int,
    val minSdkVersion: Int,
    val targetSdkVersion: Int
) {
    companion object {
        @JvmStatic
        fun loadFromAab(zipFile: ZipFile): AndroidManifest {
            val manifestPath =
                ZipPath.create(BundleModuleName.BASE_MODULE_NAME.name)
                    .resolve(BundleModule.SpecialModuleEntry.ANDROID_MANIFEST.path)
            val manifestProto =
                XmlProtoNode(
                    DumpManagerUtils.extractAndParse(
                        zipFile, manifestPath
                    ) { input: InputStream? -> XmlNode.parseFrom(input) })

            val doc = XmlProtoToXmlConverter.convert(manifestProto)

            val minSdk = getAttribute(
                doc,
                manifestProto,
                "//uses-sdk/@android:minSdkVersion"
            ).toIntOrNull() ?: 1
            val targetSdk = getAttribute(
                doc,
                manifestProto,
                "//uses-sdk/@android:targetSdkVersion"
            ).toIntOrNull() ?: minSdk
            val wffVersion =
                getAttribute(doc, manifestProto, "//property/@android:value").toInt()
            return AndroidManifest(wffVersion, minSdk, targetSdk)

        }

        @JvmStatic
        private fun loadFromBinaryXml(inputStream: InputStream): AndroidManifest {
            val manifestBytes = inputStream.readAllBytes()
            val xmlBytes = BinaryXmlParser.decodeXml(manifestBytes)
            return loadFromPlainXml(xmlBytes)
        }

        @JvmStatic
        private fun loadFromPlainXml(bytes: ByteArray): AndroidManifest {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isValidating = false
            factory.isNamespaceAware = true
            factory.isIgnoringComments = false
            val docBuilder = factory.newDocumentBuilder()

            val doc = docBuilder.parse(ByteArrayInputStream(bytes))
            val minSdk = getAttribute(doc, "//uses-sdk/@android:minSdkVersion").toIntOrNull() ?: 1
            val targetSdk =
                getAttribute(doc, "//uses-sdk/@android:targetSdkVersion").toIntOrNull() ?: minSdk
            val wffVersion = getAttribute(doc, "//property/@android:value").toInt()
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

        @OptIn(ExperimentalPathApi::class)
        @JvmStatic
        fun loadFromAabDirectory(aabPath: Path): AndroidManifest {
            val childrenFiles = aabPath.walk()
            val manifestPath = childrenFiles
                .filter { p: Path -> p.endsWith(ANDROID_MANIFEST_FILE_NAME) }.first()
            val inputStream = Files.newInputStream(manifestPath)
            return loadFromPlainXml(inputStream.readAllBytes())
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
            val xPathResult = XPathResolver.resolve(doc, expression)
            return xPathResult.toString()
        }

        private fun getAttribute(
            doc: Document,
            manifestProto: XmlProtoNode,
            pathSpec: String
        ): String {
            val xPath = XPathFactory.newInstance().newXPath()
            xPath.namespaceContext = XmlNamespaceContext(manifestProto)
            val compiledXPathExpression = xPath.compile(pathSpec)
            val xPathResult = XPathResolver.resolve(doc, compiledXPathExpression)
            return xPathResult.toString()
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