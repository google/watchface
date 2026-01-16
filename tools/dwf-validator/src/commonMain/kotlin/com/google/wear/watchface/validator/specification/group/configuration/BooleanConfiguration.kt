package com.google.wear.watchface.validator.specification.group.configuration

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.clock.analogClock
import com.google.wear.watchface.validator.specification.clock.digitalClock
import com.google.wear.watchface.validator.specification.common.condition
import com.google.wear.watchface.validator.specification.group.group
import com.google.wear.watchface.validator.specification.group.part.animatedImage.partAnimatedImage
import com.google.wear.watchface.validator.specification.group.part.draw.partDraw
import com.google.wear.watchface.validator.specification.group.part.image.partImage
import com.google.wear.watchface.validator.specification.group.part.text.partText

/**
 * Specification constraint for a 'BooleanConfiguration' element.
 *
 * A BooleanConfiguration must be defined in the user configuration section of the XML. Then it may
 * be referenced by id from a Scene element.
 */
fun booleanConfiguration() =
    constraint("BooleanConfiguration") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty()),

                /* Child Elements */
                childElement("BooleanOption", ::booleanOption, minOccurs = 1, maxOccurs = 2),

                /* Conditions */
                condition(
                    childrenHaveUniqueAttribute("id"),
                    "BooleanOption 'id' attributes must be unique within a BooleanConfiguration.",
                ),
            )

        // TODO(b/446857123): CANNOT BE NESTED WITHIN A SCENE WHEN id uses 2 or more configuration
        // options as data,
        // TODO(b/443260010) validate that a BooleanConfiguration is declared first in
        // UserConfiguration.
    }

/** Specification constraint for a 'BooleanOption' element. */
private fun booleanOption() =
    constraint("BooleanOption") {
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "id",
                    enum("TRUE", "FALSE"),
                    "Attribute 'id' must be either 'TRUE' or 'FALSE'.",
                )
            )
            .allow(
                /* Child Elements */
                childElement("PartText", ::partText),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("PartDraw", ::partDraw),
                childElement("Group", ::group),
                childElement("Condition", ::condition),
                childElement("AnalogClock", ::analogClock),
                childElement("DigitalClock", ::digitalClock),
            )
    }
