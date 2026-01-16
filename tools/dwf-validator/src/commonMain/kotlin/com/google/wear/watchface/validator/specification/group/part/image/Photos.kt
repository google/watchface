package com.google.wear.watchface.validator.specification.group.part.image

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.widthAndHeight

fun photos(): Constraint =
    constraint("Photos") {
        allVersions()
            .require(
                /* Attributes */
                attribute("source", nonEmpty()),
                attribute("defaultImageResource", nonEmpty()),
                // TODO(b/443752403) 'source' must correspond to a PhotosConfiguration id
            )
            .allow(
                /* Attributes */
                *widthAndHeight,
                attribute("change", enum("TAP", "ON_VISIBLE")),
                attribute(
                    "changeAfterEvery",
                    integer(3, 10),
                    "changeAfterEvery must be an integer in the range [3. 10]",
                ),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute(
                    "changeDirection",
                    enum("FORWARD", "BACKWARD", "RANDOM"),
                    default = "FORWARD",
                )
            )
    }
