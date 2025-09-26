package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.tintColor

fun secondHand() =
    constraint("SecondHand") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes,
                attribute("resource"),

                /* Child Elements */
                choice(
                    childElement("Sweep", ::sweep, maxOccurs = 1),
                    childElement("Tick", ::tick, maxOccurs = 1),
                    minOccurs = 0,
                    errorMessage =
                        "Cannot have both Sweep and Tick elements in the same <SecondHand> tag.",
                ),
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
