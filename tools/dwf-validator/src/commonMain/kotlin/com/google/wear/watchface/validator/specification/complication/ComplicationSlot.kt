package com.google.wear.watchface.validator.specification.complication

import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.angle
import com.google.wear.watchface.validator.specification.common.screenReader
import com.google.wear.watchface.validator.specification.common.variant.variant
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.scaleFloatAttributes
import com.google.wear.watchface.validator.specification.tintColor

fun complicationSlot() =
    constraint("ComplicationSlot") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes,
                attribute("slotId", integer(), "slotId must be an integer"),
                attribute(
                    "supportedTypes",
                    { value ->
                        value
                            .split(Regex("\\s"))
                            .filter { it.isNotBlank() }
                            .all { it in COMPLICATION_TYPES }
                    },
                    "must be a space separated list of supported complication types: $COMPLICATION_TYPES",
                ),
                // TODO(b/443260010) slotId must be unique

                /* Child Elements */
                choice(
                    childElement("BoundingBox", ::boundingBox),
                    childElement("BoundingRoundBox", ::boundingRoundBox),
                    childElement("BoundingOval", ::boundingOval),
                    childElement("BoundingArc", ::boundingArc),
                    errorMessage =
                        "A ComplicationSlot element must contain exactly one Bounding Area element.",
                ),
                childElement("Complication", ::complication),

                /* Conditions */
                ElementCondition(
                    "A ComplicationSlot must contain at least one Complication element per supported Complication Type",
                    { node, _ ->
                        val supportedTypes =
                            node.attributes["supportedTypes"]
                                ?.split(Regex("\\s"))
                                ?.filter { it.isNotBlank() }
                                ?: return@ElementCondition false

                        val complicationTypes =
                            node.children
                                .asSequence()
                                .filter { it.tagName == "Complication" }
                                .mapNotNull { it.attributes["type"] }
                                .toSet()

                        supportedTypes.all { it in complicationTypes }
                    },
                ),
            )
            .allow(
                /* Attributes */
                angle,
                tintColor,
                alpha,
                *scaleFloatAttributes,
                *pivots,
                attribute("name", nonEmpty()),
                attribute("displayName"),
                attribute("isCustomizable", boolean(), "isCustomizable must be a boolean"),

                /* Child Elements */
                childElement("DefaultProviderPolicy", ::defaultProviderPolicy, maxOccurs = 1),
                childElement("ScreenReader", ::screenReader, maxOccurs = 1),
                childElement("Variant", ::variant),
            )
    }
