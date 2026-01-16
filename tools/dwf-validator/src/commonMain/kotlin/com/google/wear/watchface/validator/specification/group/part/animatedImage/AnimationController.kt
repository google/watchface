package com.google.wear.watchface.validator.specification.group.part.animatedImage

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint

internal val PLAY_OPTIONS =
    setOf("TAP", "ON_VISIBLE", "ON_NEXT_SECOND", "ON_NEXT_MINUTE", "ON_NEXT_HOUR")

fun animationController(): Constraint =
    constraint("AnimationController") {
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "play",
                    { value ->
                        value
                            .split(Regex("\\s"))
                            .filter { it.isNotBlank() }
                            .all { it in PLAY_OPTIONS }
                    },
                    "'play' must be a space separated list of play options: $PLAY_OPTIONS",
                )
            )
            .allow(
                /* Attributes */
                attribute(
                    "delayPlay",
                    float(min = 0.0f),
                    "delayPlay must be a positive float",
                    default = "0",
                ),
                attribute(
                    "delayRepeat",
                    float(min = 0.0f),
                    "delayRepeat must be a positive float",
                    default = "0",
                ),
                attribute(
                    "beforePlaying",
                    enum("DO_NOTHING", "FIRST_FRAME", "THUMBNAIL", "HIDE"),
                    default = "DO_NOTHING",
                ),
                attribute(
                    "afterPlaying",
                    enum("DO_NOTHING", "FIRST_FRAME", "THUMBNAIL", "HIDE"),
                    default = "DO_NOTHING",
                ),
                attribute("repeat", boolean(), default = "FALSE"),
                attribute("resumePlayBack", boolean(), default = "FALSE"),
                attribute("loopCount", integer(min = 0), "delayRepeat must be a positive integer"),
            )
    }
