package com.google.wear.watchface.validator.constraint

import com.google.wear.watchface.validator.ALL_WFF_VERSIONS
import com.google.wear.watchface.validator.WatchFaceDocument
import com.google.wear.watchface.validator.WatchFaceElement
import com.google.wear.watchface.validator.WatchFaceElementContext
import com.google.wear.watchface.validator.WatchFaceValidator
import com.google.wear.watchface.validator.constraint.condition.ConditionLibrary
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.specification.WatchFaceSpecification
import junit.framework.TestCase.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstraintBuilderTest {
    @Test
    fun singleConstraintShouldPreserveVersions() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(ALL_WFF_VERSIONS).require(ConditionLibrary.alwaysPass())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(ALL_WFF_VERSIONS, result)
    }

    @Test
    fun nestedSatisfiedVersionsShouldReturnTheParentSet() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 4).require(ConditionLibrary.alwaysPass())
                versions(1 to 3).require(ConditionLibrary.alwaysPass())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(1, 2, 3, 4), result)
    }

    @Test
    fun failingRequiresShouldRemoveVersions() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 4).require(ConditionLibrary.alwaysPass())
                versions(1 to 3).require(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(4), result)
    }

    @Test
    fun passingRequiresShouldReturnAllSetsByDefault() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1).require(ConditionLibrary.alwaysPass())
                versions(3 to 4).require(ConditionLibrary.alwaysPass())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(1, 2, 3, 4), result)
    }

    @Test
    fun disjointFailingRequiresShouldRemove() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 2).require(ConditionLibrary.alwaysPass())
                versions(3 to 4).require(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(1, 2), result)
    }

    @Test
    fun validationRulesCanConstrainAllVersions() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") { allVersions().require(ConditionLibrary.alwaysPass()) }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(ALL_WFF_VERSIONS, result)
    }

    @Test
    fun failingRequiresRemovesTheVersion() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 4).require(ConditionLibrary.alwaysPass())
                versions(3).require(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(1, 2, 4), result)
    }

    @Test
    fun failingAllowsDoesNotRemoveTheVersion() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 4).require(ConditionLibrary.alwaysPass())
                versions(3).allow(ConditionLibrary.alwaysFail())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(1, 2, 3, 4), result)
    }

    @Test
    fun passingAnAllowsClauseFiltersTheVersions() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                versions(1 to 4).require(ConditionLibrary.alwaysPass())
                versions(2, 4).allow(ConditionLibrary.alwaysPass())
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.findValidVersions(watchFace)

        assertEquals(setOf(2, 4), result)
    }

    @Test
    fun requireBlockNeedsAllConstraintsToBeSatisfied() {
        val missingBothWatchFace = WatchFaceElement("WatchFace", emptyMap(), emptyList())
        val missingXWatchFace = WatchFaceElement("WatchFace", mapOf("y" to "100"), emptyList())
        val missingYWatchFace = WatchFaceElement("WatchFace", mapOf("x" to "100"), emptyList())
        val hasBothWatchFace =
            WatchFaceElement("WatchFace", mapOf("x" to "100", "y" to "100"), emptyList())

        val constraint: Constraint =
            constraint("WatchFace") { allVersions().require(attribute("x"), attribute("y")) }

        val missingBothResult =
            constraint.check(missingBothWatchFace, WatchFaceElementContext.Companion.emptyContext())
        val missingXResult =
            constraint.check(missingXWatchFace, WatchFaceElementContext.Companion.emptyContext())
        val missingYResult =
            constraint.check(missingYWatchFace, WatchFaceElementContext.Companion.emptyContext())
        val hasBothResult =
            constraint.check(hasBothWatchFace, WatchFaceElementContext.Companion.emptyContext())

        assertTrue(missingBothResult is ValidationResult.Failure)
        assertTrue(missingXResult is ValidationResult.Failure)
        assertTrue(missingYResult is ValidationResult.Failure)
        assertTrue(hasBothResult is ValidationResult.Success)
    }

    @Test
    fun allowsBlockNeedsAtLeastOneConstraintToBeSatisfiedToRestrictVersions() {
        val missingBothWatchFace = WatchFaceElement("WatchFace", emptyMap(), emptyList())
        val missingXWatchFace = WatchFaceElement("WatchFace", mapOf("y" to "100"), emptyList())
        val missingYWatchFace = WatchFaceElement("WatchFace", mapOf("x" to "100"), emptyList())
        val hasBothWatchFace =
            WatchFaceElement("WatchFace", mapOf("x" to "100", "y" to "100"), emptyList())
        val constraint: Constraint =
            constraint("WatchFace") { versions(1).allow(attribute("x"), attribute("y")) }

        val missingBothResult =
            constraint.check(missingBothWatchFace, WatchFaceElementContext.emptyContext())
        val missingXResult =
            constraint.check(missingXWatchFace, WatchFaceElementContext.emptyContext())
        val missingYResult =
            constraint.check(missingYWatchFace, WatchFaceElementContext.emptyContext())
        val hasBothResult =
            constraint.check(hasBothWatchFace, WatchFaceElementContext.emptyContext())

        assertEquals(setOf(1), hasBothResult.validVersions)
        assertEquals(setOf(1), missingXResult.validVersions)
        assertEquals(setOf(1), missingYResult.validVersions)
        assertEquals(ALL_WFF_VERSIONS, missingBothResult.validVersions)
    }

    @Test
    fun choiceClauseShouldFailIfAllConditionsAreFalse() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") { allVersions().require(choice(ConditionLibrary.alwaysFail())) }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    @Test
    fun choiceShouldPassIfOneConditionIsTrue() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions()
                    .require(choice(ConditionLibrary.alwaysFail(), ConditionLibrary.alwaysPass()))
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun choiceClauseShouldPassIfAllConditionsAreFalseAndMinIsZero() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions().require(choice(ConditionLibrary.alwaysFail(), minOccurs = 0))
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun choiceClauseShouldFailIfPassingConditionsExceedsMax() {
        val watchFace = TestWatchFace(WatchFaceElement("WatchFace", emptyMap(), emptyList()))
        val constraint =
            constraint("WatchFace") {
                allVersions()
                    .require(
                        choice(
                            ConditionLibrary.alwaysPass(),
                            ConditionLibrary.alwaysPass(),
                            ConditionLibrary.alwaysPass(),
                            maxOccurs = 2,
                        )
                    )
            }

        val spec = WatchFaceSpecification(constraint, ALL_WFF_VERSIONS)
        val validator = WatchFaceValidator(spec)
        val result = validator.getValidationResult(watchFace)

        assertTrue(result is ValidationResult.Failure)
    }

    private class TestWatchFace(override val rootElement: WatchFaceElement) : WatchFaceDocument {}
}
