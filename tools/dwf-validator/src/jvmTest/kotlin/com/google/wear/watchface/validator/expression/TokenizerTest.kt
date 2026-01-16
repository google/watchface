package com.google.wear.watchface.validator.expression

import kotlin.test.Test
import kotlin.test.assertEquals

class TokenizerTest {
    @Test
    fun whiteSpacesTokeniseToEmptyList() {
        val expression = "      "

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(emptyList(), tokens)
    }

    @Test
    fun numbersTokenisedAsNumber() {
        val expression = "123"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Number("123")), tokens)
    }

    @Test
    fun numbersCanHaveDecimalPoints() {
        val expression = "12.3"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Number("12.3")), tokens)
    }

    @Test
    fun wordsTokeniseCorrectly() {
        val expression = "ACCELEROMETER_ANGLE_X"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Word("ACCELEROMETER_ANGLE_X")), tokens)
    }

    @Test
    fun wordsCanHaveDotsInThem() {
        val expression = "REFERENCE.headerPosition"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Word("REFERENCE.headerPosition")), tokens)
    }

    @Test
    fun operatorsTokenisedAsOperators() {
        val expression = "+"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Operator("+")), tokens)
    }

    @Test
    fun invalidCharactersTokenisedAsUnknown() {
        val expression = "{}"

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(listOf(Token.Unknown("{"), Token.Unknown("}")), tokens)
    }

    @Test
    fun arithmeticExpressionTokenises() {
        val expression = "5 + 3 * 2"
        val expectedTokens =
            listOf(
                Token.Number("5"),
                Token.Operator("+"),
                Token.Number("3"),
                Token.Operator("*"),
                Token.Number("2"),
            )

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(expectedTokens, tokens)
    }

    @Test
    fun arithmeticExpressionsWithBracketsTokenises() {
        val expression = "(5 + 3) * 2"
        val expectedTokens =
            listOf(
                Token.Paren("("),
                Token.Number("5"),
                Token.Operator("+"),
                Token.Number("3"),
                Token.Paren(")"),
                Token.Operator("*"),
                Token.Number("2"),
            )

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(expectedTokens, tokens)
    }

    @Test
    fun completeExpressionTokenises() {
        val expression =
            "(5/90)*clamp([ACCELEROMETER_ANGLE_X],0,90) + (-5/90)*clamp([ACCELEROMETER_ANGLE_X],-90,0)"
        val expectedTokens =
            listOf(
                Token.Paren("("),
                Token.Number("5"),
                Token.Operator("/"),
                Token.Number("90"),
                Token.Paren(")"),
                Token.Operator("*"),
                Token.Word("clamp"),
                Token.Paren("("),
                Token.Paren("["),
                Token.Word("ACCELEROMETER_ANGLE_X"),
                Token.Paren("]"),
                Token.Operator(","),
                Token.Number("0"),
                Token.Operator(","),
                Token.Number("90"),
                Token.Paren(")"),
                Token.Operator("+"),
                Token.Paren("("),
                Token.Operator("-"),
                Token.Number("5"),
                Token.Operator("/"),
                Token.Number("90"),
                Token.Paren(")"),
                Token.Operator("*"),
                Token.Word("clamp"),
                Token.Paren("("),
                Token.Paren("["),
                Token.Word("ACCELEROMETER_ANGLE_X"),
                Token.Paren("]"),
                Token.Operator(","),
                Token.Operator("-"),
                Token.Number("90"),
                Token.Operator(","),
                Token.Number("0"),
                Token.Paren(")"),
            )

        val tokens = Tokenizer.tokeniseString(expression).toList()

        assertEquals(expectedTokens, tokens)
    }
}
