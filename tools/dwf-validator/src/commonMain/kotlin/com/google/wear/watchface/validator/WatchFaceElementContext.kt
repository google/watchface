package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.constraint.AttributeConstraint
import com.google.wear.watchface.validator.constraint.ChildConstraint
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.ContentConstraint

typealias ConstraintPointer = () -> Constraint

typealias AttributeConstraintMap = MutableMap<String, AttributeConstraint>

typealias ChildConstraintMap = MutableMap<String, ChildConstraint>

typealias ContentConstraintList = MutableList<ContentConstraint>

/**
 * A data class for keeping track of relevant information about previously visited nodes when
 * traversing the element tree.
 *
 * @property scope a map of attributes which have been declared inside parent tags.
 * @property elementPath a list of tag names representing the path to the current element.
 * @property expressionAttributes a list of attribute names in the current element which contain
 *   expressions.
 *     @property permittedAttributes a mutable list of attribute names that are allowed for the
 *       current element.
 *     @property permittedChildElements a mutable list of pairs of tag names and their associated
 *       constraints that are allowed as children of the current element.
 *     @property expressionContentCheck a flag indicating whether the text content of the current
 *       element should be checked for expressions.
 */
data class WatchFaceElementContext(
    val scope: Map<String, String>,
    val elementPath: List<String>,
    val attributeConstraintMap: AttributeConstraintMap = mutableMapOf(),
    val childConstraintMap: ChildConstraintMap = mutableMapOf(),
    val contentConstraints: ContentConstraintList = mutableListOf(),
) {
    companion object {
        fun emptyContext(): WatchFaceElementContext =
            WatchFaceElementContext(emptyMap(), emptyList())
    }
}
