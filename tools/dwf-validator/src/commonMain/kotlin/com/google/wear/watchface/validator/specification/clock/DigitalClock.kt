package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.clock.timeText.timeText
import com.google.wear.watchface.validator.specification.common.localization
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes

/** Specification constraint for the `DigitalClock` element. */
fun digitalClock(): Constraint =
    constraint("DigitalClock") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes,

                /* Child Elements */
                childElement("TimeText", ::timeText),
            )
            .allow(
                /* Attributes */
                *clockTypeAttributes,

                /* Child Elements */
                childElement("Variant", ::variant),
                childElement("Localization", ::localization, maxOccurs = 1),
            )
    }
