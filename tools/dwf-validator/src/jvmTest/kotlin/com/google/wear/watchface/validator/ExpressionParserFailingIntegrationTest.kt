package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.expression.EndOfFileException
import com.google.wear.watchface.validator.expression.ExpressionParser
import com.google.wear.watchface.validator.expression.ExpressionParsingException
import com.google.wear.watchface.validator.expression.FunctionNotFoundException
import com.google.wear.watchface.validator.expression.MissingTokenException
import com.google.wear.watchface.validator.expression.TokensNotConsumedException
import com.google.wear.watchface.validator.expression.UnexpectedTokenException
import com.google.wear.watchface.validator.expression.UnknownTokenException
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpressionParserFailingIntegrationTest(
    val expectedExceptionClass: KClass<out ExpressionParsingException>,
    val testCaseString: String,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: \"{1}\" = \"{0}\"")
        fun testCases(): List<Array<Any>> =
            listOf(
                arrayOf(EndOfFileException::class, ""),
                arrayOf(UnexpectedTokenException::class, "3 - - -5"),
                arrayOf(TokensNotConsumedException::class, "3e-1 + 7.0"),
                arrayOf(TokensNotConsumedException::class, "9 + 3e-1"),
                arrayOf(UnexpectedTokenException::class, "!!0"),
                arrayOf(UnknownTokenException::class, "\"lorem"),
                arrayOf(EndOfFileException::class, "+"),
                arrayOf(EndOfFileException::class, "-"),
                arrayOf(EndOfFileException::class, "!"),
                arrayOf(EndOfFileException::class, "~"),
                arrayOf(EndOfFileException::class, "1 + "),
                arrayOf(EndOfFileException::class, "1 * "),
                arrayOf(EndOfFileException::class, "1 == "),
                arrayOf(UnknownTokenException::class, "\$£`£"),
                arrayOf(UnknownTokenException::class, "#fff"),
                arrayOf(UnknownTokenException ::class, "#00808g"),
                arrayOf(UnexpectedTokenException::class, "(++2)"),
                arrayOf(UnexpectedTokenException::class, "+ +2"),
                arrayOf(UnexpectedTokenException::class, "+-2"),
                arrayOf(UnexpectedTokenException::class, "-+2"),
                arrayOf(UnexpectedTokenException::class, "(--2)"),
                arrayOf(UnexpectedTokenException::class, "- -2"),
                arrayOf(TokensNotConsumedException::class, ".3f"),
                arrayOf(TokensNotConsumedException::class, "3f"),
                arrayOf(TokensNotConsumedException::class, "3F"),
                arrayOf(TokensNotConsumedException::class, "3D"),
                arrayOf(TokensNotConsumedException::class, "3d"),
                arrayOf(TokensNotConsumedException::class, "3e-1"),
                arrayOf(TokensNotConsumedException::class, "2fnothing"),
                arrayOf(TokensNotConsumedException::class, "3dnothing"),
                arrayOf(TokensNotConsumedException::class, "1lorem"),
                arrayOf(TokensNotConsumedException::class, "3 5 asd"),
                arrayOf(TokensNotConsumedException::class, "2147483647L"),
                arrayOf(
                    FunctionNotFoundException::class,
                    "extractColorFromWeightedColors(#FF0000 #000000 #FF00FF,1, 1, true, 0.6)",
                ),
                arrayOf(
                    FunctionNotFoundException::class,
                    "completelyMadeUpFunction(argument, another)",
                ),
            )
    }

    @Test
    fun testExpressionParsing() {
        assertEquals(
            expectedExceptionClass,
            assertFailsWith<ExpressionParsingException> {
                ExpressionParser.parse(testCaseString)
            }::class,
        )
    }
}
