package com.google.wear.watchface.validator.specification

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.Version
import com.google.wear.watchface.validator.constraint.Constraint

/**
 * Represents a specification for validating am XML file. This includes all of the constraints to
 * validate against as well as the format versions to validate for.
 *
 * @param constraintTree The root of the constraint tree to validate against.
 * @param targetVersions The set of format versions to validate against.
 */
data class WatchFaceSpecification(
    val constraintTree: Constraint,
    val targetVersions: Set<Version> =
        ALL_WFF_VERSIONS, // TODO(b/445347921) use this value to restrict validation to these
    // versions
)
