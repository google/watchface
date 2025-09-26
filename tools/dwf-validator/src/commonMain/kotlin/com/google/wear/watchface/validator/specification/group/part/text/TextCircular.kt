package com.google.wear.watchface.validator.specification.group.part.text

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.centerXAndY
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.startAndEndAngles
import com.google.wear.watchface.validator.specification.widthAndHeight

fun textCircular(): Constraint =
    constraint("TextCircular") {
        allVersions()
            .require(
                /* Attributes */
                *centerXAndY,
                *widthAndHeight,
                *startAndEndAngles, // TODO(b/442823511)  mark as transformable
            )
            .allow(
                /* Attributes */
                attribute(
                    "direction",
                    enum("CLOCKWISE", "COUNTER_CLOCKWISE"),
                    default = "CLOCKWISE",
                ),
                attribute("align", enum("START", "CENTER", "END"), "CENTER"),
                attribute("ellipsis", boolean(), errorMessage = "ellipsis must be a boolean"),

                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("BitmapFont", ::bitmapFont),
                childElement("Font", ::font),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                childElement("Reference", ::reference)
            )
    }
