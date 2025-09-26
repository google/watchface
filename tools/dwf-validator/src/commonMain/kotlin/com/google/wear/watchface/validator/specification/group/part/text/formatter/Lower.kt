package com.google.wear.watchface.validator.specification.group.part.text.formatter

import com.google.wear.watchface.validator.constraint.constraint

fun lower() =
    constraint("Lower") {
        allVersions()
            .allow(
                /* Child Elements */
                childElement("Template", ::template)
            )
    }
