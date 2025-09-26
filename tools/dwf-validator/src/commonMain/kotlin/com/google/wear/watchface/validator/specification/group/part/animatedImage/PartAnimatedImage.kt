package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.group.part.abstractPartType

internal val BLEND_MODE_OPTIONS =
    setOf(
        "CLEAR",
        "COLOR",
        "COLOR_BURN",
        "COLOR_DODGE",
        "DARKEN",
        "DIFFERENCE",
        "DST",
        "DST_ATOP",
        "DST_IN",
        "DST_OUT",
        "DST_OVER",
        "EXCLUSION",
        "HARD_LIGHT",
        "HUE",
        "LIGHTEN",
        "LUMINOSITY",
        "MODULATE",
        "MULTIPLY",
        "OVERLAY",
        "PLUS",
        "SATURATION",
        "SCREEN",
        "SOFT_LIGHT",
        "SRC",
        "SRC_ATOP",
        "SRC_IN",
        "SRC_OUT",
        "SRC_OVER",
        "XOR",
    )

fun partAnimatedImage() =
    constraint("PartAnimatedImage") {
        abstractPartType()

        allVersions()
            .require(
                /* Child Elements */
                childElement("AnimationController", ::animationController, maxOccurs = 1),
                choice(
                    childElement("AnimatedImage", ::animatedImage, maxOccurs = 1),
                    childElement("AnimatedImages", ::animatedImages, maxOccurs = 1),
                    childElement("SequenceImages", ::sequenceImages, maxOccurs = 1),
                ),
            )
            .allow(
                /* Attributes */
                attribute("blendMode", enum(BLEND_MODE_OPTIONS)),

                /* Child Elements */
                childElement("Thumbnail", ::thumbnail, maxOccurs = 1),
            )
    }
