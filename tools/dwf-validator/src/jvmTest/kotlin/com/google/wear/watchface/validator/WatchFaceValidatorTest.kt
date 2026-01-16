package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.constraint.AllowedConstraint
import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.PassAllVersions
import com.google.wear.watchface.validator.constraint.RequiredConstraint
import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.specification.WatchFaceSpecification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchFaceValidatorTest {

    @Test
    fun validateShouldApplyConstraintsToRootNode() {
        val watchFaceConstraint: Constraint =
            RequiredConstraint(
                listOf(ElementCondition { node, _ -> node.tagName == "WatchFace" }),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult =
            validator.getValidationResult(
                TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
            )
        val invalidResult =
            validator.getValidationResult(
                TestWatchFace(WatchFaceElement("BadWatchFace", emptyMap(), emptyList()))
            )

        assertTrue(validResult is ValidationResult.Success)
        assertTrue(invalidResult is ValidationResult.Failure)
    }

    @Test
    fun validatorAllowsDeclaredChildren() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Metadata", emptyMap(), emptyList())),
                )
            )
        val metadataConstraint = PassAllVersions
        val watchFaceConstraint =
            RequiredConstraint(
                listOf(ConditionLibrary.childElement("Metadata", { metadataConstraint })),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.getValidationResult(watchFace)

        assertTrue(validResult is ValidationResult.Success)
    }

    @Test
    fun validatorShouldAlsoValidateDeclaredChildren() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Metadata", emptyMap(), emptyList())),
                )
            )
        val passingMetaDataConstraint =
            RequiredConstraint(listOf(ConditionLibrary.alwaysPass()), ALL_WFF_VERSIONS)
        val passingWatchFaceConstraint =
            RequiredConstraint(
                listOf(ConditionLibrary.childElement("Metadata", { passingMetaDataConstraint })),
                ALL_WFF_VERSIONS,
            )
        val failingMetaDataConstraint =
            RequiredConstraint(listOf(ConditionLibrary.alwaysFail()), ALL_WFF_VERSIONS)
        val failingWatchFaceConstraint =
            RequiredConstraint(
                listOf(ConditionLibrary.childElement("Metadata", { failingMetaDataConstraint })),
                ALL_WFF_VERSIONS,
            )

        val validSpec = WatchFaceSpecification(passingWatchFaceConstraint, ALL_WFF_VERSIONS)
        val validResult = WatchFaceValidator(validSpec).getValidationResult(watchFace)
        val invalidSpec = WatchFaceSpecification(failingWatchFaceConstraint, ALL_WFF_VERSIONS)
        val invalidResult = WatchFaceValidator(invalidSpec).getValidationResult(watchFace)

        assertTrue(validResult is ValidationResult.Success)
        assertTrue(invalidResult is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldNotAllowUndeclaredChildren() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Metadata", emptyMap(), emptyList()),
                        WatchFaceElement("Unexpected", emptyMap(), emptyList()),
                    ),
                )
            )
        val metadataConstraint =
            RequiredConstraint(listOf(ConditionLibrary.alwaysPass()), ALL_WFF_VERSIONS)
        val watchFaceConstraint =
            RequiredConstraint(
                listOf(ConditionLibrary.childElement("Metadata", { metadataConstraint })),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldOnlyAllowDeclaredChildren() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Metadata", emptyMap(), emptyList())),
                )
            )
        val watchFaceConstraint = RequiredConstraint(listOf(), ALL_WFF_VERSIONS)

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorAllowsDeclaredAttributes() {
        val watchFace =
            TestWatchFace(WatchFaceElement("WatchFace", mapOf("attribute" to "value"), emptyList()))
        val watchFaceConstraint =
            RequiredConstraint(listOf(ConditionLibrary.attribute("attribute")), ALL_WFF_VERSIONS)

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.getValidationResult(watchFace)

        assertTrue(validResult is ValidationResult.Success)
    }

    @Test
    fun validatorShouldAlsoValidateDeclaredAttributes() {
        val watchFace =
            TestWatchFace(WatchFaceElement("WatchFace", mapOf("attribute" to "value"), emptyList()))
        val passingWatchFaceConstraint =
            RequiredConstraint(
                listOf(
                    ConditionLibrary.attribute("attribute"),
                    ElementCondition { node, _ -> node.attributes["attribute"] == "value" },
                ),
                ALL_WFF_VERSIONS,
            )
        val failingWatchFaceConstraint =
            RequiredConstraint(
                listOf(
                    ConditionLibrary.attribute("attribute"),
                    ElementCondition { node, _ -> node.attributes["attribute"] == "otherValue" },
                ),
                ALL_WFF_VERSIONS,
            )

        val validSpec = WatchFaceSpecification(passingWatchFaceConstraint, ALL_WFF_VERSIONS)
        val validResult = WatchFaceValidator(validSpec).getValidationResult(watchFace)
        val invalidSpec = WatchFaceSpecification(failingWatchFaceConstraint, ALL_WFF_VERSIONS)
        val invalidResult = WatchFaceValidator(invalidSpec).getValidationResult(watchFace)

        assertTrue(validResult is ValidationResult.Success)
        assertTrue(invalidResult is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldNotAllowUndeclaredAttributes() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement("WatchFace", mapOf("unexpectedAttribute" to "value"), emptyList())
            )
        val watchFaceConstraint = RequiredConstraint(listOf(), ALL_WFF_VERSIONS)

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldOnlyAllowDeclaredAttributes() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    mapOf("attribute" to "value", "unexpectedAttribute" to "value"),
                    emptyList(),
                )
            )
        val watchFaceConstraint =
            RequiredConstraint(listOf(ConditionLibrary.attribute("attribute")), ALL_WFF_VERSIONS)

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldReturnAllVersionsIfNoConstraintsFail() {
        val constraint: Constraint =
            constraint("WatchFace") {
                versions(1, 2, 4)
                    .require(ElementCondition { node, _ -> node.tagName == "WatchFace" })
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result =
            validator.findValidVersions(
                TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
            )

        assertEquals(setOf(1, 2, 3, 4), result)
    }

    @Test
    fun validatorShouldTakeTheIntersectionOfVersionsBetweenParentAndChildNodes() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Scene", emptyMap(), emptyList())),
                )
            )
        val sceneConstraint: Constraint = constraint("Scene") { exclusiveToVersions(2, 4) }
        val watchFaceConstraint: Constraint =
            constraint("WatchFace") {
                exclusiveToVersions(1, 2, 4)
                allVersions().allow(childElement("Scene", { sceneConstraint }))
            }

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(2, 4), result)
    }

    @Test
    fun validatorShouldTakeTheIntersectionOfVersionsBetweenSiblingNodes() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                        WatchFaceElement("Child2", emptyMap(), emptyList()),
                    ),
                )
            )
        val constraint: Constraint =
            constraint("WatchFace") {
                exclusiveToVersions(1, 2, 4)
                allVersions()
                    .allow(
                        childElement(
                            "Child1",
                            { constraint("Child1") { exclusiveToVersions(2, 4) } },
                        ),
                        childElement(
                            "Child2",
                            { constraint("Child2") { exclusiveToVersions(1, 2) } },
                        ),
                    )
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(2), result)
    }

    @Test
    fun validatorShouldTakeTheIntersectionOfVersionsBetweenManySiblingNodes() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                        WatchFaceElement("Child2", emptyMap(), emptyList()),
                        WatchFaceElement("Child3", emptyMap(), emptyList()),
                        WatchFaceElement("Child4", emptyMap(), emptyList()),
                    ),
                )
            )
        val constraint: Constraint =
            constraint("WatchFace") {
                exclusiveToVersions(1, 2, 3, 4)
                allVersions()
                    .allow(
                        childElement(
                            "Child1",
                            { constraint("Child1") { exclusiveToVersions(2, 3, 4) } },
                        ),
                        childElement(
                            "Child2",
                            { constraint("Child2") { exclusiveToVersions(2, 3) } },
                        ),
                        childElement("Child3", { constraint("Child3") { exclusiveToVersions(3) } }),
                        childElement("Child4", { constraint("Child4") { exclusiveToVersions(3) } }),
                    )
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(3), result)
    }

    @Test
    fun validatorShouldVersionWithRespectToExpressionFunctionSupport() {
        val sceneConstraint: Constraint =
            constraint("Scene") {
                exclusiveToVersions(1, 2, 4)
                allVersions().allow(attribute("expression", validExpression()))
            }
        val watchFaceConstraint: Constraint =
            constraint("WatchFace") {
                exclusiveToVersions(1, 2, 4)
                allVersions().allow(childElement("Scene", { sceneConstraint }))
            }

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val versionedExpression =
            "expression" to "colorArgb(1,2,3,4)" // only supported in version 4

        val result =
            validator.findValidVersions(
                TestWatchFace(
                    WatchFaceElement(
                        "WatchFace",
                        emptyMap(),
                        listOf(WatchFaceElement("Scene", mapOf(versionedExpression), emptyList())),
                    )
                )
            )

        assertEquals(setOf(4), result)
    }

    @Test
    fun validatorShouldVersionWithRespectToExpressionSourceSupport() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement(
                            "Scene",
                            mapOf(
                                "expression" to "[HOURS_SINCE_EPOCH]"
                            ), // only supported from version 3
                            emptyList(),
                        )
                    ),
                )
            )
        val sceneConstraint: Constraint =
            constraint("Scene") {
                exclusiveToVersions(1, 2, 4)
                allVersions().allow(attribute("expression", validExpression()))
            }
        val watchFaceConstraint: Constraint =
            constraint("WatchFace") {
                exclusiveToVersions(1, 2, 4)
                allVersions().allow(childElement("Scene", { sceneConstraint }))
            }

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(4), result)
    }

    @Test
    fun validatingAWatchFaceWithAnInvalidExpressionFails() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    mapOf("expression" to "3 *", "unexpectedAttribute" to "value"),
                    emptyList(),
                )
            )
        val watchFaceConstraint =
            RequiredConstraint(
                listOf(with(ConditionLibrary) { attribute("expression", validExpression()) }),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(watchFaceConstraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldEnforceChildOccurrences() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                    ),
                )
            )
        val constraint =
            RequiredConstraint(
                listOf(
                    ConditionLibrary.childElement(
                        "Child1",
                        { PassAllVersions },
                        minOccurs = 1,
                        maxOccurs = 1,
                    )
                ),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldEnforceChildOccurrencesInAllowsClause() {
        val noChildWatchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val oneChildWatchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Child1", emptyMap(), emptyList())),
                )
            )

        val twoChildWatchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                    ),
                )
            )

        val mixedChildWatchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement("Child1", emptyMap(), emptyList()),
                        WatchFaceElement("Child2", emptyMap(), emptyList()),
                    ),
                )
            )
        val constraint =
            AllowedConstraint(
                listOf(
                    ConditionLibrary.childElement(
                        "Child1",
                        { PassAllVersions },
                        minOccurs = 1,
                        maxOccurs = 1,
                    ),
                    ConditionLibrary.childElement("Child2", { PassAllVersions }),
                ),
                ALL_WFF_VERSIONS,
            )

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val zeroChildResult = validator.getValidationResult(noChildWatchFace)
        val oneChildResult = validator.getValidationResult(oneChildWatchFace)
        val twoChildResult = validator.getValidationResult(twoChildWatchFace)
        val mixedChildResult = validator.getValidationResult(mixedChildWatchFace)

        assertTrue(zeroChildResult is ValidationResult.Success)
        assertTrue(oneChildResult is ValidationResult.Success)
        assertTrue(twoChildResult is ValidationResult.Failure)
        assertTrue(mixedChildResult is ValidationResult.Success)
    }

    private class TestWatchFace(override val rootElement: WatchFaceElement) : WatchFaceDocument {}
}
