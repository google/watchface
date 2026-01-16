package com.google.wear.watchface.validator.expression

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExpressionParserTest {
    @Test
    fun emptyExpressionShouldFail() {
        val tokens = sequenceOf(Token.EOF)

        val exception = assertFailsWith<EndOfFileException> { ExpressionParser.parse(tokens) }

        assertEquals("Unexpected End of Expression", exception.message)
    }

    @Test
    fun singleUnknownTokensShouldFail() {
        val tokens = sequenceOf(Token.Unknown("@"), Token.EOF)

        val exception = assertFailsWith<UnknownTokenException> { ExpressionParser.parse(tokens) }

        assertEquals("Unrecognised Token: @", exception.message)
    }

    @Test
    fun embeddedUnknownTokensShouldFail() {
        val tokens =
            sequenceOf(Token.Number("3"), Token.Operator("+"), Token.Unknown("@"), Token.EOF)
        val exception = assertFailsWith<UnknownTokenException> { ExpressionParser.parse(tokens) }
        assertEquals("Unrecognised Token: @", exception.message)
    }

    @Test
    fun numericLiteralsShouldParse() {
        val tokens = sequenceOf(Token.Number("3"), Token.EOF)
        val expected: Expression = NumLiteral(3.0)

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun variablesShouldParse() {
        val tokens = sequenceOf(Token.Word("test"), Token.EOF)
        val expected: Expression = Variable("test")

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun functionsShouldParse() {
        val tokens =
            sequenceOf(
                Token.Word("round"),
                Token.Paren("("),
                Token.Number("3.9"),
                Token.Paren(")"),
                Token.EOF,
            )
        val expected: Expression = FunctionCall("round", listOf(NumLiteral(3.9)))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun basicArithmeticExpressionsShouldParse() {
        val tokens =
            sequenceOf(Token.Number("3"), Token.Operator("+"), Token.Number("5"), Token.EOF)
        val expected: Expression = Add(NumLiteral(3.0), NumLiteral(5.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun nonExistentFunctionShouldFail() {
        val tokens =
            sequenceOf(
                Token.Word("nonExistentFunction"),
                Token.Paren("("),
                Token.Number("3.9"),
                Token.Paren(")"),
                Token.EOF,
            )

        val exception =
            assertFailsWith<FunctionNotFoundException> { ExpressionParser.parse(tokens) }
    }

    @Test
    fun functionWithIncorrectArityShouldFail() {
        val tokens =
            sequenceOf(
                Token.Word("round"),
                Token.Paren("("),
                Token.Number("3.9"),
                Token.Operator(","),
                Token.Number("2.1"),
                Token.Paren(")"),
                Token.EOF,
            )

        val exception =
            assertFailsWith<FunctionNotFoundException> { ExpressionParser.parse(tokens) }
    }

    @Test
    fun mulShouldParseWithHigherPrecedenceThanAdd() {
        val tokens =
            sequenceOf(
                Token.Number("3"),
                Token.Operator("+"),
                Token.Number("5"),
                Token.Operator("*"),
                Token.Number("5"),
                Token.EOF,
            )
        val expected: Expression = Add(NumLiteral(3.0), Mul(NumLiteral(5.0), NumLiteral(5.0)))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun parserShouldPreserveOrder() {
        val tokens =
            sequenceOf(
                Token.Number("5"),
                Token.Operator("*"),
                Token.Number("5"),
                Token.Operator("+"),
                Token.Number("3"),
                Token.EOF,
            )
        val expected: Expression = Add(Mul(NumLiteral(5.0), NumLiteral(5.0)), NumLiteral(3.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun redundantBracketsShouldDoNothing() {
        val tokens =
            sequenceOf(
                Token.Paren("("),
                Token.Number("5"),
                Token.Operator("*"),
                Token.Number("5"),
                Token.Paren(")"),
                Token.Operator("+"),
                Token.Number("3"),
                Token.EOF,
            )
        val expected: Expression = Add(Mul(NumLiteral(5.0), NumLiteral(5.0)), NumLiteral(3.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun necessaryBracketsShouldChangeOrderOfOperations() {
        val tokens =
            sequenceOf(
                Token.Paren("("),
                Token.Number("5"),
                Token.Operator("+"),
                Token.Number("5"),
                Token.Paren(")"),
                Token.Operator("*"),
                Token.Number("3"),
                Token.EOF,
            )
        val expected: Expression = Mul(Add(NumLiteral(5.0), NumLiteral(5.0)), NumLiteral(3.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun unaryOperatorsHaveTheHighestPrecedence() {
        val tokens =
            sequenceOf(
                Token.Number("5"),
                Token.Operator("+"),
                Token.Number("5"),
                Token.Operator("*"),
                Token.Operator("-"),
                Token.Number("3"),
                Token.EOF,
            )
        val expected: Expression = Add(NumLiteral(5.0), Mul(NumLiteral(5.0), Neg(NumLiteral(3.0))))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun equalityOperationsShouldParse() {
        val tokens =
            sequenceOf(Token.Number("3"), Token.Operator("=="), Token.Number("5"), Token.EOF)
        val expected: Expression = Equal(NumLiteral(3.0), NumLiteral(5.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun equalityOperationsShouldNotChain() {
        val tokens =
            sequenceOf(
                Token.Number("3"),
                Token.Operator("=="),
                Token.Number("5"),
                Token.Operator("=="),
                Token.Number("5"),
                Token.EOF,
            )

        val exception =
            assertFailsWith<TokensNotConsumedException> { ExpressionParser.parse(tokens) }

        assertEquals("Expression Parser Finished Early: == not consumed", exception.message)
    }

    @Test
    fun comparisonOperationsShouldParse() {
        val tokens =
            sequenceOf(Token.Number("3"), Token.Operator("<="), Token.Number("5"), Token.EOF)
        val expected: Expression = LessThanOrEqual(NumLiteral(3.0), NumLiteral(5.0))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }

    @Test
    fun comparisonOperationsShouldNotChain() {
        val tokens =
            sequenceOf(
                Token.Number("3"),
                Token.Operator("<="),
                Token.Number("5"),
                Token.Operator("<"),
                Token.Number("5"),
                Token.EOF,
            )

        val exception =
            assertFailsWith<TokensNotConsumedException> { ExpressionParser.parse(tokens) }

        assertEquals("Expression Parser Finished Early: < not consumed", exception.message)
    }

    @Test
    fun chainableOperationsShouldBeRightAssociative() {
        val tokens =
            sequenceOf(
                Token.Number("3"),
                Token.Operator("&"),
                Token.Number("5"),
                Token.Operator("&"),
                Token.Number("5"),
                Token.EOF,
            )
        val expected = BitwiseAnd(NumLiteral(3.0), BitwiseAnd(NumLiteral(5.0), NumLiteral(5.0)))

        val result: Expression = ExpressionParser.parse(tokens)

        assertEquals(expected, result)
    }
}
