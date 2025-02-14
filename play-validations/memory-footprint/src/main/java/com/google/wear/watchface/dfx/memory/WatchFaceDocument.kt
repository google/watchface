package com.google.wear.watchface.dfx.memory

import com.google.wear.watchface.dfx.memory.AndroidResource.Companion.NO_VERSION_QUALIFIER
import org.w3c.dom.Document

data class WatchFaceDocument(val document: Document, private val versionQualifier: Int) {
    companion object {
        // If no WFF version has been attached to this document, namely, it came from an unqualified
        // directory, e.g. /res/raw, not /res/raw-v34 etc.
        const val NO_WFF_VERSION: Int = -1

        // WFFv1 corresponds to API level 33, v2 to 34, etc.
        const val WFF_VERSION_OFFSET: Int = 32
    }

    val wffVersion: Int
        get() = if (versionQualifier == NO_VERSION_QUALIFIER) {
            NO_WFF_VERSION
        } else {
            versionQualifier - WFF_VERSION_OFFSET
        }
}