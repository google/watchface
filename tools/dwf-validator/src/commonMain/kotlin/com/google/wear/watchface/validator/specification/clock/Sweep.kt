package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.constraint

/** Specification constraint for the `Sweep` element. */
fun sweep() =
    constraint("Sweep") {
        allVersions()
            .require(
                /* Attributes */
                attribute("frequency", enum("2", "5", "10", "15"))
            )

        versions(2 to MAX_WFF_VERSION)
            .require(
                /* Attributes */
                attribute("frequency", enum("2", "5", "10", "15", "SYNC_TO_DEVICE"))
            )
    }
