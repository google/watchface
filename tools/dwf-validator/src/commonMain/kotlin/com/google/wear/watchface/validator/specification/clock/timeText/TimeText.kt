package com.google.wear.watchface.validator.specification.clock.timeText

import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.angle
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.tintColor

/** Specification constraint for the `AnalogClock` element. */
fun timeText() =
    constraint("TimeText") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes,
                attribute(
                    "format",
                    timeTextFormat(),
                    "'format' should be a valid placeholder such as 'hh:mm:ss', 'ss', 'hh_10' etc.",
                ),
                choice(
                    childElement("Font", ::font, maxOccurs = 1),
                    childElement("BitmapFont", ::bitmapFont, maxOccurs = 1),
                    minOccurs = 0,
                    errorMessage =
                        "A <TimeText> can not have both <Font> and <BitmapFont> child elements",
                ),
            )
            .allow(
                /* Attributes */
                *pivots,
                angle,
                alpha,
                tintColor,
                attribute("hourFormat", enum("12", "24", "SYNC_TO_DEVICE")),
                attribute("align", enum("START", "CENTER", "END"), default = "CENTER"),

                /* Child Elements */
                childElement("Variant", ::variant),
            )
    }
