package com.google.wear.watchface.dfx.memory

import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/** Represents a resource, from an AAB or APK. */
class AndroidResource(
    // Resource type, for example "raw", "asset", "drawable" etc.
    private val resourceType: String,
    // Resource name, for example "watchface" for res/raw/watchface.xml.
    val resourceName: String,
    // File extension of the resource, for example "xml" for res/raw/watchface.xml
    private val extension: String,
    // Path in the package. This is the obfuscated path to the actual data, where obfuscation has
    // been used, for example "res/raw/watchface.xml" may point to something like "res/li.xml".
    val filePath: Path,
    // The resource data itself.
    val data: ByteArray
) {
    // TODO: This should be improved to parse res/xml/watch_face_info.xml where present, so as not
    // to assume all XML files in the res/raw directory are watch face XML files.
    fun isWatchFaceXml(): Boolean = extension == "xml" && resourceType == "raw"

    fun isDrawable(): Boolean = resourceType == "drawable"

    fun isFont(): Boolean = resourceType == "font"

    fun isAsset(): Boolean = resourceType == "asset"

    fun isRaw(): Boolean = resourceType == "raw"

    companion object {
        private val VALID_RESOURCE_PATH: Pattern =
            Pattern.compile(".*res/([^-/]+).*/([^.]+)(\\.|)(.*|)$")
        private const val VALID_RESOURCE_GROUPS: Int = 4

        @JvmStatic
        fun fromPath(filePath: Path, data: ByteArray): AndroidResource {
            val pathWithFwdSlashes = filePath.toString().replace('\\', '/')
            val matcher = VALID_RESOURCE_PATH.matcher(pathWithFwdSlashes)
            if (matcher.matches() && matcher.groupCount() == VALID_RESOURCE_GROUPS) {
                val resType = matcher.group(1)
                val resName = matcher.group(2)
                val ext = matcher.group(4)
                return AndroidResource(resType, resName, ext, filePath, data)
            }
            throw RuntimeException("Not a valid resource file: $pathWithFwdSlashes")
        }

        @JvmStatic
        fun fromPath(filePath: String, data: ByteArray): AndroidResource =
            fromPath(Paths.get(filePath), data)

        @JvmStatic
        fun isValidResourcePath(filePath: Path): Boolean {
            val pathWithFwdSlashes = filePath.toString().replace('\\', '/')
            val matcher = VALID_RESOURCE_PATH.matcher(pathWithFwdSlashes)
            return matcher.matches() && matcher.groupCount() == VALID_RESOURCE_GROUPS
        }

        @JvmStatic
        fun isValidResourcePath(filePath: String): Boolean =
            isValidResourcePath(Paths.get(filePath))
    }
}