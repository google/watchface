package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.group.part.image.image

fun sequenceImages(): Constraint =
    constraint("SequenceImages") {
        allVersions()
            .require(
                /* Child Elements */
                childElement("Image", ::image)
            )
            .allow(
                /* Attributes */
                attribute(
                    "loopCount",
                    integer(min = 0),
                    "loopCount must be a positive integer",
                    default = "1",
                ),
                attribute("thumbnail"),
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute(
                    "frameRate",
                    integer(1, 60),
                    "frameRate must be a positive integer",
                    default = "15",
                )
            )
    }
