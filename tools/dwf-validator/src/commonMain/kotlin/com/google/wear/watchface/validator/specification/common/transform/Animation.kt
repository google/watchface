package com.google.wear.watchface.validator.specification.common.transform

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.angleDirection
import com.google.wear.watchface.validator.specification.interpolationAndControls

fun animation() =
    constraint("Animation") {
        allVersions()
            .require(
                /* Attributes */
                attribute("duration", float(min = 0f), "duration must be a non-negative float")
            )
            .allow(
                /* Attributes */
                *interpolationAndControls,
                angleDirection,
                attribute(
                    "repeat",
                    integer(min = -1),
                    errorMessage =
                        "repeat must be a non-negative integer (or -1 meaning the loop lasts forever)",
                    default = "0",
                ),
                attribute("fps", integer(min = 1), "fps must be a positive integer", default = "15"),
            )
    }
