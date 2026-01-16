package com.google.wear.watchface.validator.error

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.Version
import com.google.wear.watchface.validator.constraint.VersionSet

typealias ErrorMap = Map<Version, MutableList<ValidationError>>

/**
 * A sealed interface representing the different types of result that the validator can produce.
 *
 * @property validVersions a set of [Version]s that are still valid after the validation process.
 * @property errorMap a map from [Version] to a list of [ValidationError]s that occurred that have
 *   invalidated that version.
 */
sealed interface ValidationResult {
    val validVersions: VersionSet
    val errorMap: ErrorMap

    /**
     * Represents a successful validation, in which the document is valid for all versions and has
     * no errors.
     */
    object Success : ValidationResult {
        override val validVersions: Set<Version> = ALL_WFF_VERSIONS
        override val errorMap: ErrorMap = emptyMap()
    }

    /**
     * Represents a validation failure, where the document is invalid for all versions. It contains
     * a map of errors that occurred during validation.
     *
     * @param errorMap a map from [Version] to a list of [ValidationError]s that occurred during
     *   validation.
     * @constructor creates a [ValidationResult.Failure] with the provided [errorMap] or a single
     *   global Error.
     */
    data class Failure(override val errorMap: ErrorMap) : ValidationResult {
        override val validVersions: VersionSet = emptySet()

        /**
         * A convenience constructor for creating a failure with a single global error. ie an error
         * which causes a failure for all versions.
         */
        constructor(
            globalError: ValidationError
        ) : this(mapOf(GLOBAL_ERROR_KEY to mutableListOf(globalError)))
    }

    /**
     * Represents a partial success in validation, where some versions are valid, but there are
     * still errors present. It contains a set of valid versions and a map of errors that occurred
     * during validation.
     *
     * @param validVersions a set of [Version]s that are still valid after the validation process.
     * @param errorMap a map from [Version] to a list of [ValidationError]s that occurred during
     *   validation.
     */
    data class PartialSuccess(
        override val validVersions: VersionSet,
        override val errorMap: ErrorMap = emptyMap(),
    ) : ValidationResult

    companion object {
        /**
         * Creates a [ValidationResult] based on the provided [versions] and [errorMap]. If the
         * [versions] set is empty, it returns a [Failure] with the provided [errorMap]. If the
         * [errorMap] is not empty, it returns a [PartialSuccess] with the provided [versions] and
         * [errorMap]. Otherwise, it returns a [Success] result.
         *
         * @param versions a set of [Version]s that are still valid after the validation process.
         * @param errorMap a map from [Version] to a list of [ValidationError]s that occurred during
         *   validation.
         * @return a [ValidationResult] representing the outcome of the validation.
         */
        fun of(versions: VersionSet, errorMap: ErrorMap): ValidationResult {
            return when {
                versions.isEmpty() -> Failure(errorMap)
                errorMap.isNotEmpty() -> PartialSuccess(versions, errorMap)
                else -> Success
            }
        }
    }
}

/**
 * Combines two [ValidationResult]s by intersecting their valid versions and merging their error
 * maps.
 */
infix fun ValidationResult.combineWith(other: ValidationResult): ValidationResult {
    val newValidVersions = this.validVersions intersect other.validVersions
    val combinedErrors =
        (this.errorMap.asSequence() + other.errorMap.asSequence())
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, lists) -> lists.flatten().toMutableList() }

    return ValidationResult.of(newValidVersions, combinedErrors)
}
