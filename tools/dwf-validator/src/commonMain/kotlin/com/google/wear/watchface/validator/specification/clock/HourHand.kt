package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.tintColor

fun hourHand() =
    constraint("HourHand") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes,
                attribute("resource"),
            )
            .allow(
                /* Attributes */
                *pivots,
                alpha,
                tintColor,

                /* Child Elements */
                childElement("Variant", ::variant),
            )
    }
