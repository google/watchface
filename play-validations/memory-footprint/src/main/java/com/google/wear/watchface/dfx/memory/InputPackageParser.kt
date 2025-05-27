package com.google.wear.watchface.dfx.memory

import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.streams.asSequence

object InputPackageParser {
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
                AndroidManifestParser.loadFromAabDirectory(rootPath)

            override fun close() {}
        }
    }

    /** Creates an input package from a declarative watch face APK. */
    fun openFromApkFile(apkPath: String): InputPackage {
        val zipFile = ZipFile(apkPath)
        return object : InputPackage {
            override fun getWatchFaceFiles() =
                AndroidResourceLoader.streamFromApkFile(zipFile)

            override fun getManifest() = AndroidManifestParser.loadFromApk(zipFile)

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

            override fun getManifest() = AndroidManifestParser.loadFromAab(zipFile)

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
                    return AndroidManifestParser.loadFromMokkaZip(baseSplitApkZip)
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