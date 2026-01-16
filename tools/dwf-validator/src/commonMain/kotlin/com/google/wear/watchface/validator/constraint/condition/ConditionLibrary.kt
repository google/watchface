package com.google.wear.watchface.validator.constraint.condition

import com.google.wear.watchface.validator.constraint.AttributeConstraint
import com.google.wear.watchface.validator.constraint.ChildConstraint
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.ContentConstraint

/**
 * A scope that combines [ElementConditions] and [ValueConditions] to provide a unified interface
 * for accessing both types of conditions.
 */
interface ConditionScope : ElementConditions, ValueConditions {

    /**
     * Declare that this element can have between minOccurs and maxOccurs children with the given
     * tagName.
     */
    fun childElement(
        tagName: String,
        constraintTree: () -> Constraint,
        minOccurs: Int = 1,
        maxOccurs: Int = Int.MAX_VALUE,
        errorMessage: String = "Child element: $tagName.",
    ): ElementCondition

    /**
     * This condition declares the attribute as permitted, and associates it with the given
     * condition which is delegated to the validator via context. The condition also asserts that
     * the attribute is present.
     */
    fun attribute(
        name: String,
        conditionFunction: ValueConditionFunction,
        errorMessage: String = "Attribute: $name.",
        default: String? = null,
    ): ElementCondition

    /**
     * This condition declares the attribute as permitted, and associates it with the given
     * condition which is delegated to the validator via context. The condition also asserts that
     * the attribute is present.
     */
    fun attribute(
        name: String,
        condition: Condition = alwaysPass(),
        errorMessage: String = "Attribute: $name.",
        default: String? = null,
    ): ElementCondition

    /**
     * Declare that the content of this element must satisfy the given condition. This validation is
     * added to the context to be invoked by the validator later.
     */
    fun content(
        condition: Condition,
        errorMessage: String = "Content does not meet condition.",
    ): ElementCondition

    /** Wrap an existing ElementCondition, optionally overriding its error message. */
    fun condition(elementCondition: ElementCondition, errorMessage: String?): ElementCondition

    /** Create a new ElementCondition from a condition function and error message. */
    fun condition(
        conditionFunction: ElementConditionFunction,
        errorMessage: String,
    ): ElementCondition

    /**
     * A choice condition where between minOccurs and maxOccurs of the given conditions must hold.
     */
    fun choice(
        vararg conditions: ElementCondition,
        minOccurs: Int = 1,
        maxOccurs: Int = 1,
        errorMessage: String =
            "Between $minOccurs and $maxOccurs of the given conditions must be satisfied.",
    ): ElementCondition
}

object ConditionLibrary :
    ConditionScope,
    ElementConditions by ElementConditionLibrary,
    ValueConditions by ValueConditionLibrary {

    override fun childElement(
        tagName: String,
        constraintTree: () -> Constraint,
        minOccurs: Int,
        maxOccurs: Int,
        errorMessage: String,
    ) =
        ElementCondition(
            errorMessage,
            { node, ctx ->
                /* declare this child element as a permitted child with its associated constraint */
                ctx.childConstraintMap[tagName] =
                    ChildConstraint(tagName, constraintTree, minOccurs..maxOccurs)

                /* assert that there are between min and max children with this name */
                node.children.any { it.tagName == tagName }
            },
        )

    override fun attribute(
        name: String,
        conditionFunction: ValueConditionFunction,
        errorMessage: String,
        default: String?,
    ) =
        ElementCondition(
            errorMessage,
            { node, ctx ->
                /* Declare the attribute as permitted by adding it to the attributeConstraintMap in
                the context along with its associated constraint. */
                val attributeConstraint =
                    AttributeConstraint(name, ValueCondition(errorMessage, conditionFunction))
                ctx.attributeConstraintMap[name] = attributeConstraint

                /* assert that the attribute is present */
                node.attributes.containsKey(name)
            },
        )

    override fun attribute(
        name: String,
        condition: Condition,
        errorMessage: String,
        default: String?,
    ) =
        ElementCondition(
            errorMessage,
            { node, ctx ->
                /* Declare the attribute as permitted by adding it to the attributeConstraintMap in
                the context along with its associated constraint. */
                val attributeConstraint = AttributeConstraint(name, condition)
                ctx.attributeConstraintMap[name] = attributeConstraint

                /* assert that the attribute is present */
                node.attributes.containsKey(name)
            },
        )

    override fun content(condition: Condition, errorMessage: String) =
        ElementCondition(
            errorMessage,
            { node, ctx ->
                /* delegate the content condition to the validator via context.
                This ensures that every content condition is checked.*/
                ctx.contentConstraints.add(ContentConstraint(condition))

                /* assert that the content is non-empty */
                node.textContent.isNotEmpty()
            },
        )

    override fun condition(elementCondition: ElementCondition, errorMessage: String?) =
        ElementCondition(errorMessage ?: elementCondition.errorMessage, elementCondition.check)

    override fun condition(conditionFunction: ElementConditionFunction, errorMessage: String) =
        ElementCondition(errorMessage, conditionFunction)

    override fun choice(
        vararg conditions: ElementCondition,
        minOccurs: Int,
        maxOccurs: Int,
        errorMessage: String,
    ): ElementCondition =
        ElementCondition(
            errorMessage,
            { node, ctx ->
                val count = conditions.count { it.check(node, ctx) }
                count in minOccurs..maxOccurs
            },
        )
}
