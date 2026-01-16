package com.google.wear.watchface.validator.specification.userConfiguration

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.constraint

/** Specification constraint for a 'UserConfigurations' element. */
fun userConfigurations() =
    constraint("UserConfigurations") {
        allVersions()
            .require(
                /* Conditions */
                condition(
                    childrenHaveUniqueAttribute("id"),
                    "Each configuration must have a unique 'id' attribute.",
                ),
                condition(
                    { node, _ -> node.children.size in 1..20 },
                    "UserConfigurations must have between 1 and 20 child elements.",
                ),
            )
            .allow(
                /* Child Elements */
                childElement("BooleanConfiguration", ::booleanConfiguration),
                childElement("ListConfiguration", ::listConfiguration),
                childElement("ColorConfiguration", ::colorConfiguration),
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("Flavors", ::flavors)
            )

        versions(3 to MAX_WFF_VERSION)
            .allow(
                /* Child Elements */
                childElement("PhotosConfiguration", ::photosConfiguration)
            )
    }

/**
 * Specification constraint for a 'ListConfiguration' element in a Declarative Watch Face (DWF).
 *
 * A List Configuration allows the user to select one item from a list when customizing the watch
 * face in the watch face editor.
 */
fun listConfiguration() =
    constraint("ListConfiguration") {
        abstractConfigurationType()
        allVersions()
            .require(
                /* Child Elements */
                childElement("ListOption", ::listOption, maxOccurs = 100),

                /* Conditions*/
                ElementCondition(
                    "Attribute 'defaultValue' must match the id of one of the ListOption children.",
                    { node, _ ->
                        node.children.any {
                            it.attributes["id"]?.equals(node.attributes["defaultValue"]) ?: false
                        }
                    },
                ),
            )
    }

/** Specification constraint for a 'ListOption' element. */
private fun listOption() = constraint("ListOption") { abstractConfigurationPartType() }

fun colorConfiguration() =
    constraint("ColorConfiguration") {
        abstractConfigurationType()
        allVersions()
            .require(
                /* Child Elements */
                childElement("ColorOption", ::colorOption, maxOccurs = 20),

                /* Conditions */
                ElementCondition(
                    "Attribute 'defaultValue' must match the id of one of the ColorOption children",
                    { node, _ ->
                        node.children.any {
                            it.attributes["id"]?.equals(node.attributes["defaultValue"]) ?: false
                        }
                    },
                ),
            )
    }

/** Specification constraint for a 'ColorOption' element. */
fun colorOption() =
    constraint("ColorOption") {
        abstractConfigurationPartType()
        allVersions()
            .require(
                /* Attributes */
                attribute(
                    "colors",
                    argbVector(maxLength = 5),
                    "Attribute 'colors' must be a space separated list of hex ARGB colors. Max length is 5.",
                )
            )
    }

/** Specification constraint for a 'PhotosConfiguration' element. */
fun photosConfiguration() =
    constraint("PhotosConfiguration") {
        allVersions()
            .require(
                /* Attributes */
                attribute("id", nonEmpty()),
                attribute("configType", enum("SINGLE", "MULTIPLE")),
            )
    }

/**
 * Specification constraint for a 'BooleanConfiguration' element.
 *
 * A BooleanConfiguration must be defined in the user configuration section of the XML. Then it may
 * be referenced by id from a Scene element.
 */
fun booleanConfiguration() =
    constraint("BooleanConfiguration") {
        abstractConfigurationType()
        allVersions()
            .require(
                /* Attributes */
                attribute("defaultValue", enum("TRUE", "FALSE"))
            )
    }
