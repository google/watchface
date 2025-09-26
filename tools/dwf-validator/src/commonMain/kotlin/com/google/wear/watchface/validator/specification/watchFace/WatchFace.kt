package com.google.wear.watchface.validator.specification.watchFace

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.cornerRadiiAttributes
import com.google.wear.watchface.validator.specification.userConfiguration.userConfigurations
import com.google.wear.watchface.validator.specification.widthAndHeight

/**
 * Specification Constraint for a 'WatchFace' element, the root element of a watch face document.
 *
 * It contains information about the elements that should appear in the watch face preview when
 * users choose which watch face to use on their Wear OS devices.
 */
fun watchFace(): Constraint =
    constraint("WatchFace") {
        allVersions()
            .require(
                /* Attributes */
                *widthAndHeight,

                /* Child Elements */
                childElement("Scene", ::scene, maxOccurs = 1),
            )
            .allow(
                /* Attributes */
                *cornerRadiiAttributes,
                attribute(
                    "clipShape",
                    enum("NONE", "CIRCLE", "RECTANGLE"),
                    "Attribute: 'clipShape' must be one of 'NONE', 'CIRCLE' or 'RECTANGLE'",
                ),

                /* Child Elements */
                childElement("Metadata", ::metadata),
                childElement("BitmapFonts", ::bitmapFonts, maxOccurs = 1),
                childElement("UserConfigurations", ::userConfigurations, maxOccurs = 1),
            )
    }
