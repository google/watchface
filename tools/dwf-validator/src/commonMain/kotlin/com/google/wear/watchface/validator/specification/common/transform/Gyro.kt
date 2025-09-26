package com.google.wear.watchface.validator.specification.common.transform

import com.google.wear.watchface.validator.constraint.constraint

/** Specification constraint for the `Gyro` element. */
fun gyro() =
    constraint("Gyro") {
        allVersions()
            .allow(
                /* Attributes */
                attribute("x", validExpression()),
                attribute("y", validExpression()),
                attribute("angle", validExpression()),
                attribute("alpha", validExpression()),
                attribute("scaleX", validExpression()),
                attribute("scaleY", validExpression()),
            )
    }
