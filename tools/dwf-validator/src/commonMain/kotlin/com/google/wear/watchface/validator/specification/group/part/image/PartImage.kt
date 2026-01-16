package com.google.wear.watchface.validator.specification.group.part.image

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.group.part.abstractPartType
import com.google.wear.watchface.validator.specification.group.part.animatedImage.BLEND_MODE_OPTIONS
import com.google.wear.watchface.validator.specification.group.part.image.imageFilter.imageFilters

fun partImage(): Constraint =
    constraint("PartImage") {
        abstractPartType()

        allVersions()
            .require(
                /* Child Elements */
                choice(
                    childElement("Image", ::image, maxOccurs = 1),
                    childElement("Images", ::images, maxOccurs = 1),
                    childElement("Photos", ::photos, maxOccurs = 1),
                    errorMessage =
                        "One of Image, Images, or Photos is required. Note that Photos is only supported in WFFv3 and above.",
                )
            )
            .allow(
                /* Attributes */

                /* Child Elements */
                childElement("ImageFilters", ::imageFilters, maxOccurs = 1)
            )

        versions(3 to MAX_WFF_VERSION)
            .allow(
                /* Attributes */
                attribute("blendMode", enum(BLEND_MODE_OPTIONS)),

                /* Child Elements */
                childElement("Photos", ::photos, maxOccurs = 1),
            )
    }
