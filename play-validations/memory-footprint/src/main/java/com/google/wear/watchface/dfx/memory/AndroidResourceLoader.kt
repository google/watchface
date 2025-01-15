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

import com.google.common.io.Files
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence

/**
 * Represents all the resources of interest in the Android package.
 *
 * <p>Where obfuscation has been applied, the mapping is derived, for the creation of the
 * AndroidResource objects. For example, for a logical resource res/raw/watchface.xml, an APK may in
 * fact store this as res/aB.xml. The resources.arsc file contains this mapping, and the
 * AndroidResourceLoader provides a stream of resources with their logical types, names and data.
 *
 * <p>Note that more than one AndroidResource object can exist for the given dimensions. For
 * example, if there is a drawable and a drawable-fr folder, then there may be multiple
 * AndroidResource entries for drawables with the same type, name and extension. The configuration
 * detail, e.g. "fr" or "default", is not currently exposed in the AndroidResource objects as it
 * isn't used.
 */
object AndroidResourceLoader {
    private val RESOURCE_TYPES = setOf("raw", "xml", "drawable", "font", "asset")
    private const val RESOURCES_FILE_NAME = "resources.arsc"

    fun streamFromAabDirectory(aabPath: Path): Sequence<AndroidResource> {
        val relativePathOffset = aabPath.toString().length + 1
        return aabPath.toFile().walk().mapNotNull { file ->
            if (AndroidResource.isValidResourcePath(file.toPath())) {
                val resourceRelativePath =  Paths.get(file.path.substring(relativePathOffset))
                AndroidResource.fromPath(resourceRelativePath, file.readBytes())
            } else if (file.toPath().endsWith("manifest/AndroidManifest.xml")) {
                val resourceRelativePath =  Paths.get(file.path.substring(relativePathOffset))
                AndroidResource(
                    resourceType = "xml",
                    resourceName = "AndroidManifest.xml",
                    extension = "xml",
                    filePath = resourceRelativePath,
                    data = file.readBytes()
                )
            } else {
                null
            }
        }
    }

    fun streamFromAabFile(aabZipFile: ZipFile): Sequence<AndroidResource> {
        return aabZipFile.stream().asSequence().mapNotNull { zipEntry ->
            val zipEntryPath = Paths.get(zipEntry.name)
            if (AndroidResource.isValidResourcePath(zipEntryPath)) {
                AndroidResource.fromPath(
                    zipEntryPath,
                    aabZipFile.getInputStream(zipEntry).use { it.readBytes() }
                )
            } else if (zipEntry.name.endsWith("manifest/AndroidManifest.xml")) {
                AndroidResource(
                    "xml",
                    "AndroidManifest",
                    "xml",
                    Paths.get(zipEntry.name),
                    aabZipFile.getInputStream(zipEntry).use { it.readBytes() }
                )
            } else {
                null
            }
        }
    }

    fun streamFromMokkaZip(baseSplitZipStream: ZipInputStream): Sequence<AndroidResource> {
        return sequence {
                var next = baseSplitZipStream.nextEntry
                while (next != null) {
                    yield(next)
                    next = baseSplitZipStream.nextEntry
                }
            }
            .filter { AndroidResource.isValidResourcePath(it.name) }
            .map { AndroidResource.fromPath(it.name, baseSplitZipStream.readBytes()) }
    }

    fun streamFromApkFile(apkFile: ZipFile): Sequence<AndroidResource> {
        val arscEntry = ZipEntry(RESOURCES_FILE_NAME)

        val resources =
            apkFile.getInputStream(arscEntry).use { BinaryResourceFile.fromInputStream(it) }

        val chunks = resources.chunks
        if (chunks.isEmpty()) {
            throw IOException("no chunks")
        }
        if (chunks[0] !is ResourceTableChunk) {
            throw IOException("no res table chunk")
        }
        val table = chunks[0] as ResourceTableChunk
        val stringPool = table.stringPool

        val typeChunks = table.packages.asSequence().flatMap { it.typeChunks }

        return typeChunks
            .flatMap { it.entries.values.asSequence() }
            .filter { RESOURCE_TYPES.contains(it.typeName()) }
            .filter { it.value().type() == BinaryResourceValue.Type.STRING }
            .map { entry ->
                val path = stringPool.getString(entry.value().data())
                val data = apkFile.getInputStream(ZipEntry(path)).readBytes()
                AndroidResource(
                    entry.parent().typeName,
                    entry.key(),
                    Files.getFileExtension(path),
                    Paths.get(path),
                    data
                )
            }
    }

    @JvmStatic
    fun readAllBytes(steam: InputStream) = steam.readBytes()
}
