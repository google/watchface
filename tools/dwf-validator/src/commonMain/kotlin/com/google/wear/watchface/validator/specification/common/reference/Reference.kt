package com.google.wear.watchface.validator.specification.common.reference

import com.google.wear.watchface.validator.constraint.constraint

fun reference() =
    constraint("Reference") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name"),
                attribute("source"),
                attribute("defaultValue", color() or integer() or float()),
            )
        // TODO(b/442823511): Implement correct Transform and Reference validation
    }
