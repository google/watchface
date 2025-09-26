package com.google.wear.watchface.validator.specification.watchFace

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.UNBOUNDED
import com.google.wear.watchface.validator.specification.marginAttributes
import com.google.wear.watchface.validator.specification.widthAndHeight

/**
 * Specification Constraint for a 'BitmapFonts' element, a container for user-defined bitmap fonts.
 */
fun bitmapFonts(): Constraint =
    constraint("BitmapFonts") {
        allVersions()
            .require(
                /* Child Elements */
                childElement("BitmapFont", ::bitmapFont),

                /* Conditions */
                condition(
                    childrenHaveUniqueAttribute("name"),
                    "Each <BitmapFont> must have a unique 'name' attribute",
                ),
            )
    }

private fun bitmapFont(): Constraint =
    constraint("BitmapFont") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name", nonEmpty()),

                /* Child Elements */
                choice(
                    childElement("Character", ::character),
                    childElement("Word", ::word),
                    maxOccurs = UNBOUNDED,
                ),

                /* Conditions */
                condition(
                    childrenHaveUniqueAttribute("name"),
                    "Each <Character> and <Word> must have, a unique 'name' attribute",
                ),
            )
    }

/**
 * Specification constraint for a 'Character' element, Specifies a particular character in a
 * user-defined bitmap font.
 */
private fun character() =
    constraint("Character") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name", stringOfLength(1)),
                attribute("resource", nonEmpty()),
                *widthAndHeight,
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                *marginAttributes
            )
    }

/**
 * Specification constraint for a 'Word' element, Specifies a particular word in a user-defined
 * bitmap font.
 */
private fun word() =
    constraint("Word") {
        allVersions()
            .require(
                /* Attributes */
                attribute("name", nonEmpty()),
                attribute("resource", nonEmpty()),
                *widthAndHeight,
            )

        versions(2 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                *marginAttributes
            )
    }
