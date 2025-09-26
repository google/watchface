package com.google.wear.watchface.validator.error

import com.google.wear.watchface.validator.Version

/** An interface representing a validation error in the DWF validator. */
sealed interface ValidationError {
    val elementPath: List<String>
}

/** An error generated when the validator finds an unexpected tagName. */
data class IllegalTagError(
    val tagName: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when a tag occurs an invalid number of times. */
data class TagOccurrenceError(
    val tagName: String,
    val actualCount: Int,
    val expectedRange: IntRange,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when an attribute is not allowed on a given element. */
data class IllegalAttributeError(
    val attributeName: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when a condition fails that belongs to a required constraint. */
data class RequiredConditionFailedError(
    val conditionMessage: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when the validator encounters an expression syntax error. */
data class ExpressionSyntaxError(
    val errorMessage: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when an expression is exclusive to certain versions */
data class ExpressionVersionEliminationError(
    val expressionResource: String,
    val permittedVersions: Set<Version>,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/**
 * An error generated when a condition is exclusive to certain other versions. This is used to
 * indicate that a condition passed which is not valid for the current version of the watch face.
 *
 * This error is used in an allowed constraint.
 */
data class VersionEliminationError(
    val conditionMessage: String,
    val permittedVersions: Set<Version>,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/**
 * An error generated when the validator encounters an unknown error that does not fit any specific
 * category.
 */
data class UnknownError(
    val errorMessage: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when an attribute has an invalid value. */
data class AttributeValueError(
    val attributeName: String,
    val attributeValue: String,
    val errorMessage: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError

/** An error generated when the content of an element is invalid. */
data class ContentError(
    val content: String,
    val errorMessage: String,
    override val elementPath: List<String> = emptyList(),
) : ValidationError
