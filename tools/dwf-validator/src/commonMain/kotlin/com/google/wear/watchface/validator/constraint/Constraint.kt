package com.google.wear.watchface.validator.constraint

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.Version
import com.google.wear.watchface.validator.WatchFaceElement
import com.google.wear.watchface.validator.WatchFaceElementContext
import com.google.wear.watchface.validator.constraint.condition.Condition
import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.condition.ExpressionCondition
import com.google.wear.watchface.validator.constraint.condition.ValueCondition
import com.google.wear.watchface.validator.error.AttributeValueError
import com.google.wear.watchface.validator.error.ContentError
import com.google.wear.watchface.validator.error.RequiredConditionFailedError
import com.google.wear.watchface.validator.error.TagOccurrenceError
import com.google.wear.watchface.validator.error.UnknownError
import com.google.wear.watchface.validator.error.ValidationError
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.error.VersionEliminationError
import com.google.wear.watchface.validator.error.combineWith
import com.google.wear.watchface.validator.expression.ExpressionParser
import kotlin.collections.minus

/** Interface for constraints that can be applied to watch face elements. */
sealed interface Constraint {
    /**
     * Checks if the given watch face element satisfies the constraint. Returns the set of versions
     * that are still valid after this constraint has been evaluated
     *
     * @param element The watch face element to check against the constraint.
     * @param context The context in which the watch face element is being evaluated.
     */
    fun check(element: WatchFaceElement, context: WatchFaceElementContext): ValidationResult
}

/**
 * A composite constraint that checks if two constraints are satisfied. It returns the intersection
 * of the version sets returned by both constraints. This is useful for combining multiple
 * constraints that must all be satisfied for a watch face.
 *
 * @property c1 The first constraint to check.
 * @property c2 The second constraint to check.
 */
data class And(val c1: Constraint, val c2: Constraint) : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        return c1.check(element, context) combineWith c2.check(element, context)
    }
}

/**
 * A constraint that requires specific conditions to be satisfied for a watch face to be valid for
 * certain versions. If any of the conditions fail, the watch face is invalid for the specified
 * versions; otherwise, it is valid for all versions.
 *
 * @property conditions The list of conditions that must be satisfied.
 * @property versions The set of versions for which this constraint applies if all conditions are
 *   met.
 */
data class RequiredConstraint(
    private val conditions: List<ElementCondition>,
    val versions: VersionSet,
) : Constraint {

    /**
     * Checks if all conditions are satisfied for the watch face element. If all conditions are
     * satisfied, it returns all versions; otherwise, it returns all other versions except the
     * specified versions.
     */
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        var newVersions: VersionSet = ALL_WFF_VERSIONS
        val errors: MutableMap<Version, MutableList<ValidationError>> = mutableMapOf()
        for (condition in conditions) {
            if (!condition.check(element, context)) {
                versions.forEach { v: Version ->
                    errors
                        .getOrPut(v) { mutableListOf() }
                        .add(
                            RequiredConditionFailedError(
                                condition.errorMessage,
                                context.elementPath,
                            )
                        )
                }
                newVersions = ALL_WFF_VERSIONS - versions
            }
        }
        return ValidationResult.of(newVersions, errors)
    }
}

/**
 * A constraint that allows specific conditions to be satisfied for a watch face to be valid for
 * certain versions. If any of the conditions are satisfied, the watch face is valid for the
 * specified versions; otherwise, it defaults to all versions.
 *
 * @property conditions The list of conditions that can allow the watch face to be valid.
 * @property versions The set of versions for which this constraint applies if any condition is met.
 */
