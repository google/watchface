package com.google.wear.watchface.validator.specification.clock.timeText

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.group.part.text.FONT_WEIGHT_OPTIONS
import com.google.wear.watchface.validator.specification.group.part.text.FONT_WIDTH_OPTIONS

/**
 * This font constraint is used by TimeText element and does not permit the attribute
 * 'letterSpacing' or child elements.
 */
fun font(): Constraint =
    constraint("Font") {
        allVersions()
            .require(
                /* Attributes */
                attribute("family", nonEmpty()),
                attribute("size", float(min = 0f), "size must be a positive float"),
            )
            .allow(
                /* Attributes */
                colorAttributeType(default = "#FFFFFFFF"),
                attribute("slant", enum("NORMAL", "ITALIC"), default = "NORMAL"),
                attribute("width", enum(FONT_WIDTH_OPTIONS), default = "NORMAL"),
                attribute("weight", enum(FONT_WEIGHT_OPTIONS), default = "NORMAL"),
            )
    }
