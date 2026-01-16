package com.google.wear.watchface.validator.specification.group.part.draw.style

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform

fun weightedStroke(): Constraint =
    constraint("WeightedStroke") {
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "colors",
                    argbVector() or dataSource(),
                    "Attribute 'colors' must be a space separated list of hex ARGB colors or a [SOURCE]",
                ),
                attribute("thickness", float(min = 0f), "thickness must be a positive float"),
            )
            .allow(
                /* Attributes */
                attribute(
                    "weights",
                    floatVector() or dataSource(),
                    "Attribute 'weights' must be a space separated list of floats",
                    default = "0.0",
                ),
                attribute(
                    "discreteGap",
                    float(min = 0.0F),
                    "discreteGap must be a non-negative float",
                    default = "0.0",
                ),
                attribute(
                    "interpolate",
                    boolean() or dataSource(),
                    "interpolate must be a boolean",
                    default = "FALSE",
                ),
                attribute("cap", enum("BUTT", "ROUND", "SQUARE"), default = "BUTT"),

                /* Child Elements */
                childElement("Transform", ::transform),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