data class AllowedConstraint(
    private val conditions: List<ElementCondition>,
    val versions: VersionSet,
) : Constraint {

    /**
     * Checks if any of the conditions are satisfied for the watch face element. If any condition is
     * satisfied, it returns the specified versions; otherwise, it returns all versions.
     */
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        var newVersions: VersionSet = ALL_WFF_VERSIONS
        val errors: MutableMap<Version, MutableList<ValidationError>> = mutableMapOf()
        for (condition in conditions) {
            if (condition.check(element, context)) {
                (ALL_WFF_VERSIONS - versions).forEach { v: Version ->
                    errors
                        .getOrPut(v) { mutableListOf() }
                        .add(
                            VersionEliminationError(
                                condition.errorMessage,
                                versions,
                                context.elementPath,
                            )
                        )
                }
                newVersions = versions
            }
        }

        return ValidationResult.of(newVersions, errors)
    }
}

/**
 * A constraint that checks if a specific attribute of a watch face element satisfies a given
 * condition. If the attribute is not present, the constraint is considered satisfied by default.
 *
 * @property attributeName The name of the attribute to check.
 * @property condition The condition to check against the attribute's value.
 */
data class AttributeConstraint(val attributeName: String, val condition: Condition) : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {

        /* Ignore the condition if the attribute is not found. */
        val attributeValue = element.attributes[attributeName] ?: return ValidationResult.Success

        when (condition) {
            is ExpressionCondition -> {
                return ExpressionParser.getValidationResult(attributeValue, context)
            }

            is ElementCondition -> {
                /* Check the condition against the node and context. */
                if (!condition.check(element, context)) {
                    return ValidationResult.Failure(
                        AttributeValueError(
                            attributeName,
                            attributeValue,
                            condition.errorMessage,
                            context.elementPath,
                        )
                    )
                }
            }

            is ValueCondition -> {
                /* Check the condition against the attributes value. */
                if (!condition.check(attributeValue)) {
                    return ValidationResult.Failure(
                        AttributeValueError(
                            attributeName,
                            attributeValue,
                            condition.errorMessage,
                            context.elementPath,
                        )
                    )
                }
            }
        }

        return ValidationResult.Success
    }
}

/**
 * A constraint that checks if the content (text) of a watch face element satisfies a given
 * condition.
 *
 * @property condition The condition to check against the attribute's value.
 */
data class ContentConstraint(val condition: Condition) : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        val content = element.textContent
        when (condition) {
            is ExpressionCondition -> {
                return ExpressionParser.getValidationResult(content, context)
            }

            is ElementCondition -> {
                /* Check the condition against the node and context. */
                if (!condition.check(element, context)) {
                    return ValidationResult.Failure(
                        ContentError(content, condition.errorMessage, context.elementPath)
                    )
                }
            }

            is ValueCondition -> {
                /* Check the condition against the attributes value. */
                if (!condition.check(element.textContent)) {
                    return ValidationResult.Failure(
                        ContentError(content, condition.errorMessage, context.elementPath)
                    )
                }
            }
        }

        return ValidationResult.Success
    }
}

/**
 * A constraint that checks if the occurrences of a specific child element within a watch face
 * element fall within a specified range. It also provides a way to retrieve the constraint tree for
 * the child element.
 *
 * @param tagName The name of the child element to check.
 * @param getConstraintTree A function that returns the constraint tree for the child element.
 * @param occurrenceRange The range of valid occurrences for the child element (inclusive).
 */
data class ChildConstraint(
    val tagName: String,
    val getConstraintTree: () -> Constraint,
    private val occurrenceRange: IntRange,
) : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        val occurrences = element.children.count { it.tagName == tagName }
        if (occurrences !in occurrenceRange) {
            return ValidationResult.Failure(
                TagOccurrenceError(tagName, occurrences, occurrenceRange, context.elementPath)
            )
        } else {
            return ValidationResult.Success
        }
    }
}

/** A utility constraint that always returns ALL_WFF_VERSIONS. */
object PassAllVersions : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        return ValidationResult.Success
    }
}

/** A utility constraint that always fails, returning the empty set. */
object FailAllVersions : Constraint {
    override fun check(
        element: WatchFaceElement,
        context: WatchFaceElementContext,
    ): ValidationResult {
        return ValidationResult.Failure(UnknownError("Instantiated by FailAllVersions.check()"))
    }
}
