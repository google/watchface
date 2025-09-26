package com.google.wear.watchface.validator.specification.userConfiguration

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.ConstraintBuilder

fun ConstraintBuilder.abstractConfigurationType() {
    allVersions()
        .require(
            /* Attributes */
            attribute("id", nonEmpty()),
            attribute("displayName"),
            attribute("defaultValue"),
        )
        .allow(
            /* Attributes */
            attribute("icon"),
            attribute("screenReaderText"),
        )

    versions(4 to MAX_WFF_VERSION).allow(attribute("highlight"))
}

fun ConstraintBuilder.abstractConfigurationPartType() =
    allVersions()
        .require(
            /* Attributes */
            attribute("id", nonEmpty())
        )
        .allow(
            /* Attributes */
            attribute("icon"),
            attribute("screenReaderText"),
            attribute("displayName", nonEmpty()),
        )
