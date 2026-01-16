package com.google.wear.watchface.validator.specification.clock

import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.condition.ValueConditionLibrary
import com.google.wear.watchface.validator.specification.alpha
import com.google.wear.watchface.validator.specification.angle
import com.google.wear.watchface.validator.specification.pivots
import com.google.wear.watchface.validator.specification.renderMode
import com.google.wear.watchface.validator.specification.scaleFloatAttributes
import com.google.wear.watchface.validator.specification.tintColor

internal val clockTypeAttributes: Array<ElementCondition> =
    with(ConditionLibrary) {
        with(ValueConditionLibrary) {
            arrayOf(*pivots, *scaleFloatAttributes, angle, alpha, renderMode, tintColor)
        }
    }
