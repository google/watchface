package com.google.wear.watchface.validator.specification.userConfiguration

import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.complication.defaultProviderPolicy

/** Specification constraint for a 'Flavors' element. */
fun flavors() =
    constraint("Flavors") {
        allVersions()
            .require(
                /* Attributes */
                attribute("defaultValue", nonEmpty()),

                /* Child Elements */
                childElement("Flavor", ::flavor, maxOccurs = 20),

                /* Conditions */
                ElementCondition(
                    "Attribute 'defaultValue' must match the id of one of the Flavor children.",
                    { node, _ ->
                        val defaultValue =
                            node.attributes["defaultValue"] ?: return@ElementCondition false
                        node.children.any { it.attributes["id"]?.equals(defaultValue) ?: false }
                    },
                ),
            )
    }

/** Specification constraint for a 'Flavor' element. */
private fun flavor() =
    constraint("Flavor") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty()),
                attribute("displayName"),
                // TODO(b/443260010) make 'id' unique across user configuration.

                /* Child Elements */
                childElement("Configuration", ::configuration, maxOccurs = 100),
            )
            .allow(
                /* Attributes */
                attribute("icon"),
                attribute("screenReaderText"),

                /* Child Elements */
                childElement("ComplicationSlot", ::complicationSlot),
            )
    }

/** Specification constraint for a 'Configuration' element. */
private fun configuration() =
    constraint("Configuration") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty()),
                attribute("optionId", nonEmpty()),
                // TODO(b/443260010) 'id' should match a previously defined configuration id,
                // TODO(b/443260010) should be unique optionId
            )
            .allow(
                /* Attributes */
                attribute("screenReaderText")
            )
    }

/** Specification constraint for a 'ComplicationSlot' element inside a flavor configuration. */
fun complicationSlot() =
    constraint("ComplicationSlot") {
        allVersions()
            .require(
                /* Attributes */
                attribute("slotId", integer(), "slotId must be an integer"),
                // TODO(b/443752403) slotId must match one of the slotIds defined in the Scene

                /* Child Elements */
                childElement(
                    "DefaultProviderPolicy",
                    ::defaultProviderPolicy,
                    minOccurs = 0,
                    maxOccurs = 1,
                ),
            )
    }
