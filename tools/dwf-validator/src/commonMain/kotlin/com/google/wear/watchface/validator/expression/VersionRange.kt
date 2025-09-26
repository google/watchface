package com.google.wear.watchface.validator.expression

/**
 * A data class representing the supported versions of a function/source.
 *
 * @property minVersion The version in which the function was introduced.
 * @property maxVersion The version after which the function is deprecated. Defaults to the current
 *   version.
 */
data class VersionRange(val minVersion: Int, val maxVersion: Int = Int.MAX_VALUE)
