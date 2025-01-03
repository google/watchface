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

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence

/**
 * Exposes the files of a watch face package as a stream of watch face package files. It is backed
 * by a closeable resource, reading the watch face package zip, so it must be closed after consuming
 * the package.
 */
interface InputPackage : AutoCloseable {
    /**
     * Returns a stream of resource representations form the watch face package. The stream must not
     * be consumed more than once. The InputPackage must be closed only after consuming the stream
     * of files.
     */
    fun getWatchFaceFiles(): Sequence<AndroidResource>

    fun getManifest(): AndroidManifest

    /** Close the backing watch face package resource. */
    override fun close()

    companion object {
        /** Creates an input package from a declarative watch face package. */
        @JvmStatic
        fun open(packagePath: String): InputPackage {
            val packageFile = File(packagePath)
            if (!packageFile.exists()) {
                throw IllegalArgumentException("Package path $packagePath does not exist")
            }

            return when {
                packageFile.isDirectory -> openFromAabDirectory(packageFile)
                packagePath.endsWith("zip") -> openFromMokkaZip(packagePath)
                packagePath.endsWith("aab") -> openFromAabFile(packagePath)
                packagePath.endsWith("apk") -> openFromApkFile(packagePath)
                else -> throw IllegalArgumentException("Incorrect file type")
            }
        }

        /**
         * Creates an input package from a directory containing the structure of a Declarative Watch
         * Face AAB. Each file is relative to the base module of the directory.
         */
        fun openFromAabDirectory(aabDirectory: File): InputPackage {
            val rootPath = aabDirectory.toPath()
            return object : InputPackage {
                override fun getWatchFaceFiles() =
                    AndroidResourceLoader.streamFromAabDirectory(rootPath)

                override fun getManifest() =
                    AndroidManifest.loadFromAabDirectory(rootPath)

                override fun close() {}
            }
        }

        /** Creates an input package from a declarative watch face APK. */
        fun openFromApkFile(apkPath: String): InputPackage {
            val zipFile = ZipFile(apkPath)
            return object : InputPackage {
                override fun getWatchFaceFiles() =
                    AndroidResourceLoader.streamFromApkFile(zipFile)

                override fun getManifest() = AndroidManifest.loadFromApk(zipFile)

                override fun close() {
                    zipFile.close()
                }
            }
        }

        /**
         * Creates an input package from a declarative watch face AAB. Each file is relative to the
         * base module of the app bundle. Every other module will be ignored.
         */
        fun openFromAabFile(aabPath: String): InputPackage {
            val zipFile = ZipFile(aabPath)
            return object : InputPackage {
                override fun getWatchFaceFiles(): Sequence<AndroidResource> {
                    return AndroidResourceLoader.streamFromAabFile(zipFile)
                }

                override fun getManifest() = AndroidManifest.loadFromAab(zipFile)

                override fun close() {
                    zipFile.close()
                }
            }
        }

        /**
         * Creates an input package from a zip file containing the base split apk, as produced by
         * mokka.
         */
        fun openFromMokkaZip(zipPath: String): InputPackage {
            val mokkaZip = ZipFile(zipPath)

            try {
                val baseSplitRegex = """.*base[-_]split.*""".toRegex()
                val baseSplitApk =
                    mokkaZip
                        .stream()
                        .asSequence()
                        .filter { entry -> entry.name.matches(baseSplitRegex) }
                        .firstOrNull()
                if (baseSplitApk == null) {
                    throw InvalidTestRunException("Zip file does not contain a base split apk")
                }

                return object : InputPackage {
                    override fun getWatchFaceFiles(): Sequence<AndroidResource> {
                        val baseSplitApkZip = ZipInputStream(mokkaZip.getInputStream(baseSplitApk))
                        return AndroidResourceLoader.streamFromMokkaZip(baseSplitApkZip)
                    }

                    override fun getManifest(): AndroidManifest {
                        val baseSplitApkZip = ZipInputStream(mokkaZip.getInputStream(baseSplitApk))
                        return AndroidManifest.loadFromMokkaZip(baseSplitApkZip)
                    }

                    override fun close() {
                        mokkaZip.close()
                    }
                }
            } catch (e: Exception) {
                mokkaZip.close()
                throw e
            }
        }
    }
}
