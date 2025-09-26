package com.google.wear.watchface.validator.specification.common

import com.google.wear.watchface.validator.constraint.constraint

fun screenReader() =
    constraint("ScreenReader") {
        allVersions()
            .require(
                /* Attributes */
                attribute("stringId", nonEmpty())
            )
            .allow(
                /* Child Elements */
                childElement("Parameter", ::parameter)
            )
    }

fun parameter() =
    constraint("Parameter") {
        allVersions()
            .require(
                /* Attributes */
                attribute("expression", validExpression())
            )
    }
