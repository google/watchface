package com.google.wear.watchface.validator.specification

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.specification.watchFace.watchFace

val WFF_SPECIFICATION: WatchFaceSpecification =
    WatchFaceSpecification(watchFace(), ALL_WFF_VERSIONS)
