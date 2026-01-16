package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.constraint.AttributeConstraint
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.error.IllegalAttributeError
import com.google.wear.watchface.validator.error.IllegalTagError
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.error.combineWith
import com.google.wear.watchface.validator.specification.WatchFaceSpecification

/** Type aliases */
typealias Version = Int

typealias Tag = String

/**
 * Validator for a declarative watchface as a [[WatchFaceDocument]] which applies a set of custom
 * constraints.
 *
 * @param specification the watch face format .
 */
class WatchFaceValidator(private val specification: WatchFaceSpecification) {

    /**
     * Validates a declarative watch face document against the constraints defined in the
     * constraints map. It returns a set of versions for which the document is valid.
     *
     * @param document the declarative watch face document to validate.
     * @return a set of valid versions for which the document is valid.
     */
    fun findValidVersions(document: WatchFaceDocument): Set<Version> =
        getValidationResult(
                document.rootElement,
                specification.constraintTree,
                WatchFaceElementContext.emptyContext(),
            )
            .validVersions intersect specification.targetVersions

    /**
     * Validates a declarative watch face document against the constraints defined in the
     * constraints map. It returns a [[ValidationResult]] containing the versions for which the
     * document is valid and any errors encountered during validation.
     *
     * @param document the declarative watch face document to validate.
     * @return the result of the validation.
     */
    fun getValidationResult(document: WatchFaceDocument): ValidationResult =
        getValidationResult(
            document.rootElement,
            specification.constraintTree,
            WatchFaceElementContext.emptyContext(),
        )

    /**
     * Validates a watch face element against the constraints defined in the constraints map. It
     * recursively traverses the element tree and returns the intersection of valid versions from
     * each node.
     *
     * @param element the current element node to validate.
     * @param ctx the current context containing the scope, element path, and version range.
     */
    private fun getValidationResult(
        element: WatchFaceElement,
        constraint: Constraint,
        ctx: WatchFaceElementContext,
    ): ValidationResult {

        val newCtx =
            WatchFaceElementContext(
                ctx.scope + element.attributes,
                ctx.elementPath + element.tagName,
            )

        /* validating the element against the constraint.
        Also queues up further validation (for attributes, children and content) in the context. */
        var result: ValidationResult = constraint.check(element, newCtx)

        /* validate attributes */
        for (attr in element.attributes.keys) {
            if (attr in newCtx.attributeConstraintMap) {
                val attributeConstraint: AttributeConstraint = newCtx.attributeConstraintMap[attr]!!

                result = result combineWith attributeConstraint.check(element, newCtx)
            } else {
                /* return a failure if an unrecognised attribute is found. */
                result = ValidationResult.Failure(IllegalAttributeError(attr, newCtx.elementPath))
            }
        }

        /* validate content */
        for (contentConstraint in newCtx.contentConstraints) {
            result = result combineWith contentConstraint.check(element, newCtx)
        }

        /* validate child elements */
        for (child in element.children) {
            if (result.validVersions.isEmpty()) break

            /* The child must be declared in the constraint and have an associated sub-constraint */
            if (newCtx.childConstraintMap.contains(child.tagName)) {
                val childConstraint = newCtx.childConstraintMap[child.tagName]!!
                val nextConstraint = childConstraint.getConstraintTree()

                /* apply constraint to this element (usually an occurrence range check). */
                result = result combineWith childConstraint.check(element, newCtx)

                /* recurse to validate the child element itself. */
                result = result combineWith getValidationResult(child, nextConstraint, newCtx)
            } else {
                result =
                    ValidationResult.Failure(IllegalTagError(child.tagName, newCtx.elementPath))
            }
        }

        return result
    }
}
