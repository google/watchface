package com.google.wear.watchface.validator.specification.group.part.image.imageFilter

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun imageFilters(): Constraint =
    constraint("ImageFilters") {
        allVersions()
            .require(
                /* Child Elements */
                childElement("HsbFilter", ::hsbFilter)
            )
    }
