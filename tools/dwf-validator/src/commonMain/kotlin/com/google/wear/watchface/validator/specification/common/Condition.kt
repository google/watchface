package com.google.wear.watchface.validator.specification.common

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.clock.analogClock
import com.google.wear.watchface.validator.specification.clock.digitalClock
import com.google.wear.watchface.validator.specification.group.group
import com.google.wear.watchface.validator.specification.group.part.animatedImage.partAnimatedImage
import com.google.wear.watchface.validator.specification.group.part.draw.partDraw
import com.google.wear.watchface.validator.specification.group.part.image.partImage
import com.google.wear.watchface.validator.specification.group.part.text.partText

/** Specification constraint for the `Condition` element. */
fun condition(): Constraint =
    constraint("Condition") {
        allVersions()
            .require(
                /* Child Elements */
                childElement("Expressions", ::expressions),

                /* Conditions */
                condition(
                    { node, ctx ->
                        val expressionsChild =
                            node.children.firstOrNull { it.tagName == "Expressions" }
                                ?: return@condition false
                        val expressionNames =
                            expressionsChild.children
                                .filter { it.tagName == "Expression" }
                                .mapNotNull { it.attributes["name"] }

                        val compareExpressions =
                            node.children
                                .filter { it.tagName == "Compare" }
                                .mapNotNull { it.attributes["expression"] }

                        val expressionNameSet = expressionNames.toSet()
                        val compareExpressionSet = compareExpressions.toSet()

                        expressionNameSet == compareExpressionSet &&
                            expressionNames.size == expressionNameSet.size &&
                            compareExpressions.size == compareExpressionSet.size
                    },
                    "There must be a one-to-one mapping between <Expression> 'name' attributes and <Compare> 'expression' attributes.",
                ),
            )
            .allow(
                /* Child Elements */
                childElement("Compare", ::compare),
                childElement("Default", ::default, maxOccurs = 1),
            )
    }

/** Specification constraint for the `Expressions` element. */
fun expressions(): Constraint =
    constraint("Expressions") {
        allVersions()
            .require(
                /* Child Elements */
                childElement("Expression", ::expression, minOccurs = 1),

                /* Conditions */
                condition(
                    childrenHaveUniqueAttribute("name"),
                    "Each Expression name must be unique",
                ),
            )
    }

/** Specification constraint for the `Expression` element. */
fun expression(): Constraint =
    constraint("Expression") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name", nonEmpty()),

                /* Content */
                content(
                    validExpression(),
                    "Expression content must be a non-empty, valid expression.",
                ),
            )
    }

/** Specification constraint for the `Compare` element. */
fun compare(): Constraint =
    constraint("Compare") {
        allVersions()
            .require(
                /* Attributes */
                attribute("expression", nonEmpty()),

                /* Conditions */
                condition(
                    hasAtLeastOneChild(),
                    "A <Compare> Tag must have at least one child element.",
                ),
            )
            .allow(
                /* Child Elements */
                childElement("Group", ::group),
                childElement("PartText", ::partText),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("PartDraw", ::partDraw),
                childElement("Condition", ::condition),
                childElement("AnalogClock", ::analogClock),
                childElement("DigitalClock", ::digitalClock),
            )
    }

/** Specification constraint for the `Default` element. */
fun default() =
    constraint("Default") {
        allVersions()
            .require(
                /* Conditions */
                condition(
                    hasAtLeastOneChild(),
                    "A <Default> Tag must have at least one child element.",
                )
            )
            .allow(
                /* Child Elements */
                childElement("Group", ::group),
                childElement("PartText", ::partText),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("PartDraw", ::partDraw),
                childElement("Condition", ::condition),
                childElement("AnalogClock", ::analogClock),
                childElement("DigitalClock", ::digitalClock),
            )
    }
