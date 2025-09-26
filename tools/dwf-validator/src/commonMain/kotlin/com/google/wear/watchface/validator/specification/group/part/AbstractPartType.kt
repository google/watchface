package com.google.wear.watchface.validator.specification.group.part

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.ConstraintBuilder
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.angle
import com.google.wear.watchface.validator.specification.common.launch
import com.google.wear.watchface.validator.specification.common.localization
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.screenReader
import com.google.wear.watchface.validator.specification.common.transform.gyro
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.renderMode
import com.google.wear.watchface.validator.specification.scaleFloatAttributes
import com.google.wear.watchface.validator.specification.tintColor

fun ConstraintBuilder.abstractPartType() {
    allVersions()
        .require(
            /* Attributes */
            *geometricAttributes
        )
        .allow(
            /* Attributes */
            attribute("name"),
            angle,
            alpha,
            renderMode,
            tintColor,
            *scaleFloatAttributes,
            *pivots,

            /* Child Elements */
            childElement("Transform", ::transform),
            childElement("Variant", ::variant),
            childElement("Gyro", ::gyro, maxOccurs = 1),
            childElement("Launch", ::launch, maxOccurs = 1),
            childElement("Localization", ::localization, maxOccurs = 1),
            childElement("ScreenReader", ::screenReader, maxOccurs = 1),
        )

    versions(4 to MAX_WFF_VERSION)
        .allow(
            /* Child Elements */
            childElement("Reference", ::reference)
        )
}
