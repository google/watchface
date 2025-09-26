package com.google.wear.watchface.validator.specification.group.part.text.decoration

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.text.formatter.inlineImage
import com.google.wear.watchface.validator.specification.group.part.text.formatter.lower
import com.google.wear.watchface.validator.specification.group.part.text.formatter.template
import com.google.wear.watchface.validator.specification.group.part.text.formatter.upper

fun outGlow(): Constraint =
    constraint("OutGlow") {
        allVersions()
            .require(
                /* Attributes */
                colorAttributeType(),
                attribute("radius", float(min = 0.0f), default = "8.0"),
            )
            .allow(
                /* Child Elements */
                childElement("Underline", ::underline),
                childElement("StrikeThrough", ::strikeThrough),
                childElement("InlineImage", ::inlineImage),
                childElement("Template", ::template),
                childElement("Upper", ::upper),
                childElement("Lower", ::lower),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Reference", ::reference),
            )
    }
