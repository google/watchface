/*
 * Copyright 2026 Google LLC
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

import com.android.aapt.Resources
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Utility for resolving Android resource references across various package formats
 * (binary XML/ARSC, protobuf/AAB, and plain XML directories).
 */
object AndroidResourceResolver {

    @JvmStatic
    fun parseResourceId(value: String): Int? {
        if (!value.startsWith("@")) return null
        
        var cleanValue = value.substring(1)
        val colonIdx = cleanValue.indexOf(':')
        if (colonIdx != -1) {
            cleanValue = cleanValue.substring(colonIdx + 1)
        }

        val hexIdx = cleanValue.indexOf("0x", ignoreCase = true)
        if (hexIdx != -1) {
            val hexStr = cleanValue.substring(hexIdx + 2)
            return try {
                hexStr.toLong(16).toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }

        if (cleanValue.all { it.isDigit() }) {
            return try {
                cleanValue.toLong().toInt()
            } catch (e: NumberFormatException) {
                null
            }
        }

        return null
    }

    @JvmStatic
    fun resolveResourceValue(resourceTable: ResourceTableChunk, resourceId: Int): String {
        return resolveResourceValue(resourceTable, resourceId, emptySet())
    }

    private fun resolveResourceValue(
        resourceTable: ResourceTableChunk,
        resourceId: Int,
        visited: Set<Int>
    ): String {
        if (visited.contains(resourceId)) {
            throw java.lang.RuntimeException("Cyclic resource reference detected for ID $resourceId")
        }
        val packageId = (resourceId ushr 24) and 0xFF
        val typeId = (resourceId ushr 16) and 0xFF
        val entryId = resourceId and 0xFFFF

        val pkg = resourceTable.packages.firstOrNull { it.id == packageId }
            ?: throw java.lang.RuntimeException("Package not found for ID $packageId")

        val typeChunks = pkg.typeChunks.filter { it.id == typeId }
        if (typeChunks.isEmpty()) {
            throw java.lang.RuntimeException("Type not found for ID $typeId")
        }

        val newVisited = visited + resourceId

        for (typeChunk in typeChunks) {
            val entry = typeChunk.entries[entryId]
            if (entry != null) {
                val value = entry.value()
                if (value != null) {
                    if (value.type() == BinaryResourceValue.Type.INT_DEC ||
                        value.type() == BinaryResourceValue.Type.INT_HEX) {
                        return value.data().toString()
                    } else if (value.type() == BinaryResourceValue.Type.STRING) {
                        return resourceTable.stringPool.getString(value.data())
                    } else if (value.type() == BinaryResourceValue.Type.REFERENCE ||
                               value.type() == BinaryResourceValue.Type.DYNAMIC_REFERENCE) {
                        return resolveResourceValue(resourceTable, value.data(), newVisited)
                    } else {
                        throw java.lang.RuntimeException("Unsupported resource value type: ${value.type()}")
                    }
                }
            }
        }
        throw java.lang.RuntimeException("Entry not found for ID $entryId")
    }

    @JvmStatic
    fun resolveProtoResource(resourceTable: Resources.ResourceTable, ref: String): String {
        if (!ref.startsWith("@")) return ref
        val parts = ref.substring(1).split("/")
        if (parts.size != 2) return ref
        val resType = parts[0]
        val resName = parts[1]

        for (pkg in resourceTable.packageList) {
            val type = pkg.typeList.firstOrNull { it.name == resType } ?: continue
            val entry = type.entryList.firstOrNull { it.name == resName } ?: continue
            val configValue = entry.configValueList.firstOrNull() ?: continue
            val value = configValue.value
            if (value.hasItem()) {
                val item = value.item
                if (item.hasPrim()) {
                    val prim = item.prim
                    return when {
                        prim.hasIntDecimalValue() -> prim.intDecimalValue.toString()
                        prim.hasIntHexadecimalValue() -> prim.intHexadecimalValue.toString()
                        prim.hasBooleanValue() -> prim.booleanValue.toString()
                        else -> ref
                    }
                } else if (item.hasStr()) {
                    return item.str.value
                }
            }
        }
        return ref
    }
    @JvmStatic
    fun resolveProtoAttribute(resourceTable: Resources.ResourceTable?, value: String): String {
        if (resourceTable == null) return value
        return resolveProtoResource(resourceTable, value)
    }
    @JvmStatic
    fun resolvePlainXmlResource(resDir: Path, ref: String): String {
        if (!ref.startsWith("@")) return ref
        val parts = ref.substring(1).split("/")
        if (parts.size != 2) return ref
        val resType = parts[0]
        val resName = parts[1]

        val valuesDir = resDir.resolve("values")
        if (!java.nio.file.Files.exists(valuesDir)) return ref

        val files = valuesDir.toFile().listFiles { f -> f.extension == "xml" } ?: return ref
        for (file in files) {
            try {
                val factory = DocumentBuilderFactory.newInstance()
                val builder = factory.newDocumentBuilder()
                val doc = builder.parse(file)
                val nodes = doc.getElementsByTagName(resType)
                for (i in 0 until nodes.length) {
                    val node = nodes.item(i) as org.w3c.dom.Element
                    if (node.getAttribute("name") == resName) {
                        return node.textContent.trim()
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        return ref
    }

    @JvmStatic
    fun resolveAttributeValue(
        value: String,
        resourceTable: ResourceTableChunk?,
        resDir: Path?
    ): String? {
        if (value.isEmpty()) return null
        if (!value.startsWith("@")) return value

        if (resourceTable != null) {
            val resourceId = parseResourceId(value) ?: return value
            return try {
                resolveResourceValue(resourceTable, resourceId)
            } catch (e: Exception) {
                value
            }
        }

        if (resDir != null) {
            return try {
                resolvePlainXmlResource(resDir, value)
            } catch (e: Exception) {
                value
            }
        }

        return value
    }
}
