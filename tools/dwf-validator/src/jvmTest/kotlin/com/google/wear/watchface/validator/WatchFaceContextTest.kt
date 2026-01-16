package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.constraint.Constraint
import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.condition.ElementCondition
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.specification.WatchFaceSpecification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WatchFaceContextTest {

    @Test
    fun contextShouldKeepTrackOfVisitedElements() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Metadata", emptyMap(), emptyList())),
                )
            )
        val metadataConstraint: Constraint =
            constraint("Metadata") {
                allVersions()
                    .require(
                        ElementCondition { _, ctx ->
                            ctx.elementPath == listOf("WatchFace", "Metadata")
                        }
                    )
            }
        val constraint: Constraint =
            constraint("WatchFace") {
                allVersions().require(childElement("Metadata", { metadataConstraint }))
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.findValidVersions(watchFace)

        assertEquals(ALL_WFF_VERSIONS, validResult)
    }

    @Test
    fun contextShouldKeepAllAttributesInScope() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Metadata", mapOf("key" to "CLOCK_TYPE"), emptyList())),
                )
            )
        val metadataConstraint: Constraint =
            constraint("Metadata") {
                allVersions()
                    .require(
                        attribute("key"),
                        ElementCondition { _, ctx -> ctx.scope["key"] == "CLOCK_TYPE" },
                    )
            }
        val constraint: Constraint =
            constraint("WatchFace") {
                allVersions().require(childElement("Metadata", { metadataConstraint }))
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.findValidVersions(watchFace)

        assertEquals(ALL_WFF_VERSIONS, validResult)
    }

    @Test
    fun validatorShouldValidateAttributeExpressionIfFlaggedInContext() {
        val constraint: Constraint =
            constraint("WatchFace") {
                allVersions().require(attribute("expression", validExpression()))
            }
        val validDocument =
            TestWatchFace(
                WatchFaceElement("WatchFace", mapOf("expression" to "1 + 2"), emptyList())
            )
        val inValidDocument =
            TestWatchFace(WatchFaceElement("WatchFace", mapOf("expression" to "1 +"), emptyList()))

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.getValidationResult(validDocument)
        val invalidResult = validator.getValidationResult(inValidDocument)

        assertEquals(ValidationResult.Success, validResult)
        assertTrue(invalidResult is ValidationResult.Failure)
    }

    @Test
    fun validatorShouldValidateContentExpressionIfFlaggedInContext() {
        val constraint: Constraint =
            constraint("WatchFace") { allVersions().require(content(validExpression())) }
        val validDocument =
            TestWatchFace(
                WatchFaceElement("WatchFace", emptyMap(), emptyList(), textContent = "1 + 2")
            )
        val inValidDocument =
            TestWatchFace(
                WatchFaceElement("WatchFace", emptyMap(), emptyList(), textContent = "1 + ")
            )

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val validResult = validator.getValidationResult(validDocument)
        val invalidResult = validator.getValidationResult(inValidDocument)

        assertTrue(validResult is ValidationResult.Success)
        assertTrue(invalidResult is ValidationResult.Failure, "was: $invalidResult")
    }

    @Test
    fun contextShouldResetExpressionContentCheckBetweenSiblings() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement(
                            "Expression",
                            emptyMap(),
                            emptyList(),
                            textContent = "1 + 2",
                        ),
                        WatchFaceElement(
                            "NoExpression",
                            emptyMap(),
                            emptyList(),
                            textContent = "1 +",
                        ),
                    ),
                )
            )
        val expressionConstraint: Constraint =
            constraint("Expression") { allVersions().require(content(validExpression())) }
        val noExpressionConstraint: Constraint =
            constraint("NoExpression") { allVersions().require(ConditionLibrary.alwaysPass()) }
        val constraint: Constraint =
            constraint("WatchFace") {
                allVersions()
                    .require(
                        childElement("Expression", { expressionConstraint }),
                        childElement("NoExpression", { noExpressionConstraint }),
                    )
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertEquals(ValidationResult.Success, result)
    }

    @Test
    fun contextShouldResetExpressionContentCheckBetweenParentAndChild() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(
                        WatchFaceElement(
                            "Expression",
                            emptyMap(),
                            listOf(
                                WatchFaceElement(
                                    "NoExpression",
                                    emptyMap(),
                                    emptyList(),
                                    textContent = "1 +",
                                )
                            ),
                            textContent = "1 + 2",
                        )
                    ),
                )
            )
        val noExpressionConstraint: Constraint =
            constraint("NoExpression") { allVersions().require(ConditionLibrary.alwaysPass()) }
        val expressionConstraint: Constraint =
            constraint("Expression") {
                allVersions()
                    .require(
                        childElement("NoExpression", { noExpressionConstraint }),
                        content(validExpression()),
                    )
            }
        val constraint: Constraint =
            constraint("WatchFace") {
                allVersions().require(childElement("Expression", { expressionConstraint }))
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertEquals(ValidationResult.Success, result)
    }

    private class TestWatchFace(override val rootElement: WatchFaceElement) : WatchFaceDocument {}
}
