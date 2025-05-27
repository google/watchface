package com.google.wear.watchface.dfx.memory

/**
 * Class that represents important properties of the AndroidManifest.xml file, for use in working
 * with watch face packages.
 */
data class AndroidManifest(
    val wffVersion: Int,
    val minSdkVersion: Int,
    val targetSdkVersion: Int,
)
