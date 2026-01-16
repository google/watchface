package com.google.wear.watchface.validator.specification.group.part.image.imageFilter

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun hsbFilter(): Constraint =
    constraint("HsbFilter") {
        allVersions()
            .allow(
                /* Attributes */
                attribute(
                    "hueRotate",
                    float(0.0f, 360.0f),
                    "hueRotate must be a float in the range [0, 360]",
                ),
                attribute(
                    "saturate",
                    float(0.0f, 1.0f),
                    "saturate must be a float in the range [0, 1]",
                    default = "1.0",
                ),
                attribute(
                    "brightness",
                    float(0.0f, 1.0f),
                    "brightness must be a float in the range [0, 1]",
                    default = "1.0",
                ),
            )
    }
