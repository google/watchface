package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.constraint

/** Specification constraint for the `Tick` element. */
fun tick() =
    constraint("Tick") {
        // TODO(b/438165281) make these ranges exclusive where needed
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "duration",
                    float(0.0F, 1.0F),
                    "duration must be a float in the range: (0.0, 1.0)",
                ),
                attribute(
                    "strength",
                    float(0.0F, 1.0F),
                    "strength attribute should be in range: (0.0, 1.0]",
                ),
            )
    }
