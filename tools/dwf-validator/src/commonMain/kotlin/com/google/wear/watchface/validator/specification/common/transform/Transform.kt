package com.google.wear.watchface.validator.specification.common.transform

import com.google.wear.watchface.validator.constraint.constraint

/** Specification constraint for the `Transform` element. */
fun transform() =
    constraint("Transform") {
        allVersions()
            .require(
                /* Attributes */
                attribute("target"),
                attribute("value", validExpression()),
            )
            .allow(
                /* Attributes */
                attribute("mode", enum("BY", "TO"), default = "TO"),

                /* Child Elements */
                childElement("Animation", ::animation),
            )
    }
