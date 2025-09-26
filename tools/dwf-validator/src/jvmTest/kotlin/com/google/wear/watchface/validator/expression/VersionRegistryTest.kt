package com.google.wear.watchface.validator.expression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VersionRegistryTest {

    @Test
    fun nonExistentFunctionThrowsFunctionNotFoundException() {
        val fakeFunction: FunctionCall = FunctionCall("fakeFunction", emptyList())

        assertFailsWith<FunctionNotFoundException> {
            VersionRegistry.getFunctionVersions(fakeFunction)
        }
    }

    @Test
    fun existentFunctionWithIncorrectArityThrowsFunctionNotFoundException() {
        val tooManyArgumentsFunction: FunctionCall =
            FunctionCall("sin", listOf(NumLiteral(6.0), NumLiteral(7.0)))

        assertFailsWith<FunctionNotFoundException> {
            VersionRegistry.getFunctionVersions(tooManyArgumentsFunction)
        }
    }

    @Test
    fun functionWithCorrectArityReturnsVersions() {
        val sin: FunctionCall = FunctionCall("sin", listOf(NumLiteral(6.0)))

        val versions: VersionRange = VersionRegistry.getFunctionVersions(sin)

        assertEquals(VersionRange(1), versions)
    }

    @Test
    fun existentSourcesReturnsVersions() {
        val versions: VersionRange = VersionRegistry.getSourceVersions("HEART_RATE")

        val expectedVersions = VersionRange(1)

        assertEquals(expectedVersions, versions)
    }

    @Test
    fun sourcesWithIndexPlaceholderReturnsVersions() {
        val versions: VersionRange =
            VersionRegistry.getSourceVersions("WEATHER.HOURS.{index}.IS_AVAILABLE")

        val expectedVersions = VersionRange(2)

        assertEquals(expectedVersions, versions)
    }

    @Test
    fun sourcesWithIntegerIndexReturnsVersions() {
        val versions: VersionRange =
            VersionRegistry.getSourceVersions("WEATHER.HOURS.3.IS_AVAILABLE")

        val expectedVersions = VersionRange(2)

        assertEquals(expectedVersions, versions)
    }

    @Test
    fun sourcesVersionsWithDifferentIndicesPointHaveTheSameVersioning() {
        val indexThree: VersionRange =
            VersionRegistry.getSourceVersions("WEATHER.HOURS.3.IS_AVAILABLE")
        val indexFour: VersionRange =
            VersionRegistry.getSourceVersions("WEATHER.HOURS.4.IS_AVAILABLE")

        assertEquals(indexThree, indexFour)
    }
}
