package com.google.wear.watchface.validator.specification.group.part.text.decoration

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.group.part.text.formatter.inlineImage
import com.google.wear.watchface.validator.specification.group.part.text.formatter.lower
import com.google.wear.watchface.validator.specification.group.part.text.formatter.template
import com.google.wear.watchface.validator.specification.group.part.text.formatter.upper

fun underline(): Constraint =
    constraint("Underline") {
        allVersions()
            .allow(
                /* Child Elements */
                childElement("Underline", ::underline),
                childElement("StrikeThrough", ::strikeThrough),
                childElement("InlineImage", ::inlineImage),
                childElement("Template", ::template),
                childElement("Upper", ::upper),
                childElement("Lower", ::lower),
            )
    }
