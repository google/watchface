package com.google.wear.watchface.validator.specification.group.part.text

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.colorAttributeType
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.group.part.text.formatter.inlineImage
import com.google.wear.watchface.validator.specification.group.part.text.formatter.lower
import com.google.wear.watchface.validator.specification.group.part.text.formatter.template
import com.google.wear.watchface.validator.specification.group.part.text.formatter.upper

/**
 * Specification constraint for a 'BitmapFont' element, Specifies a particular user-defined bitmap
 * font.
 */
fun bitmapFont() =
    constraint("BitmapFont") {
        allVersions()
            .require(
                /* Attributes */
                attribute("family", nonEmpty()),
                attribute("size", float(min = 0f), "size must be a positive float"),
            )
            .allow(
                /* Attributes */
                colorAttributeType(default = "#FFFFFFFF"),

                /* Child Elements */
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
