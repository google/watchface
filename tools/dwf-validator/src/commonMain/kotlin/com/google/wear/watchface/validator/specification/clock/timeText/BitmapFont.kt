package com.google.wear.watchface.validator.specification.clock.timeText

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType

/**
 * Specification constraint for a 'BitmapFont' element, Specifies a particular user-defined bitmap
 * font inside a TimeText tag which does not permit the attribute
 * * 'letterSpacing' or child elements.
 */
fun bitmapFont() =
    constraint("BitmapFont") {
        allVersions()
            .require(
                /* Attributes */
                attribute("family", nonEmpty()),
                attribute("size", float(min = 0f), "size must be a positive float"),
            )
            .allow(
                /* Attributes */
                colorAttributeType(default = "#FFFFFFFF")
            )
    }
