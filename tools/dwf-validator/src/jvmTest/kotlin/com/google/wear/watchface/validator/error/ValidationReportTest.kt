package com.google.wear.watchface.validator.error

import com.google.wear.watchface.validator.WatchFaceDocument
import com.google.wear.watchface.validator.WatchFaceElement
import com.google.wear.watchface.validator.WatchFaceValidator
import com.google.wear.watchface.validator.constraint.ConstraintBuilderTest.TestWatchFace
import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.constraint.constraint
import com.google.wear.watchface.validator.specification.WatchFaceSpecification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationReportTest {

    @Test
    fun validWatchFaceReturnsSuccess() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") { allVersions().require(ConditionLibrary.alwaysPass()) }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertEquals(ValidationResult.Success, result)
    }

    @Test
    fun undeclaredElementFailsWithIllegalTagError() {
        val watchFace =
            TestWatchFace(
                WatchFaceElement(
                    "WatchFace",
                    emptyMap(),
                    listOf(WatchFaceElement("Bad", emptyMap(), emptyList())),
                )
            )
        val constraint =
            constraint("WatchFace") { allVersions().require(ConditionLibrary.alwaysPass()) }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
        assertTrue(result.errorMap[GLOBAL_ERROR_KEY]?.firstOrNull() is IllegalTagError)
        assertEquals(emptySet(), result.validVersions)
    }

    @Test
    fun invalidExpressionReturnsFailure() {
        val watchFace =
            TestWatchFace(WatchFaceElement("WatchFace", mapOf("expression" to "3 +"), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions()
                    .require(
                        ConditionLibrary.alwaysPass(),
                        attribute("expression", validExpression()),
                    )
            }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)
        val error = result.errorMap[GLOBAL_ERROR_KEY]?.firstOrNull()

        assertTrue(result is ValidationResult.Failure)
        assertTrue(error is ExpressionSyntaxError)
        assertEquals(emptySet(), result.validVersions)
    }

    @Test
    fun failedVersionReturnsPartialSuccess() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions().require(ConditionLibrary.alwaysPass())
                versions(4).require(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)
        val error = result.errorMap[4]?.firstOrNull()

        assertTrue(result is ValidationResult.PartialSuccess)
        assertTrue(error is RequiredConditionFailedError)
        assertEquals(setOf(1, 2, 3), result.validVersions)
    }

    @Test
    fun passingAllowedConstraintVersionReturnsPartialSuccess() {

        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions().require(ConditionLibrary.alwaysPass())
                versions(4).allow(ConditionLibrary.alwaysPass())
            }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        val version1Error = result.errorMap[1]?.firstOrNull()
        val version2Error = result.errorMap[2]?.firstOrNull()
        val version3Error = result.errorMap[3]?.firstOrNull()

        assertTrue(result is ValidationResult.PartialSuccess)
        assertTrue(version1Error is VersionEliminationError)
        assertTrue(version2Error is VersionEliminationError)
        assertTrue(version3Error is VersionEliminationError)
        assertEquals(setOf(4), result.validVersions)
    }

    @Test
    fun invalidatingEveryVersionReturnsFailure() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 3).require(ConditionLibrary.alwaysFail())
                versions(4).require(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)
        val version1Error = result.errorMap[1]?.firstOrNull()
        val version2Error = result.errorMap[2]?.firstOrNull()
        val version3Error = result.errorMap[3]?.firstOrNull()
        val version4Error = result.errorMap[4]?.firstOrNull()

        assertTrue(result is ValidationResult.Failure)
        assertTrue(version1Error is RequiredConditionFailedError)
        assertTrue(version2Error is RequiredConditionFailedError)
        assertTrue(version3Error is RequiredConditionFailedError)
        assertTrue(version4Error is RequiredConditionFailedError)
        assertEquals(emptySet(), result.validVersions)
    }

    private class TestWatchFace(override val rootElement: WatchFaceElement) : WatchFaceDocument {}
}
