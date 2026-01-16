package com.google.wear.watchface.validator.specification.group.part.image

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.default

fun images(): Constraint =
    constraint("Images") {
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "change",
                    enum("TAP", "ON_VISIBLE", "ON_NEXT_SECOND", "ON_NEXT_MINUTE", "ON_NEXT_HOUR"),
                    default = "TAP",
                ),

                /* Child Elements */
                childElement("Image", ::image),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute(
                    "changeDirection",
                    enum("FORWARD", "BACKWARD", "RANDOM"),
                    default = "FORWARD",
                )
            )
    }
