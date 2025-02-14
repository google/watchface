package com.google.wear.watchface.dfx.memory

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.nio.file.InvalidPathException

@RunWith(JUnit4::class)
class AndroidResourceTest {
    @Test
    fun fromPath_validResourceNoQualifiers() {
        val path = "res/raw/watchface.xml"
        val resource = AndroidResource.fromPath(path, byteArrayOf())

        assertThat(resource.resourceName).isEqualTo("watchface")
        assertThat(resource.isWatchFaceXml()).isEqualTo(true)
        assertThat(resource.isRaw()).isEqualTo(true)
        assertThat(resource.extension).isEqualTo("xml")
        assertThat(resource.versionQualifier).isEqualTo(AndroidResource.NO_VERSION_QUALIFIER);
    }

    @Test
    fun fromPath_validResourceNoExtension() {
        val path = "res/drawable/preview"
        val resource = AndroidResource.fromPath(path, byteArrayOf())

        assertThat(resource.resourceName).isEqualTo("preview")
        assertThat(resource.isWatchFaceXml()).isEqualTo(false)
        assertThat(resource.isDrawable()).isEqualTo(true)
        assertThat(resource.extension).isEqualTo("")
    }

    @Test
    fun fromPath_invalidResourcePath() {
        assertThrows(InvalidPathException::class.java) {
            val path = "resxyz/drawable/preview.png"
            AndroidResource.fromPath(path, byteArrayOf())
        }
    }

    @Test
    fun fromPath_validWatchfaceWithVersionQualifier() {
        val path = "res/raw-v34/watchface.xml"
        val resource = AndroidResource.fromPath(path, byteArrayOf())

        assertThat(resource.resourceName).isEqualTo("watchface")
        assertThat(resource.isWatchFaceXml()).isEqualTo(true)
        assertThat(resource.isRaw()).isEqualTo(true)
        assertThat(resource.extension).isEqualTo("xml")
        assertThat(resource.versionQualifier).isEqualTo(34)
    }

    @Test
    fun fromPath_validResourceWithVersionQualifier() {
        val path = "res/drawable-nodpi/image.png"
        val resource = AndroidResource.fromPath(path, byteArrayOf())

        assertThat(resource.resourceName).isEqualTo("image")
        assertThat(resource.isWatchFaceXml()).isEqualTo(false)
        assertThat(resource.isDrawable()).isEqualTo(true)
        assertThat(resource.extension).isEqualTo("png")
        assertThat(resource.versionQualifier).isEqualTo(AndroidResource.NO_VERSION_QUALIFIER);
    }

    @Test
    fun fromPath_validResourceWithMultipleQualifier() {
        val path = "res/raw-round-v34/watchface.xml"
        val resource = AndroidResource.fromPath(path, byteArrayOf())

        assertThat(resource.resourceName).isEqualTo("watchface")
        assertThat(resource.isWatchFaceXml()).isEqualTo(true)
        assertThat(resource.isRaw()).isEqualTo(true)
        assertThat(resource.extension).isEqualTo("xml")
        assertThat(resource.versionQualifier).isEqualTo(34)
    }
}