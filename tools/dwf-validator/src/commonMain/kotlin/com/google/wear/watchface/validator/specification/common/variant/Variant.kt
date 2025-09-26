package com.google.wear.watchface.validator.specification.common.variant

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.angleDirection
import com.google.wear.watchface.validator.specification.interpolationAndControls

/** Specification constraint for the `Variant` element. */
fun variant() =
    constraint("Variant") {
        allVersions()
            .require(
                /* Attributes */
                attribute("mode", equals("AMBIENT"), "mode value must be 'AMBIENT"),
                attribute("target"),
                attribute("value", validExpression()),

                // TODO(b/442823511) target transformable and it must match an attribute name
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                *interpolationAndControls,
                angleDirection,
                attribute(
                    "duration",
                    float(0.0f, 1.0f),
                    errorMessage = "duration must be a float in range: (0.0, 1.0)",
                    default = "1.0",
                ),
                attribute(
                    "startOffSet",
                    float(0.0f, 1.0f),
                    errorMessage = "startOffSet must be a float in range: [0.0, 1.0)",
                    default = "0.0",
                ),
            )
    }
