package com.google.wear.watchface.validator.specification.group.part.draw.gradient

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform

fun radialGradient(): Constraint =
    constraint("RadialGradient") {
        allVersions()
            .require(
                /* Attributes */
                attribute("centerX", float()),
                attribute("centerY", float()),
                attribute("radius", float(min = 0.0F), "radius must be a positive float"),
                attribute(
                    "colors",
                    argbVector() or dataSource(),
                    "Attribute 'colors' must be a space separated list of hex ARGB colors",
                ),
                attribute(
                    "positions",
                    floatVector(),
                    "Attribute 'positions' must be a space separated list of floats in range [0, 1]",
                ), // TODO(b/443782460) validate each float is in range [0, 1]
            )
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform, maxOccurs = 4)
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
