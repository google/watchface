package com.google.wear.watchface.validator.constraint.condition

import com.google.wear.watchface.validator.WatchFaceElement
import com.google.wear.watchface.validator.WatchFaceElementContext

typealias ElementConditionFunction = (WatchFaceElement, WatchFaceElementContext) -> Boolean

interface ElementConditions {

    /** Condition that checks if an element has a child with the specified tag name. */
    fun hasChild(tagName: String): ElementCondition

    /** Condition that checks if an element has a specific number of children. */
    fun hasNumberOfChildren(num: Int): ElementCondition

    /** Condition that checks if a node has a nonempty child list */
    fun hasAtLeastOneChild(): ElementCondition

    /** Condition that checks if a given attribute is unique across all child elements */
    fun childrenHaveUniqueAttribute(attribute: String): ElementCondition

    /** Condition that checks a given attribute value against a custom condition function */
    fun checkAttribute(
        attribute: String,
        valueConditionFunction: ValueConditionFunction,
        errorMessage: String = "Attribute $attribute must satisfy condition",
        default: String? = null,
    ): ElementCondition

    /** Condition that checks a given attribute value against a value condition */
    fun checkAttribute(
        attribute: String,
        valueCondition: ValueCondition,
        default: String? = null,
    ): ElementCondition

    /** Checks a 'then' condition only if the 'if' condition is met */
    fun ifThen(`if`: ElementCondition, then: ElementCondition) =
        ElementCondition(
            errorMessage = "If (${`if`.errorMessage}) passes, then apply constraint (${then.errorMessage})",
            check = { node, ctx -> !`if`.check(node, ctx) || then.check(node, ctx) },
        )

    /** Combines two conditions with a logical OR */
    infix fun ElementCondition.or(other: ElementCondition) =
        ElementCondition(
            errorMessage = "(${this.errorMessage}) OR (${other.errorMessage})",
            check = { node, ctx -> this.check(node, ctx) || other.check(node, ctx) },
        )

    /** Combines two conditions with a logical AND */
    infix fun ElementCondition.and(other: ElementCondition) =
        ElementCondition(
            errorMessage = "(${this.errorMessage}) AND (${other.errorMessage})",
            check = { node, ctx -> this.check(node, ctx) && other.check(node, ctx) },
        )

    /** Condition that always passes */
    fun alwaysPass() = ElementCondition("", { _, _ -> true })

    /** Condition that always fails */
    fun alwaysFail() = ElementCondition("WARNING: Configured to always fail.", { _, _ -> false })
}

object ElementConditionLibrary : ElementConditions {

    override fun hasChild(tagName: String) =
        ElementCondition(
            "Must have child element with tag name $tagName.",
            { node, _ -> node.children.any { it.tagName == tagName } },
        )

    override fun hasNumberOfChildren(num: Int) =
        ElementCondition(
            "Must have exactly $num child elements.",
            { node, _ -> node.children.size == num },
        )

    override fun hasAtLeastOneChild() =
        ElementCondition(
            "Must have at least one child element.",
            { node, _ -> node.children.isNotEmpty() },
        )

    override fun childrenHaveUniqueAttribute(attribute: String) =
        ElementCondition(
            "Each child must have a unique value for: $attribute.",
            { node, _ ->
                val names = node.children.mapNotNull { it.attributes[attribute] }
                names.toSet().size == names.size
            },
        )

    override fun checkAttribute(
        attribute: String,
        valueConditionFunction: ValueConditionFunction,
        errorMessage: String,
        default: String?,
    ) =
        ElementCondition(
            errorMessage,
            { node, _ ->
                val attrValue = node.attributes[attribute] ?: default
                attrValue?.let(valueConditionFunction) ?: true
            },
        )

    override fun checkAttribute(
        attribute: String,
        valueCondition: ValueCondition,
        default: String?,
    ) =
        ElementCondition(
            valueCondition.errorMessage,
            { node, _ ->
                val attrValue = node.attributes[attribute] ?: default
                attrValue?.let(valueCondition.check) ?: true
            },
        )
}
