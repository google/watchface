package com.google.wear.watchface.validator.specification.group.part.text.formatter

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.widthAndHeight

fun inlineImage(): Constraint =
    constraint("InlineImage") {
        allVersions()
            .require(
                /* Attributes */
                *widthAndHeight,
                attribute("resource", nonEmpty()),
            )
            .allow(
                /* Attributes */
                colorAttributeType(default = "#FFFFFFFF"),
                attribute("source", validExpression()),
                attribute("overlapLeft", float()),
                attribute("overlapRight", float()),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Reference", ::reference),
            )
    }
