package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun animatedImages(): Constraint =
    constraint("AnimatedImages") {
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "change",
                    enum("TAP", "ON_VISIBLE", "ON_NEXT_SECOND", "ON_NEXT_MINUTE", "ON_NEXT_HOUR"),
                    default = "TAP",
                ),

                /* Child Elements */
                choice(
                    childElement("AnimatedImage", ::animatedImage),
                    childElement("SequenceImages", ::sequenceImages),
                ),
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
