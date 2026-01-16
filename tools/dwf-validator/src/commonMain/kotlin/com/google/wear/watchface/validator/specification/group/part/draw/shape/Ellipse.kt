package com.google.wear.watchface.validator.specification.group.part.draw.shape

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.draw.style.fill
import com.google.wear.watchface.validator.specification.group.part.draw.style.stroke

fun ellipse(): Constraint =
    constraint("Ellipse") {
        allVersions()
            .require(
                /* Attributes */
                attribute("x", float()),
                attribute("y", float()),
                attribute("width", float(min = 0.0F), "width must be a positive float"),
                attribute("height", float(min = 0.0F), "height must be a positive float"),

                /* Child Elements */
                choice(
                    childElement("Stroke", ::stroke, maxOccurs = 1),
                    childElement("Fill", ::fill, maxOccurs = 1),
                ),
            )
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform)
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
