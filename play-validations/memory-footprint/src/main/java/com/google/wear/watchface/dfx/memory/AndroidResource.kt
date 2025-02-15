/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.wear.watchface.dfx.memory

import com.google.common.annotations.VisibleForTesting
import java.nio.file.InvalidPathException
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
    @VisibleForTesting val extension: String,
    // Path in the package. This is the obfuscated path to the actual data, where obfuscation has
    // been used, for example "res/raw/watchface.xml" may point to something like "res/li.xml".
    val filePath: Path,
    // The resource data itself.
    val data: ByteArray,
    val versionQualifier: Int = NO_VERSION_QUALIFIER,
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
            Pattern.compile(".*res/([^-/]+)(|.*-v(\\d+)|-.*)/([^.]+)[.]?(.*|)$")
        private const val VALID_RESOURCE_GROUPS: Int = 5
        const val NO_VERSION_QUALIFIER: Int = -1

        @JvmStatic
        fun fromPath(filePath: Path, data: ByteArray): AndroidResource {
            val pathWithFwdSlashes = filePath.toString().replace('\\', '/')
            val m = VALID_RESOURCE_PATH.matcher(pathWithFwdSlashes)

            // Extracts both scenarios without a version resource qualifier, e.g. /res/raw and those
            // with a version resource qualifier, e.g. /res/raw-v34 or /res/raw-round-v34. The
            // version qualifier is always last in the list of qualifiers.
            if (m.matches() && m.groupCount() == VALID_RESOURCE_GROUPS) {
                val resType = m.group(1)
                var qualifierVersion = NO_VERSION_QUALIFIER
                if (m.group(2) != null && m.group(2).isNotEmpty() &&
                    m.group(3) != null && m.group(3).isNotEmpty()
                ) {
                    qualifierVersion = m.group(3).toInt()
                }
                val resName = m.group(4)
                val ext = m.group(5)
                return AndroidResource(resType, resName, ext, filePath, data, qualifierVersion)
            }
            throw InvalidPathException(
                filePath.toString(),
                "Not a valid resource file"
            )
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
