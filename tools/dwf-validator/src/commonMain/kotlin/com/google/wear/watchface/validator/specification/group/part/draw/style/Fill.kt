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

fun fill(): Constraint =
    constraint("Fill") {
        allVersions()
            .require(
                /* Attributes */
                colorAttributeType()
            )
            .allow(
                /* Child Elements */
                childElement("LinearGradient", ::linearGradient),
                childElement("RadialGradient", ::radialGradient),
                childElement("SweepGradient", ::sweepGradient),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Reference", ::reference),
            )
    }
