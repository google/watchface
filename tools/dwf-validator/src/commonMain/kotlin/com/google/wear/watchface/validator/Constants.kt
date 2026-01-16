package com.google.wear.watchface.validator

internal const val MIN_WFF_VERSION = 1
internal const val MAX_WFF_VERSION = 4
internal val ALL_WFF_VERSIONS = (MIN_WFF_VERSION..MAX_WFF_VERSION).toSet()
internal const val DEFAULT_CONDITION_MESSAGE = "Condition check failed."
