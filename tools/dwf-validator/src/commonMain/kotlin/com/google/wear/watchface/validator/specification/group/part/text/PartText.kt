package com.google.wear.watchface.validator.specification.group.part.text

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.group.part.abstractPartType

fun partText(): Constraint =
    constraint("PartText") {
        abstractPartType()

        allVersions()
            .require(
                /* Child Elements */
                choice(
                    childElement("Text", ::text, maxOccurs = 1),
                    childElement("TextCircular", ::textCircular, maxOccurs = 1),
                )
            )
    }
