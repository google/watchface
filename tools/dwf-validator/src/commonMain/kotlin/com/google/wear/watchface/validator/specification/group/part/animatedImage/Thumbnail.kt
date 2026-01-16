package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun thumbnail(): Constraint =
    constraint("Thumbnail") {
        allVersions()
            .require(
                /* Attributes */
                attribute("resource", nonEmpty())
            )
    }
