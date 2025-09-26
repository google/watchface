package com.google.wear.watchface.validator.specification.group.part.text

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.text.decoration.outGlow
import com.google.wear.watchface.validator.specification.group.part.text.decoration.outline
import com.google.wear.watchface.validator.specification.group.part.text.decoration.shadow
import com.google.wear.watchface.validator.specification.group.part.text.decoration.strikeThrough
import com.google.wear.watchface.validator.specification.group.part.text.decoration.underline
import com.google.wear.watchface.validator.specification.group.part.text.formatter.inlineImage
import com.google.wear.watchface.validator.specification.group.part.text.formatter.lower
import com.google.wear.watchface.validator.specification.group.part.text.formatter.template
import com.google.wear.watchface.validator.specification.group.part.text.formatter.upper

fun font(): Constraint =
    constraint("Font") {
        allVersions()
            .require(
                /* Attributes */
                attribute("family", nonEmpty()),
                attribute("size", float(min = 0f), "size must be a positive float"),
            )
            .allow(
                /* Attributes */
                colorAttributeType(default = "#FFFFFFFF"),
                attribute("slant", enum("NORMAL", "ITALIC"), default = "NORMAL"),
                attribute("width", enum(FONT_WIDTH_OPTIONS), default = "NORMAL"),
                attribute("weight", enum(FONT_WEIGHT_OPTIONS), default = "NORMAL"),

                /* Child Elements */
                childElement("Shadow", ::shadow),
                childElement("Outline", ::outline),
                childElement("OutGlow", ::outGlow),
                childElement("Underline", ::underline),
                childElement("StrikeThrough", ::strikeThrough),
                childElement("InlineImage", ::inlineImage),
                childElement("Template", ::template),
                childElement("Upper", ::upper),
                childElement("Lower", ::lower),
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute("letterSpacing", float(), default = "0.0")
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Reference", ::reference),
            )
    }
