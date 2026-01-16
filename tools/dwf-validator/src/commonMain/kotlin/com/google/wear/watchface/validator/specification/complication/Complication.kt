package com.google.wear.watchface.validator.specification.complication

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.common.condition
import com.google.wear.watchface.validator.specification.group.group
import com.google.wear.watchface.validator.specification.group.part.animatedImage.partAnimatedImage
import com.google.wear.watchface.validator.specification.group.part.draw.partDraw
import com.google.wear.watchface.validator.specification.group.part.image.partImage
import com.google.wear.watchface.validator.specification.group.part.text.partText

fun complication(): Constraint =
    constraint("Complication") {
        allVersions()
            .require(
                /* Attributes */
                attribute("type", enum(COMPLICATION_TYPES))
            )
            .allow(
                /* Child Elements */
                childElement("Group", ::group),
                childElement("Condition", ::condition),
                childElement("PartText", ::partText),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("PartDraw", ::partDraw),
            )

        // TODO(b/443247475) Add complication specific source types to arithmetic expressions
    }
