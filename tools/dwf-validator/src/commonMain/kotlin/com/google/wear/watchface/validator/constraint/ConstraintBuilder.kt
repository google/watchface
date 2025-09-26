package com.google.wear.watchface.validator.constraint

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.Tag
import com.google.wear.watchface.validator.Version
import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ConditionScope
import com.google.wear.watchface.validator.constraint.condition.ElementCondition

typealias VersionSet = Set<Version>

/**
 * A DSL builder for constructing a complex [Constraint] from a set of version-specific rules.
 *
 * Within the builder's scope, you can define validation rules declaratively. The typical pattern
 * is:
 * 1. Define a set of watch face format (WFF) versions using [versions] or [versions].
 * 2. Chain a call to [require] or [allow] on the resulting [VersionSet].
 * 3. Pass the necessary conditions (e.g.,
 *    [com.google.wear.watchface.validator.constraint.condition.ConditionScope.hasChild],
 *    [com.google.wear.watchface.validator.constraint.condition.ConditionScope.attributeIsIn]) to
 *    these methods.
 *
 * This class implements [com.google.wear.watchface.validator.constraint.condition.ConditionScope]
 * by delegation, making all standard condition factories directly available within the builder's
 * lambda.
 *
 * ### Example
 *
 * ```kotlin
 * val constraint = constraint("MyElement") {
 *   versions(1, 2, 3).require(
 *      attribute("width", integer())
 *      attribute("height", integer())
 *
 *      childElement("ChildElement". ::childElement),
 *   )
 * ```
 */
class ConstraintBuilder(private val tagName: Tag) : ConditionScope by ConditionLibrary {
    /**
     * List of constraints that will be combined to form the final [Constraint]. Starts with a base
     * case that passes all versions.
     */
    private val constraintList = mutableListOf<Constraint>(PassAllVersions)

    /**
     * Represents all WFF versions. This is a convenience function that returns a set of all
     * versions that the constraint applies to.
     */
    fun allVersions(): VersionSet = ALL_WFF_VERSIONS

    /** Represents a set of WFF versions for the constraint builder DSL. */
    fun versions(versions: Set<Version>): VersionSet = versions

    /** Represents a set of WFF versions for the constraint builder DSL. */
    fun versions(vararg versions: Int): VersionSet = versions.toSet()

    /**
     * Convenience function for creating a set of versions that this constraint applies to. Takes in
     * a pair which can be written as `Pair(1, 4)` or `1 to 4`, the latter being more readable.
     *
     * @param versionRange the versions for which the constraint is active.
     */
    fun versions(versionRange: Pair<Version, Version>): VersionSet =
        (versionRange.first..versionRange.second).toSet()

    /**
     * Represents a list of conditions that 'must' be satisfied for the watch face to be valid for
     * the specified versions. This method creates a constraint based on the set of versions it is
     * called on together with the conditions passed in.
     *
     * @param conditions the conditions that must be satisfied for the constraint to pass.
     * @return the set of versions for which the constraint is for (for chaining constraints to
     *   avoid duplication).
     * @receiver the set of versions for which the constraint is for.
     */
    fun VersionSet.require(vararg conditions: ElementCondition): VersionSet {
        constraintList.add(RequiredConstraint(conditions.toList(), this))
        return this
    }

    /**
     * Represents a list of conditions that are permitted in the specified versions. If these
     * conditions are satisfied, then the watch can only be valid for the versions which 'allow'
     * this behaviour. This method adds a constraint based on the set of versions it is called on
     * together with the conditions passed in.
     *
     * @param conditions the permitted conditions for the specified versions.
     * @return the set of versions for which the constraint is for (for chaining constraints to
     *     * avoid duplication).
     *
     * @receiver the set of versions for which the constraint is for.
     */
    fun VersionSet.allow(vararg conditions: ElementCondition): VersionSet {
        constraintList.add(AllowedConstraint(conditions.toList(), this))
        return this
    }

    /** Adds an empty constraint that restricts the versions for which an element is valid. */
    fun exclusiveToVersions(vararg versions: Int) {
        exclusiveToVersions(versions.toSet())
    }

    /** Adds an empty constraint that restricts the versions for which an element is valid. */
    fun exclusiveToVersions(versions: Set<Version>) {
        constraintList.add(
            AllowedConstraint(
                listOf(condition(alwaysPass(), "Exclusive to versions: $versions")),
                versions,
            )
        )
    }

    /** Creates a composite constraint that combines all the constraints added to the builder. */
    fun build(): Constraint {
        constraintList.add(
            RequiredConstraint(
                listOf(
                    ElementCondition(
                        "Expected tag: $tagName",
                        { node, _ -> node.tagName == tagName },
                    )
                ),
                ALL_WFF_VERSIONS,
            )
        )
        return constraintList.reduce { acc, nextRule -> And(acc, nextRule) }
    }
}

/**
 * Creates a [Constraint] using the [ConstraintBuilder] DSL.
 *
 * This function is the primary entry point for declaratively building a complex `Constraint`. See
 * [ConstraintBuilder] for a detailed explanation and examples of the available DSL methods.
 *
 * @param block A lambda with [ConstraintBuilder] as its receiver, where you define the validation
 *   rules.
 * @return The final, composite [Constraint] that combines all defined rules.
 */
fun constraint(tagName: Tag, block: ConstraintBuilder.() -> Unit): Constraint =
    ConstraintBuilder(tagName).apply(block).build()
