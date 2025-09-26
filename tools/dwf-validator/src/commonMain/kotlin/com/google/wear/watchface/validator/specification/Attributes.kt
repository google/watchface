package com.google.wear.watchface.validator.specification

import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ElementCondition

internal val widthAndHeight: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "width",
                integer(min = 0),
                errorMessage = "Attribute: 'width' must be a positive integer",
            ),
            attribute(
                "height",
                integer(min = 0),
                errorMessage = "Attribute: 'height' must be a positive integer",
            ),
        )
    }

internal val geometricAttributes: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute("x", integer(), errorMessage = "Attribute: 'x' must be an integer"),
            attribute("y", integer(), errorMessage = "Attribute: 'y' must be an integer"),
            *widthAndHeight,
        )
    }

internal val pivots: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "pivotX",
                float(0.0F, 1.0F),
                errorMessage = "Attribute: 'pivotX' must be a float in the range 0.0 to 1.0",
            ),
            attribute(
                "pivotY",
                float(0.0F, 1.0F),
                errorMessage = "Attribute: 'pivotY' must be a float in the range 0.0 to 1.0",
            ),
        )
    }

internal val angle: ElementCondition =
    with(ConditionLibrary) {
        attribute("angle", float(), errorMessage = "Attribute: 'angle' must be a float.")
    }

internal val alpha: ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "alpha",
            integer(0, 255),
            errorMessage = "Attribute: 'alpha' must be an int in the range 0 to 255",
        )
    }

internal val scaleFloatAttributes: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute("scaleX", float(), errorMessage = "Attribute: 'scaleX' must be a float"),
            attribute("scaleY", float(), errorMessage = "Attribute: 'scaleY' must be a float"),
        )
    }
internal val renderMode: ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "renderMode",
            enum("SOURCE", "MASK", "ALL"),
            errorMessage = "Attribute: 'renderMode' must be one of 'SOURCE', 'MASK' or 'ALL'",
            default = "SOURCE",
        )
    }

internal val tintColor: ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "tintColor",
            color() or dataSource(),
            errorMessage = "Attribute: 'tintColor' must be in the form #RRGGBB or #AARRGGBB",
        )
    }

val marginAttributes: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "marginLeft",
                integer(0, Int.MAX_VALUE),
                errorMessage = "Attribute: 'marginLeft' must be a non-negative integer",
            ),
            attribute(
                "marginRight",
                integer(0, Int.MAX_VALUE),
                errorMessage = "Attribute: 'marginRight' must be a non-negative integer",
            ),
            attribute(
                "marginTop",
                integer(0, Int.MAX_VALUE),
                errorMessage = "Attribute: 'marginTop' must be a non-negative integer",
            ),
            attribute(
                "marginBottom",
                integer(0, Int.MAX_VALUE),
                errorMessage = "Attribute: 'marginBottom' must be a non-negative integer",
            ),
        )
    }

internal val interpolationAndControls: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "interpolation",
                enum("LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT", "OVERSHOOT", "CUBIC_BEZIER"),
                errorMessage =
                    "Attribute: 'interpolation' must be one of LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, OVERSHOOT or CUBIC_BEZIER",
                default = "LINEAR",
            ),
            attribute(
                "controls",
                floatVector(4),
                errorMessage =
                    "Attribute: 'controls' must be a 4-component space separated vector: 0.5 0.5 0.5 0.5",
                default = "0.5 0.5 0.5 0.5",
            ),
        )
    }

internal val angleDirection: ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "angleDirection",
            enum("CLOCKWISE", "COUNTER_CLOCKWISE", "NONE"),
            errorMessage =
                "Attribute: 'angleDirection' must be one of CLOCKWISE, COUNTER_CLOCKWISE or NONE",
            default = "NONE",
        )
    }

internal val outlinePadding: ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "outlinePadding",
            float(),
            errorMessage = "Attribute: 'outlinePadding' must be a float",
            default = "0.0",
        )
    }

internal val startAndEndAngles: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "startAngle",
                float(),
                errorMessage = "Attribute: 'startAngle' must be a float",
            ),
            attribute("endAngle", float(), errorMessage = "Attribute: 'endAngle' must be a float"),
        )
    }

internal val cornerRadiiAttributes: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute(
                "cornerRadiusX",
                float(),
                errorMessage = "Attribute: 'cornerRadiusX' must be a float",
            ),
            attribute(
                "cornerRadiusY",
                float(),
                errorMessage = "Attribute: 'cornerRadiusY' must be a float",
            ),
        )
    }

internal val centerXAndY: Array<ElementCondition> =
    with(ConditionLibrary) {
        arrayOf(
            attribute("centerX", float(), errorMessage = "Attribute: 'centerX' must be a float"),
            attribute("centerY", float(), errorMessage = "Attribute: 'centerY' must be a float"),
        )
    }
