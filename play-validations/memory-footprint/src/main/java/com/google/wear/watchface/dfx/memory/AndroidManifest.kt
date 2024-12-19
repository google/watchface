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
import com.google.common.primitives.Shorts
import com.google.devrel.gmscore.tools.apk.arsc.Chunk
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
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

class AndyManifest private constructor(val wffVersion: Int, val minSdkVersion: Int, val targetSdkVersion: Int) {
    companion object {
        @JvmStatic
        fun loadFromAab(zipFile: ZipFile): AndyManifest {
            val manifestPath =
                ZipPath.create(BundleModuleName.BASE_MODULE_NAME.name)
                    .resolve(BundleModule.SpecialModuleEntry.ANDROID_MANIFEST.path)
            val manifestProto =
                XmlProtoNode(
                    DumpManagerUtils.extractAndParse(
                        zipFile, manifestPath
                    ) { input: InputStream? -> XmlNode.parseFrom(input) })

            val doc = XmlProtoToXmlConverter.convert(manifestProto)

            try {
                val minSdk = getAttribute(doc, manifestProto,"//uses-sdk/@android:minSdkVersion").toIntOrNull() ?: 1
                val targetSdk = getAttribute(doc, manifestProto,"//uses-sdk/@android:targetSdkVersion").toIntOrNull() ?: minSdk
                val wffVersion = getAttribute(doc, manifestProto,"//property/@android:value").toInt()
                return AndyManifest(wffVersion, minSdk, targetSdk)
            } catch (e: XPathExpressionException) {
                throw RuntimeException(e)
            }
        }

        @JvmStatic
        private fun loadFromBinaryXml(inputStream: InputStream): AndyManifest {
            val manifestBytes = inputStream.readAllBytes()
            val code = Shorts.fromBytes(manifestBytes[1], manifestBytes[0])
            val isBinaryXml = code == Chunk.Type.XML.code()
            val xmlBytes = BinaryXmlParser.decodeXml(manifestBytes)
            return loadFromPlainXml(xmlBytes)
        }

        @JvmStatic
        private fun loadFromPlainXml(bytes: ByteArray): AndyManifest {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isValidating = false
            factory.isNamespaceAware = true
            factory.isIgnoringComments = false
            val docBuilder = factory.newDocumentBuilder()

            val doc = docBuilder.parse(ByteArrayInputStream(bytes))
            val minSdk = getAttribute(doc, "//uses-sdk/@android:minSdkVersion").toIntOrNull() ?: 1
            val targetSdk = getAttribute(doc, "//uses-sdk/@android:targetSdkVersion").toIntOrNull() ?: minSdk
            val wffVersion = getAttribute(doc, "//property/@android:value").toInt()
            return AndyManifest(wffVersion, minSdk, targetSdk)
        }

        @JvmStatic
        fun loadFromApk(zipFile: ZipFile): AndyManifest {
            val manifestEntry = zipFile.stream()
                .filter { f: ZipEntry -> f.name.endsWith("AndroidManifest.xml") }
                .findFirst()
                .get()

            val inputStream = zipFile.getInputStream(manifestEntry)
            return loadFromBinaryXml(inputStream)
        }

        @JvmStatic
        fun loadFromAabDirectory(aabPath: Path): AndyManifest {
            val childrenFilesStream = Files.walk(aabPath)
            val manifestPath = childrenFilesStream
                .filter { p: Path -> p.endsWith("AndroidManifest.xml") }.findFirst().get()
            val inputStream = Files.newInputStream(manifestPath)
            return loadFromPlainXml(inputStream.readAllBytes())
        }

        @JvmStatic
        fun loadFromMokkaZip(baseSplitZipStream: ZipInputStream): AndyManifest {
            var entry = baseSplitZipStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("AndroidManifest.xml")) {
                    return loadFromBinaryXml(baseSplitZipStream)
                }
                entry = baseSplitZipStream.nextEntry
            }
            throw java.lang.RuntimeException("Android Manifest not found")
        }

        private fun extractMinSdkVersion(doc: Document, manifestProto: XmlProtoNode) = getAttribute(
            doc, manifestProto, "//uses-sdk/@android:minSdkVersion"
        ).toInt()

        private fun extractTargetSdkVersion(doc: Document, manifestProto: XmlProtoNode) =
            getAttribute(
                doc, manifestProto, "//uses-sdk/@android:targetSdkVersion"
            ).toInt()

        private fun extractWffVersion(doc: Document, manifestProto: XmlProtoNode) = getAttribute(
            doc, manifestProto, "//property/@android:value"
        ).toInt()

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

val androidNamespace = object: NamespaceContext {
    val namespaces = mapOf("android" to "http://schemas.android.com/apk/res/android")

    override fun getNamespaceURI(prefix: String?) = namespaces[prefix]

    override fun getPrefix(uri: String?) = namespaces.entries.firstOrNull { it.value == uri }?.key

    override fun getPrefixes(uri: String?) = namespaces.entries
        .filter { it.value == uri }
        .map { it.key }
        .iterator()
}