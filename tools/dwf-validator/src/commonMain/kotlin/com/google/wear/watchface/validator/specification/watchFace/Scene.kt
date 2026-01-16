package com.google.wear.watchface.validator.specification.watchFace

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.clock.analogClock
import com.google.wear.watchface.validator.specification.clock.digitalClock
import com.google.wear.watchface.validator.specification.common.condition
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.complication.complicationSlot
import com.google.wear.watchface.validator.specification.group.configuration.booleanConfiguration
import com.google.wear.watchface.validator.specification.group.configuration.listConfiguration
import com.google.wear.watchface.validator.specification.group.group
import com.google.wear.watchface.validator.specification.group.part.animatedImage.partAnimatedImage
import com.google.wear.watchface.validator.specification.group.part.draw.partDraw
import com.google.wear.watchface.validator.specification.group.part.image.partImage
import com.google.wear.watchface.validator.specification.group.part.text.partText

/**
 * Specification constraint for a 'Scene' element.
 *
 * A scene is a container of visual tags. Each watch face must contain exactly one Scene element.
 */
fun scene(): Constraint =
    constraint("Scene") {
        allVersions()
            .require(
                // "A <Scene> Tag must have at least one child element"
                condition(
                    hasAtLeastOneChild(),
                    "A <Scene> Tag must have at least one child element.",
                )
            )
            .allow(
                /* Attributes */
                attribute(
                    "backgroundColor",
                    color() or dataSource(),
                    "'backgroundColor' must be a color in the form #RRGGBB or #AARRGGBB or a [DATA.SOURCE]",
                ),

                /* Child Elements */
                childElement("PartText", ::partText),
                childElement("PartDraw", ::partDraw),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("Group", ::group),
                childElement("Condition", ::condition),
                childElement("ListConfiguration", ::listConfiguration),
                childElement("BooleanConfiguration", ::booleanConfiguration),
                childElement("Variant", ::variant),
                childElement("AnalogClock", ::analogClock),
                childElement("DigitalClock", ::digitalClock),
                childElement("ComplicationSlot", ::complicationSlot, maxOccurs = 8),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Reference", ::reference),
            )
    }
