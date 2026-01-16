package com.google.wear.watchface.validator.specification.complication

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.centerXAndY
import com.google.wear.watchface.validator.specification.geometricAttributes
import com.google.wear.watchface.validator.specification.outlinePadding
import com.google.wear.watchface.validator.specification.startAndEndAngles
import com.google.wear.watchface.validator.specification.widthAndHeight

/** Specification Constraint for complications that have a bounding box, oval, or arc shape. */
fun boundingBox(): Constraint =
    constraint("BoundingBox") { allVersions().require(*geometricAttributes).allow(outlinePadding) }

/** Specification Constraint for complications that have a bounding box with rounded corners. */
fun boundingRoundBox(): Constraint =
    constraint("BoundingRoundBox") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes
            )
            .allow(
                /* Attributes */
                outlinePadding,
                attribute("cornerRadius", float(), "cornerRadius must be a float"),
            )
    }

/** Specification Constraint for complications that have a bounding oval shape. */
fun boundingOval(): Constraint =
    constraint("BoundingOval") {
        allVersions()
            .require(
                /* Attributes */
                *geometricAttributes
            )
            .allow(
                /* Attributes */
                outlinePadding
            )
    }

/** Specification Constraint for complications that have a bounding arc shape. */
fun boundingArc(): Constraint =
    constraint("BoundingArc") {
        allVersions()
            .require(
                /* Attributes */
                *widthAndHeight,
                *centerXAndY,
                *startAndEndAngles,
                attribute("thickness", float(), "thickness must be a float"),
            )
            .allow(
                /* Attributes */
                outlinePadding,
                attribute("isRoundEdge", boolean(), "isRoundEdge must be a boolean"),
                attribute(
                    "direction",
                    enum("CLOCKWISE", "COUNTER_CLOCKWISE"),
                    errorMessage = "direction must be CLOCKWISE or COUNTER_CLOCKWISE",
                    default = "CLOCKWISE",
                ),
            )
    }
