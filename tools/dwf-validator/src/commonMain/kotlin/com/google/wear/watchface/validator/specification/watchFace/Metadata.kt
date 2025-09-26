package com.google.wear.watchface.validator.specification.watchFace

import com.google.wear.watchface.validator.constraint.constraint

/**
 * Specification constraint for a 'Metadata' element.
 *
 * Represents a predefined or user-defined key-value pair.
 */
fun metadata() =
    constraint("Metadata") {
        allVersions()
            .require(
                /* Attributes */
                attribute("key", nonEmpty()),
                attribute("value", nonEmpty()),

                /* Conditions */
                condition(
                    ifThen(
                        `if` = checkAttribute("key", equals("PREVIEW_TIME")),
                        then = checkAttribute("value", time()),
                    ),
                    "If key=PREVIEW_TIME, then value must be of type time (HH:MM:SS)",
                ),
                condition(
                    ifThen(
                        `if` = checkAttribute("key", equals("CLOCK_TYPE")),
                        then = checkAttribute("value", enum("ANALOG", "DIGITAL")),
                    ),
                    "If key=CLOCK_TYPE, then value must be ANALOG or DIGITAL",
                ),
                condition(
                    ifThen(
                        `if` = checkAttribute("key", equals("STEP_GOAL")),
                        then = checkAttribute("value", integer(min = 0)),
                    ),
                    "If key=STEP_GOAL, then value must be a positive integer",
                ),
            )
    }
