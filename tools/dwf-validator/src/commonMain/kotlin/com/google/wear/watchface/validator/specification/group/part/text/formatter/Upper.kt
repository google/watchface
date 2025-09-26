package com.google.wear.watchface.validator.specification.group.part.text.formatter

import com.google.wear.watchface.validator.constraint.constraint

fun upper() =
    constraint("Upper") {
        allVersions()
            .allow(
                /* Child Elements */
                childElement("Template", ::template)
            )
    }
