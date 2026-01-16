package com.google.wear.watchface.validator.specification.group.part.draw.shape

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.draw.style.stroke
import com.google.wear.watchface.validator.specification.group.part.draw.style.weightedStroke

fun line(): Constraint =
    constraint("Line") {
        allVersions()
            .require(
                /* Attributes */
                attribute("startX", float()),
                attribute("startY", float()),
                attribute("endX", float()),
                attribute("endY", float()),

                /* Child Elements */
                choice(
                    childElement("Stroke", ::stroke, maxOccurs = 1),
                    childElement("WeightedStroke", ::weightedStroke, maxOccurs = 1),
                    errorMessage =
                        "Either Stroke or WeightedStroke is required. Note that WeightedStroke is only supported in WFFv3 and above.",
                ),
            )
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform)
            )

        versions(3 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                childElement("WeightedStroke", ::weightedStroke, maxOccurs = 1)
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
