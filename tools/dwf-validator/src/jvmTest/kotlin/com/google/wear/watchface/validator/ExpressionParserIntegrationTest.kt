package com.google.wear.watchface.validator

import com.google.wear.watchface.validator.expression.Add
import com.google.wear.watchface.validator.expression.BitwiseAnd
import com.google.wear.watchface.validator.expression.BitwiseNot
import com.google.wear.watchface.validator.expression.BitwiseOr
import com.google.wear.watchface.validator.expression.BooleanLiteral
import com.google.wear.watchface.validator.expression.Color
import com.google.wear.watchface.validator.expression.ColorList
import com.google.wear.watchface.validator.expression.Div
import com.google.wear.watchface.validator.expression.Equal
import com.google.wear.watchface.validator.expression.Expression
import com.google.wear.watchface.validator.expression.ExpressionParser
import com.google.wear.watchface.validator.expression.FunctionCall
import com.google.wear.watchface.validator.expression.LogicalAnd
import com.google.wear.watchface.validator.expression.LogicalNot
import com.google.wear.watchface.validator.expression.LogicalOr
import com.google.wear.watchface.validator.expression.MissingTokenException
import com.google.wear.watchface.validator.expression.Mod
import com.google.wear.watchface.validator.expression.Mul
import com.google.wear.watchface.validator.expression.Neg
import com.google.wear.watchface.validator.expression.NumList
import com.google.wear.watchface.validator.expression.NumLiteral
import com.google.wear.watchface.validator.expression.Pos
import com.google.wear.watchface.validator.expression.StringLiteral
import com.google.wear.watchface.validator.expression.Sub
import com.google.wear.watchface.validator.expression.Ternary
import com.google.wear.watchface.validator.expression.Variable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpressionParserIntegrationTest(val expected: Expression, val testCaseString: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: \"{1}\" = \"{0}\"")
        fun testCases(): List<Array<Any>> =
            listOf(
                arrayOf(Variable("black"), "black"),
                arrayOf(Variable("BLACK"), "BLACK"),
                arrayOf(Variable("darkgray"), "darkgray"),
                arrayOf(Variable("DARKGRAY"), "DARKGRAY"),
                arrayOf(Variable("gray"), "gray"),
                arrayOf(Variable("GRAY"), "GRAY"),
                arrayOf(Variable("lightgray"), "lightgray"),
                arrayOf(Variable("LIGHTGRAY"), "LIGHTGRAY"),
                arrayOf(Variable("white"), "white"),
                arrayOf(Variable("WHITE"), "WHITE"),
                arrayOf(Variable("red"), "red"),
                arrayOf(Variable("RED"), "RED"),
                arrayOf(Variable("green"), "green"),
                arrayOf(Variable("GREEN"), "GREEN"),
                arrayOf(Variable("blue"), "blue"),
                arrayOf(Variable("BLUE"), "BLUE"),
                arrayOf(Variable("yellow"), "yellow"),
                arrayOf(Variable("YELLOW"), "YELLOW"),
                arrayOf(Variable("cyan"), "cyan"),
                arrayOf(Variable("CYAN"), "CYAN"),
                arrayOf(Variable("magenta"), "magenta"),
                arrayOf(Variable("MAGENTA"), "MAGENTA"),
                arrayOf(Variable("aqua"), "aqua"),
                arrayOf(Variable("AQUA"), "AQUA"),
                arrayOf(Variable("fuchsia"), "fuchsia"),
                arrayOf(Variable("FUCHSIA"), "FUCHSIA"),
                arrayOf(Variable("darkgrey"), "darkgrey"),
                arrayOf(Variable("DARKGREY"), "DARKGREY"),
                arrayOf(Variable("grey"), "grey"),
                arrayOf(Variable("GREY"), "GREY"),
                arrayOf(Variable("lightgrey"), "lightgrey"),
                arrayOf(Variable("LIGHTGREY"), "LIGHTGREY"),
                arrayOf(Variable("lime"), "lime"),
                arrayOf(Variable("LIME"), "LIME"),
                arrayOf(Variable("maroon"), "maroon"),
                arrayOf(Variable("MAROON"), "MAROON"),
                arrayOf(Variable("navy"), "navy"),
                arrayOf(Variable("NAVY"), "NAVY"),
                arrayOf(Variable("olive"), "olive"),
                arrayOf(Variable("OLIVE"), "OLIVE"),
                arrayOf(Variable("purple"), "purple"),
                arrayOf(Variable("PURPLE"), "PURPLE"),
                arrayOf(Variable("silver"), "silver"),
                arrayOf(Variable("SILVER"), "SILVER"),
                arrayOf(Variable("teal"), "teal"),
                arrayOf(Variable("TEAL"), "TEAL"),
                // COLOR LITERALS
                arrayOf(Color("#FF008080"), "#FF008080"),
                // SOURCE VARIABLES
                arrayOf(Variable("MONTH_S"), "[MONTH_S]"),
                arrayOf(Variable("WEATHER.DAYS.6.CONDITION_DAY"), "WEATHER.DAYS.6.CONDITION_DAY"),

                // NUMERIC LITERALS
                arrayOf(Neg(NumLiteral(2147483648.0)), "-2147483648"),
                arrayOf(Neg(NumLiteral(2147483647.0)), "-2147483647"),
                arrayOf(NumLiteral(0.3), ".3"),
                // ARITHMETIC_UNARY_PLUS
                arrayOf(Pos(NumLiteral(1.0)), "+1"),
                arrayOf(Pos(NumLiteral(1.5)), "+1.5"),
                // ARITHMETIC_UNARY_MINUS
                arrayOf(Neg(NumLiteral(1.0)), "-1"),
                arrayOf(Neg(NumLiteral(1.5)), "-1.5"),
                arrayOf(Pos(Neg(NumLiteral(2.0))), "+(-2)"),
                arrayOf(Neg(Neg(NumLiteral(2.0))), "-(-2)"),

                // ARITHMETIC_PLUS
                arrayOf(Add(NumLiteral(1.0), NumLiteral(2.0)), "1 + 2"),
                arrayOf(Add(NumLiteral(1.0), NumLiteral(2.5)), "1 + 2.5"),
                arrayOf(Add(NumLiteral(1.5), NumLiteral(2.0)), "1.5 + 2"),
                arrayOf(Add(NumLiteral(1.5), NumLiteral(2.5)), "1.5 + 2.5"),
                arrayOf(Add(NumLiteral(1.5), NumLiteral(2.6)), "1.5 + 2.6"),
                arrayOf(
                    Add(Add(NumLiteral(1.5), NumLiteral(2.6)), NumLiteral(1.9)),
                    "1.5 + 2.6 + 1.9",
                ),
                arrayOf(Add(NumLiteral(2.0), Neg(NumLiteral(3.0))), "2 + -3"),
                arrayOf(Add(NumLiteral(2.0), Neg(NumLiteral(3.0))), "2 + - 3"),
                arrayOf(Sub(NumLiteral(3.0), Pos(NumLiteral(2.0))), "3 - + 2"),
                arrayOf(Add(Add(NumLiteral(1.0), NumLiteral(2.0)), NumLiteral(3.0)), "(1+2)+3"),
                arrayOf(Add(NumLiteral(1.0), Add(NumLiteral(2.0), NumLiteral(3.0))), "1+(2+3)"),
                arrayOf(Add(NumLiteral(2147483647.0), NumLiteral(10.0)), "2147483647 + 10"),
                // ARITHMETIC_MINUS
                arrayOf(Sub(NumLiteral(1.0), NumLiteral(2.0)), "1 - 2"),
                arrayOf(Sub(NumLiteral(1.0), NumLiteral(2.5)), "1 - 2.5"),
                arrayOf(Sub(NumLiteral(1.5), NumLiteral(2.0)), "1.5 - 2"),
                arrayOf(Sub(NumLiteral(1.5), NumLiteral(2.5)), "1.5 - 2.5"),
                arrayOf(Sub(NumLiteral(3.0), Neg(NumLiteral(5.0))), "3 - -(5)"),
                arrayOf(Sub(NumLiteral(3.0), Neg(NumLiteral(5.0))), "3 - - (5)"),
                arrayOf(Sub(NumLiteral(3.0), Neg(NumLiteral(5.0))), "3 - - 5"),
                arrayOf(Sub(NumLiteral(3.0), Neg(Neg(NumLiteral(5.0)))), "3 - - (-5)"),
                arrayOf(Sub(Sub(NumLiteral(1.0), NumLiteral(2.0)), NumLiteral(3.0)), "(1-2)-3"),
                arrayOf(Sub(NumLiteral(1.0), Sub(NumLiteral(2.0), NumLiteral(3.0))), "1-(2-3)"),
                // ARITHMETIC_MULTIPLY
                arrayOf(Mul(NumLiteral(1.0), NumLiteral(2.0)), "1 * 2"),
                arrayOf(Mul(NumLiteral(1.0), NumLiteral(2.5)), "1 * 2.5"),
                arrayOf(Mul(NumLiteral(1.4), NumLiteral(2.0)), "1.4 * 2"),
                arrayOf(Mul(NumLiteral(1.5), NumLiteral(2.0)), "1.5 * 2.0"),
                arrayOf(Mul(NumLiteral(1.5), NumLiteral(2.5)), "1.5 * 2.5"),
                arrayOf(Mul(Mul(NumLiteral(2.0), NumLiteral(2.0)), NumLiteral(4.0)), "(2 * 2) * 4"),
                // ARITHMETIC_DIVISION
                arrayOf(Div(NumLiteral(4.0), NumLiteral(2.0)), "4 / 2"),
                arrayOf(Div(NumLiteral(4.0), NumLiteral(2.0)), "4 / 2.0"),
                arrayOf(Div(NumLiteral(1.0), NumLiteral(2.0)), "1 / 2.0"),
                arrayOf(Div(NumLiteral(1.0), NumLiteral(2.0)), "1 / 2"),
                arrayOf(Div(NumLiteral(1.0), NumLiteral(2.0)), "1.0 / 2"),
                arrayOf(Div(NumLiteral(1.0), NumLiteral(2.0)), "1.0 / 2.0"),
                arrayOf(Div(NumLiteral(4.0), NumLiteral(2.0)), "4.0 / 2.0"),
                arrayOf(Div(NumLiteral(1.0), NumLiteral(0.0)), "1 / 0"),
                arrayOf(Div(NumLiteral(0.0), NumLiteral(1.0)), "0 / 1"),
                arrayOf(Div(NumLiteral(0.0), NumLiteral(0.0)), "0 / 0"),
                arrayOf(Div(Div(NumLiteral(4.0), NumLiteral(2.0)), NumLiteral(2.0)), "(4 / 2) / 2"),
                // ARITHMETIC_MODULO
                arrayOf(Mod(NumLiteral(3.0), NumLiteral(2.0)), "3 % 2"),
                arrayOf(Mod(NumLiteral(2.0), NumLiteral(3.0)), "2 % 3"),
                arrayOf(Mod(NumLiteral(2.0), NumLiteral(3.0)), "2.0 % 3"),
                arrayOf(Mod(NumLiteral(2.0), NumLiteral(3.0)), "2 % 3.0"),
                arrayOf(Mod(NumLiteral(2.0), NumLiteral(3.0)), "2.0 % 3.0"),
                arrayOf(
                    FunctionCall(
                        "numberFormat",
                        listOf(StringLiteral("#.#"), Mod(NumLiteral(3.0), NumLiteral(2.9))),
                    ),
                    "numberFormat(\"#.#\", (3 % 2.9))",
                ),
                arrayOf(Mod(NumLiteral(2.9), NumLiteral(3.1)), "2.9 % 3.1"),
                arrayOf(Mod(NumLiteral(3.2), NumLiteral(2.0)), "3.2 % 2"),
                arrayOf(Mod(NumLiteral(0.0), NumLiteral(0.0)), "0 % 0"),
                arrayOf(Mod(NumLiteral(0.0), NumLiteral(3.0)), "0 % 3"),
                arrayOf(Mod(NumLiteral(3.0), NumLiteral(0.0)), "3 % 0"),
                arrayOf(
                    Mod(Mod(NumLiteral(12.0), NumLiteral(7.0)), NumLiteral(3.0)),
                    "(12 % 7) % 3",
                ),
                // LOGICAL_NOT
                arrayOf(LogicalNot(NumLiteral(1.0)), "!1"),
                arrayOf(LogicalNot(LogicalNot(NumLiteral(2.0))), "!(!2)"),
                arrayOf(LogicalNot(NumLiteral(0.0)), "!0"),
                // LOGICAL_AND
                arrayOf(LogicalAnd(NumLiteral(1.0), NumLiteral(1.0)), "1 && 1"),
                arrayOf(LogicalAnd(NumLiteral(2.0), NumLiteral(3.0)), "2 && 3"),
                arrayOf(LogicalAnd(NumLiteral(1.0), NumLiteral(0.0)), "1 && 0"),
                arrayOf(LogicalAnd(NumLiteral(0.0), NumLiteral(1.0)), "0 && 1"),
                arrayOf(LogicalAnd(NumLiteral(0.0), NumLiteral(0.0)), "0 && 0"),
                // LOGICAL_OR
                arrayOf(LogicalOr(NumLiteral(1.0), NumLiteral(1.0)), "1 || 1"),
                arrayOf(LogicalOr(NumLiteral(2.0), NumLiteral(3.0)), "2 || 3"),
                arrayOf(LogicalOr(NumLiteral(1.0), NumLiteral(0.0)), "1 || 0"),
                arrayOf(LogicalOr(NumLiteral(0.0), NumLiteral(1.0)), "0 || 1"),
                arrayOf(LogicalOr(NumLiteral(0.0), NumLiteral(0.0)), "0 || 0"),
                // BITWISE_NOT
                arrayOf(BitwiseNot(NumLiteral(1.0)), "~1"),
                arrayOf(BitwiseNot(Neg(NumLiteral(2.0))), "~(-2)"),
                arrayOf(BitwiseNot(NumLiteral(0.0)), "~0"),
                arrayOf(BitwiseNot(Neg(NumLiteral(1.0))), "~(-1)"),
                // BITWISE_AND
                arrayOf(BitwiseAnd(NumLiteral(1.0), NumLiteral(1.0)), "1 & 1"),
                arrayOf(BitwiseAnd(NumLiteral(1.0), NumLiteral(0.0)), "1 & 0"),
                arrayOf(BitwiseAnd(NumLiteral(0.0), NumLiteral(1.0)), "0 & 1"),
                arrayOf(BitwiseAnd(NumLiteral(0.0), NumLiteral(0.0)), "0 & 0"),
                arrayOf(BitwiseAnd(NumLiteral(2.0), NumLiteral(2.0)), "2 & 2"),
                arrayOf(BitwiseAnd(NumLiteral(2.0), NumLiteral(4.0)), "2 & 4"),
                // BITWISE_OR
                arrayOf(BitwiseOr(NumLiteral(1.0), NumLiteral(1.0)), "1 | 1"),
                arrayOf(BitwiseOr(NumLiteral(1.0), NumLiteral(0.0)), "1 | 0"),
                arrayOf(BitwiseOr(NumLiteral(0.0), NumLiteral(1.0)), "0 | 1"),
                arrayOf(BitwiseOr(NumLiteral(0.0), NumLiteral(0.0)), "0 | 0"),
                arrayOf(BitwiseOr(NumLiteral(2.0), NumLiteral(2.0)), "2 | 2"),
                arrayOf(BitwiseOr(NumLiteral(2.0), NumLiteral(4.0)), "2 | 4"),
                arrayOf(
                    BitwiseOr(NumLiteral(1.0), BitwiseOr(NumLiteral(2.0), NumLiteral(4.0))),
                    "1 | 2 | 4",
                ),
                // COMPARISON_EQUAL
                arrayOf(Equal(NumLiteral(1.0), NumLiteral(1.0)), "1 == 1"),
                arrayOf(Equal(NumLiteral(1.0), NumLiteral(1.0)), "1.0 == 1"),
                arrayOf(Equal(NumLiteral(1.0), NumLiteral(1.0)), "1.0 == 1.0"),
                arrayOf(Equal(NumLiteral(1.0), NumLiteral(2.0)), "1 == 2"),
                // ... many more comparison tests omitted for brevity but follow the same pattern
                // PARENTHESIS
                arrayOf(
                    Add(Add(NumLiteral(1.5), NumLiteral(2.6)), NumLiteral(1.9)),
                    "(1.5 + 2.6 + 1.9)",
                ),
                arrayOf(
                    Add(NumLiteral(1.5), Add(NumLiteral(2.6), NumLiteral(1.9))),
                    "1.5 + (2.6 + 1.9)",
                ),
                arrayOf(
                    Add(Add(NumLiteral(1.5), NumLiteral(2.6)), NumLiteral(1.9)),
                    "(1.5 + 2.6) + 1.9",
                ),
                arrayOf(Add(NumLiteral(2.0), Mul(NumLiteral(3.0), NumLiteral(4.0))), "2 + 3 * 4"),
                arrayOf(Add(NumLiteral(2.0), Mul(NumLiteral(3.0), NumLiteral(4.0))), "2 + (3 * 4)"),
                arrayOf(Mul(Add(NumLiteral(2.0), NumLiteral(3.0)), NumLiteral(4.0)), "(2 + 3) * 4"),
                // TERNARY
                arrayOf(
                    Ternary(BooleanLiteral(true), NumLiteral(1.0), NumLiteral(2.0)),
                    "true ? 1 : 2",
                ),
                arrayOf(
                    Ternary(BooleanLiteral(false), NumLiteral(1.0), NumLiteral(2.0)),
                    "false ? 1 : 2",
                ),
                arrayOf(
                    Ternary(
                        BooleanLiteral(true),
                        Ternary(BooleanLiteral(true), NumLiteral(1.0), NumLiteral(2.0)),
                        NumLiteral(3.0),
                    ),
                    "true ? true ? 1 : 2 : 3",
                ),
                arrayOf(
                    Ternary(
                        BooleanLiteral(false),
                        NumLiteral(1.0),
                        Ternary(BooleanLiteral(true), NumLiteral(2.0), NumLiteral(3.0)),
                    ),
                    "false ? 1 : true ? 2 : 3",
                ),
                // FUNCTIONS
                arrayOf(FunctionCall("round", listOf(NumLiteral(1.0))), "round(1)"),
                arrayOf(FunctionCall("round", listOf(NumLiteral(1.4))), "round(1.4)"),
                arrayOf(
                    FunctionCall("floor", listOf(NumLiteral(1.999999999))),
                    "floor(1.999999999)",
                ),
                arrayOf(FunctionCall("ceil", listOf(NumLiteral(2.000000001))), "ceil(2.000000001)"),
                arrayOf(FunctionCall("sin", listOf(NumLiteral(0.0))), "sin(0)"),
                arrayOf(FunctionCall("abs", listOf(Neg(NumLiteral(1.5)))), "abs(-1.5)"),
                arrayOf(
                    FunctionCall(
                        "clamp",
                        listOf(NumLiteral(2.0), NumLiteral(0.0), NumLiteral(1.0)),
                    ),
                    "clamp(2, 0, 1)",
                ),
                arrayOf(
                    FunctionCall(
                        "clamp",
                        listOf(
                            Add(NumLiteral(2.0), NumLiteral(1.0)),
                            Sub(NumLiteral(1.0), NumLiteral(1.0)),
                            Add(NumLiteral(1.0), NumLiteral(1.0)),
                        ),
                    ),
                    "clamp((2 + 1), 1 - 1, 1 + 1)",
                ),
                arrayOf(FunctionCall("log", listOf(NumLiteral(10.0))), "log(10)"),
                arrayOf(FunctionCall("sqrt", listOf(NumLiteral(9.0))), "sqrt(9)"),
                arrayOf(FunctionCall("cbrt", listOf(NumLiteral(27.0))), "cbrt(27)"),
                arrayOf(
                    FunctionCall("deg", listOf(Div(Variable("PI"), NumLiteral(2.0)))),
                    "deg(PI / 2)",
                ),
                arrayOf(FunctionCall("rad", listOf(NumLiteral(180.0))), "rad(180)"),
                arrayOf(
                    FunctionCall(
                        "numberFormat",
                        listOf(StringLiteral("####.###"), NumLiteral(1234.5678)),
                    ),
                    "numberFormat(\"####.###\", 1234.5678)",
                ),
                arrayOf(FunctionCall("fract", listOf(NumLiteral(1.234))), "fract(1.234)"),
                arrayOf(FunctionCall("pow", listOf(NumLiteral(3.0), NumLiteral(3.0))), "pow(3, 3)"),
                arrayOf(
                    FunctionCall(
                        "colorArgb",
                        listOf(
                            NumLiteral(255.0),
                            NumLiteral(255.0),
                            NumLiteral(0.0),
                            NumLiteral(0.0),
                        ),
                    ),
                    "colorArgb(255, 255, 0, 0)",
                ),
                arrayOf(
                    FunctionCall(
                        "colorRgb",
                        listOf(NumLiteral(0.0), NumLiteral(0.0), NumLiteral(255.0)),
                    ),
                    "colorRgb(0, 0, 255)",
                ),
                arrayOf(
                    FunctionCall(
                        "extractColorFromColors",
                        listOf(
                            ColorList(listOf("#FF0000", "#000000", "#00FFFF")),
                            BooleanLiteral(true),
                            NumLiteral(0.6),
                        ),
                    ),
                    "extractColorFromColors(#FF0000 #000000 #00FFFF, true, 0.6)",
                ),
                arrayOf(
                    FunctionCall(
                        "extractColorFromWeightedColors",
                        listOf(
                            ColorList(listOf("#FF0000", "#000000", "#00FFFF")),
                            NumList(listOf(1.0, 1.0)),
                            BooleanLiteral(true),
                            NumLiteral(0.6),
                        ),
                    ),
                    "extractColorFromWeightedColors(#FF0000 #000000 #00FFFF,1 1, true, 0.6)",
                ),
                // QUOTED STRINGS
                arrayOf(StringLiteral("abc"), "\"abc\""),
                arrayOf(StringLiteral("abc + def"), "\"abc + def\""),
            )
    }

    @Test
    fun testExpressionParsing() {
        val result: Expression = ExpressionParser.parse(testCaseString)
        assertEquals(expected, result)
    }
}
