package com.google.wear.watchface.validator.specification.group

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.angle
import com.google.wear.watchface.validator.specification.clock.analogClock
import com.google.wear.watchface.validator.specification.clock.digitalClock
import com.google.wear.watchface.validator.specification.common.condition
import com.google.wear.watchface.validator.specification.common.launch
import com.google.wear.watchface.validator.specification.common.localization
import com.google.wear.watchface.validator.specification.common.reference.reference
import com.google.wear.watchface.validator.specification.common.screenReader
import com.google.wear.watchface.validator.specification.common.transform.gyro
import com.google.wear.watchface.validator.specification.common.transform.transform
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.group.configuration.booleanConfiguration
import com.google.wear.watchface.validator.specification.group.configuration.listConfiguration
import com.google.wear.watchface.validator.specification.group.part.animatedImage.partAnimatedImage
import com.google.wear.watchface.validator.specification.group.part.draw.partDraw
import com.google.wear.watchface.validator.specification.group.part.image.partImage
import com.google.wear.watchface.validator.specification.group.part.text.partText
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.renderMode
import com.google.wear.watchface.validator.specification.scaleFloatAttributes
import com.google.wear.watchface.validator.specification.tintColor

/**
 * Specification constraint for a 'Group' element in a Declarative Watch Face (DWF).
 *
 * A Group is a container for other elements. Child elements are rendered relative to the position,
 * size, angle, and color of the group.
 */
fun group(): Constraint =
    constraint("Group") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name"),
                *geometricAttributes,
            )
            .allow(
                /* Attributes */
                attribute("id", nonEmpty()),
                angle,
                alpha,
                renderMode,
                tintColor,
                *pivots,
                *scaleFloatAttributes,
                // TODO(b/443260010) id must be unique within the watch face.

                /* Child Elements */
                childElement("Transform", ::transform),
                childElement("Variant", ::variant),
                childElement("Group", ::group),
                childElement("PartText", ::partText),
                childElement("PartImage", ::partImage),
                childElement("PartAnimatedImage", ::partAnimatedImage),
                childElement("PartDraw", ::partDraw),
                childElement("Condition", ::condition),
                childElement("ListConfiguration", ::listConfiguration),
                childElement("BooleanConfiguration", ::booleanConfiguration),
                childElement("AnalogClock", ::analogClock),
                childElement("DigitalClock", ::digitalClock),
                childElement("Launch", ::launch, maxOccurs = 1),
                childElement("Localization", ::localization, maxOccurs = 1),
                childElement("Gyro", ::gyro, maxOccurs = 1),
                childElement("ScreenReader", ::screenReader, maxOccurs = 1),
            )

        versions(4 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Reference", ::reference)
            )
    }
