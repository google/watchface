package com.google.wear.watchface.validator.specification.group.part.draw

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.UNBOUNDED
import com.google.wear.watchface.validator.specification.group.part.abstractPartType
import com.google.wear.watchface.validator.specification.group.part.draw.shape.arc
import com.google.wear.watchface.validator.specification.group.part.draw.shape.ellipse
import com.google.wear.watchface.validator.specification.group.part.draw.shape.line
import com.google.wear.watchface.validator.specification.group.part.draw.shape.rectangle
import com.google.wear.watchface.validator.specification.group.part.draw.shape.roundRectangle

fun partDraw(): Constraint =
    constraint("PartDraw") {
        abstractPartType()

        allVersions()
            .require(

                /* Child Elements */
                choice(
                    childElement("Line", ::line),
                    childElement("Arc", ::arc),
                    childElement("Rectangle", ::rectangle),
                    childElement("Ellipse", ::ellipse),
                    childElement("RoundRectangle", ::roundRectangle),
                    maxOccurs = UNBOUNDED,
                )
            )
    }
