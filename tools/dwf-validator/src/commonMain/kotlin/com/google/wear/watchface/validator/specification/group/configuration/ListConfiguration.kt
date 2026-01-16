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
 * Specification constraint for a 'ListConfiguration' element in a Declarative Watch Face (DWF).
 *
 * A List Configuration allows the user to select one item from a list when customizing the watch
 * face in the watch face editor.
 */
fun listConfiguration() =
    constraint("ListConfiguration") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty()),

                /* Child Elements */
                childElement("ListOption", ::listOption, maxOccurs = 100),

                /* Conditions*/
                condition(
                    childrenHaveUniqueAttribute("id"),
                    "ListOption 'id' attributes must be unique within a ListConfiguration.",
                ),
            )

        // TODO(b/443260010) validate that a ListConfiguration is declared first in
        // UserConfiguration.
    }

/** Specification constraint for a 'ListOption' element. */
private fun listOption() =
    constraint("ListOption") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty())
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
