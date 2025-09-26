package com.google.wear.watchface.validator.specification.group.part.image

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun image(): Constraint =
    constraint("Image") {
        allVersions()
            .require(
                /* Attributes */
                attribute("resource", nonEmpty())
            )
    }
