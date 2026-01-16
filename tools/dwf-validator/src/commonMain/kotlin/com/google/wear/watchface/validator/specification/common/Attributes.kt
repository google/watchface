package com.google.wear.watchface.validator.specification.common

import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ElementCondition

fun colorAttributeType(default: String? = null): ElementCondition =
    with(ConditionLibrary) {
        attribute(
            "color",
            color() or dataSource(),
            errorMessage = "color must be in the form #RRGGBB or #AARRGGBB",
            default,
        )
    }
