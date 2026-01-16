package com.google.wear.watchface.validator.specification.group.part.draw.shape

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.draw.style.stroke
import com.google.wear.watchface.validator.specification.group.part.draw.style.weightedStroke
import com.google.wear.watchface.validator.specification.startAndEndAngles

fun arc(): Constraint =
    constraint("Arc") {
        allVersions()
            .require(
                /* Attributes */
                *startAndEndAngles,
                attribute("width", float(min = 0.0F), "width must be a positive float"),
                attribute("height", float(min = 0.0F), "height must be a positive float"),
                attribute("centerX", float()),
                attribute("centerY", float()),

                /* Child Elements */
                choice(
                    childElement("Stroke", ::stroke, maxOccurs = 1),
                    childElement("WeightedStroke", ::weightedStroke, maxOccurs = 1),
                    errorMessage =
                        "Either Stroke or WeightedStroke is required. Note that WeightedStroke is only supported in WFFv2 and above.",
                ),
            )
            .allow(
                /* Attributes */
                attribute(
                    "direction",
                    enum("CLOCKWISE", "COUNTER_CLOCKWISE"),
                    default = "CLOCKWISE",
                ),

                /* Child Elements */
                childElement("Transform", ::transform),
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("WeightedStroke", ::weightedStroke, maxOccurs = 1)
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
