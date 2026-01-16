package com.google.wear.watchface.validator.constraint.condition

import com.google.wear.watchface.validator.DEFAULT_CONDITION_MESSAGE
import com.google.wear.watchface.validator.WatchFaceElement

sealed interface Condition

/**
 * Data class representing a condition that can be checked against a
 * [com.google.wear.watchface.validator.WatchFaceElement] with a given context. A Constraint also
 * contains a message that can be used to report errors if the condition fails.
 *
 * @property errorMessage The error message to report if the condition check fails.
 * @property check A function that takes an element (node) and a context, which evaluates the logic
 *   of the condition.
 */
data class ValueCondition(
    val errorMessage: String = DEFAULT_CONDITION_MESSAGE,
    val check: ValueConditionFunction,
) : Condition

/**
 * Data class representing a condition that can be checked against a [WatchFaceElement] with a given
 * context. A Constraint also contains a message that can be used to report errors if the condition
 * fails.
 *
 * @property errorMessage The error message to report if the condition check fails.
 * @property check A function that takes an element (node) and a context, which evaluates the logic
 *   of the condition.
 */
data class ElementCondition(
    val errorMessage: String = DEFAULT_CONDITION_MESSAGE,
    val check: ElementConditionFunction,
) : Condition

/**
 * Data class representing an expression condition. Expressions are validated by the
 * [[com.google.wear.watchface.validator.expression.ExpressionParser]] which is invoked by the
 * constraint.
 *
 * @property errorMessage The error message to report if the condition check fails.
 */
data class ExpressionCondition(val errorMessage: String = DEFAULT_CONDITION_MESSAGE) : Condition
