package com.google.wear.watchface.validator.specification.group.part.draw.style

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.draw.gradient.linearGradient
import com.google.wear.watchface.validator.specification.group.part.draw.gradient.radialGradient
import com.google.wear.watchface.validator.specification.group.part.draw.gradient.sweepGradient

fun stroke(): Constraint =
    constraint("Stroke") {
        allVersions()
            .require(
                /* Attributes */
                colorAttributeType(),
                attribute("thickness", float(min = 0.0F), "thickness must be a non-negative float"),
                /* Child Elements */

                /* Conditions */
            )
            .allow(
                /* Attributes */
                attribute(
                    "dashIntervals",
                    floatVector(),
                    "dashIntervals must be a space separated vector of float",
                ),
                attribute("dashPhase", float(), "dashPhase must be a float"),
                attribute("cap", enum("BUTT", "ROUND", "SQUARE"), default = "BUTT"),

                /* Child Elements */
                childElement("LinearGradient", ::linearGradient),
                childElement("RadialGradient", ::radialGradient),
                childElement("SweepGradient", ::sweepGradient),
                childElement("Transform", ::transform),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
