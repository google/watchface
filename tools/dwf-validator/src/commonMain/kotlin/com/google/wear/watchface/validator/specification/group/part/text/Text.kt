package com.google.wear.watchface.validator.specification.group.part.text

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

fun text(): Constraint =
    constraint("Text") {
        allVersions()
            .allow(
                /* Attributes */
                attribute("align", enum("START", "CENTER", "END"), default = "CENTER"),
                attribute("ellipsis", boolean(), default = "FALSE"),
                attribute("maxLines", integer()),

                /* Child Elements */
                childElement("Font", ::font),
                childElement("BitmapFont", ::bitmapFont),
            )

        versions(3 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute("isAutoSize", boolean(), default = "FALSE")
            )
    }
