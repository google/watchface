package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.localization
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes

/** Specification constraint for the `AnalogClock` element. */
fun analogClock() =
    constraint("AnalogClock") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes
            )
            .allow(
                /* Attributes */
                *clockTypeAttributes,

                /* Child Elements */
                childElement("HourHand", ::hourHand, maxOccurs = 2),
                childElement("MinuteHand", ::minuteHand, maxOccurs = 2),
                childElement("SecondHand", ::secondHand, maxOccurs = 2),
                childElement("Localization", ::localization, maxOccurs = 1),
                childElement("Variant", ::variant),
            )
    }
