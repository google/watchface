package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun animatedImage(): Constraint =
    constraint("AnimatedImage") {
        allVersions()
            .require(
                /* Attributes */
                attribute("resource", nonEmpty()),
                attribute("format", enum("IMAGE", "AGIF", "WEBP")),
            )
            .allow(
                /* Attributes */
                attribute("thumbnail")
            )
    }
