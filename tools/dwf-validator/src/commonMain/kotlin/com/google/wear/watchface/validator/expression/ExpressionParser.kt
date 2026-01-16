package com.google.wear.watchface.validator.expression

import com.google.wear.watchface.validator.MAX_WFF_VERSION
import com.google.wear.watchface.validator.MIN_WFF_VERSION
import com.google.wear.watchface.validator.WatchFaceElementContext
import com.google.wear.watchface.validator.error.ExpressionSyntaxError
import com.google.wear.watchface.validator.error.ValidationResult
import kotlin.math.max
import kotlin.math.min

typealias TokenStream = Sequence<Token>

/** A parser for expressions embedded in the xml elements */
class ExpressionParser(tokens: TokenStream, val ctx: WatchFaceElementContext) {
    private val tokenIterator: Iterator<Token> = tokens.iterator()
    private var minVersion: Int = MIN_WFF_VERSION
    private var maxVersion: Int = MAX_WFF_VERSION
    private var current: Token
    private var previous: Token

    init {
        require(tokenIterator.hasNext())
        current = tokenIterator.next()
        previous = current

        if (current is Token.Unknown)
            throw UnknownTokenException("Unrecognised Token: ${current.value}")
    }

    /**
     * The top level function for parsing the sequence of tokens.
     *
     * This function will parse the tokens into an [Expression] object. Preserving the
     * precedence/order of operations. Operations are naturally right associative ie. `1 + 2 + 3` is
     * parsed as `1 + (2 + 3)`.
     */
    private fun parse(): Expression {
        val expr = expression()

        /* All the tokens should have been consumed */
        if (current !is Token.EOF)
            throw TokensNotConsumedException(
                "Expression Parser Finished Early: ${current.value} not consumed"
            )

        return expr
    }

    /* (Ternary) Expression -> OrExpression ('?' Expression ':' Expression)? */
    private fun expression(): Expression {
        val expr: Expression = orExpression()

        return when {
            match("?") -> {
                val ifBody: Expression = expression()
                expect(":")
                val elseBody: Expression = expression()
                Ternary(expr, ifBody, elseBody)
            }

            else -> expr
        }
    }

    /* OrExpression -> AndExpression ('||' AndExpression)* */
    private fun orExpression(): Expression {
        var expr: Expression = andExpression()

        while (match("||")) {
            val right: Expression = orExpression()
            expr = LogicalOr(expr, right)
        }

        return expr
    }

    /* AndExpression -> BitWiseOr ('&&' BitWiseOr)* */
    private fun andExpression(): Expression {
        var expr: Expression = bitwiseOr()

        while (match("&&")) {
            val right: Expression = andExpression()
            expr = LogicalAnd(expr, right)
        }

        return expr
    }

    /* BitWiseOr -> BitWiseAnd ('|' BitWiseAnd)* */
    private fun bitwiseOr(): Expression {
        var expr: Expression = bitwiseAnd()

        while (match("|")) {
            val right: Expression = bitwiseOr()
            expr = BitwiseOr(expr, right)
        }

        return expr
    }

    /* BitWiseAnd -> Equality ('&' Equality)* */
    private fun bitwiseAnd(): Expression {
        var expr: Expression = equality()

        while (match("&")) {
            val right: Expression = bitwiseAnd()
            expr = BitwiseAnd(expr, right)
        }

        return expr
    }

    /* Equality -> Comparison (('=='|'!=') Comparison)? */
    private fun equality(): Expression {
        val expr: Expression = comparison()

        return when {
            match("==") -> Equal(expr, comparison())
            match("!=") -> NotEqual(expr, comparison())

            else -> expr
        }
    }

    /* Comparison -> AddSubExpression (('<'|'<='|'>'|'>=') AddSubExpression)? */
    private fun comparison(): Expression {
        val expr: Expression = addSubExpression()

        return when {
            match("<") -> LessThan(expr, addSubExpression())
            match(">") -> GreaterThan(expr, addSubExpression())
            match("<=") -> LessThanOrEqual(expr, addSubExpression())
            match(">=") -> GreaterThanOrEqual(expr, addSubExpression())

            else -> expr
        }
    }

    /* AddSubExpression -> MulDivExpression (('+'|'-') MulDivExpression)* */
    private fun addSubExpression(): Expression {
        var expr: Expression = mulDivExpression()

        while (match("+", "-")) {
            val operator: Token = previous
            val right: Expression = mulDivExpression()
            expr =
                when (operator.value) {
                    "+" -> Add(expr, right)
                    "-" -> Sub(expr, right)
                    else -> throw IllegalStateException("Unexpected operator: $operator")
                }
        }

        return expr
    }

    /* MulDivExpression -> UnaryExpression (('*' | '/' | '%') UnaryExpression)* */
    private fun mulDivExpression(): Expression {
        var expr: Expression = unaryExpression()

        while (match("*", "/", "%")) {
            val operator: Token = previous
            val right: Expression = mulDivExpression()
            expr =
                when (operator.value) {
                    "*" -> Mul(expr, right)
                    "/" -> Div(expr, right)
                    "%" -> Mod(expr, right)
                    else -> throw IllegalStateException("Unexpected operator: $operator")
                }
        }

        return expr
    }

    /* UnaryExpression -> ('+' | '-' | '!' | '~')? Atom */
    private fun unaryExpression(): Expression {
        return when {
            match("+") -> Pos(atom())
            match("-") -> Neg(atom())
            match("!") -> LogicalNot(atom())
            match("~") -> BitwiseNot(atom())
            else -> atom()
        }
    }

    /* Atom -> Number | Identifier | FunctionCall | '(' Expression ')' */
    private fun atom(): Expression {
        when (current) {
            is Token.BooleanLiteral -> {
                return BooleanLiteral(popToken().value.toBoolean())
            }

            is Token.Color -> {
                val color = popToken().value

                when {
                    /* Special Case: Many Colours can be passed into a function as a single argument */
                    current is Token.Color -> {
                        val colourList = mutableListOf<String>(color)

                        do {
                            colourList.add(popToken().value)
                        } while (current is Token.Color)

                        return ColorList(colourList)
                    }

                    else -> return Color(color)
                }
            }

            is Token.StringLiteral -> {
                return StringLiteral(popToken().value.trim('"'))
            }

            is Token.Number -> {
                val number = popToken().value

                when {
                    /* Special Case: Many Numbers can be passed into a function as a single argument */
                    current is Token.Number -> {
                        val numList = mutableListOf<Double>(number.toDouble())

                        do {
                            numList.add(popToken().value.toDouble())
                        } while (current is Token.Number)

                        return NumList(numList)
                    }

                    else -> return NumLiteral(number.toDouble())
                }
            }

            is Token.Word -> {
                val name: String = popToken().value

                when {
                    match("(") -> {
                        val args = mutableListOf<Expression>()

                        if (current.value != ")") {
                            do {
                                args.add(expression())
                            } while (match(","))
                        }

                        expect(")")

                        /* assert that function exists and check version compatibility */
                        val function = FunctionCall(name, args)
                        validateFunctions(function)

                        return function
                    }

                    else -> return Variable(name)
                }
            }

            is Token.Paren -> {
                when {
                    match("(") -> {
                        val expr = expression()
                        expect(")")
                        return expr
                    }

                    match("[") -> {
                        val source = popToken().value
                        expect("]")

                        validateSource(source)
                        return Variable(source)
                    }

                    else ->
                        throw UnexpectedTokenException(
                            "Unexpected token (wrong bracket): ${current.value}"
                        )
                }
            }

            is Token.EOF -> throw EndOfFileException("Unexpected End of Expression")

            else -> throw UnexpectedTokenException("Unexpected token: ${current.value}")
        }
    }

    /** Checks if the current token matches a certain value. Advances to the next token if so. */
    private fun match(vararg values: String): Boolean {
        return values.any { current.value == it }.also { if (it) popToken() }
    }

    /**
     * Consumes the current token, updates the values for current and previous. Throws an exception
     * when an unrecognised token is found.
     */
    private fun popToken(): Token {
        if (current !is Token.EOF) {
            require(tokenIterator.hasNext())
            previous = current
            current = tokenIterator.next()

            if (current is Token.Unknown)
                throw UnknownTokenException("Unrecognised Token: ${current.value}")
        }

        return previous
    }

    /** Ensures that the current token matches the expected value. Throws an exception if not. */
    private fun expect(expected: String) {
        if (current.value == expected) {
            popToken()
        } else {
            throw MissingTokenException("Expected: '$expected'. Got: '${current.value}'")
        }
    }

    /**
     * Check that a function with this name and number of arguments actually exists.
     *
     * @param function the function to validate.
     * @throws FunctionNotFoundException if the function/arity combination does not exist.
     * @throws VersionConflictException if the function is not supported in the current version
     *   range.
     */
    private fun validateFunctions(function: FunctionCall) {
        val versionsRange = VersionRegistry.getFunctionVersions(function)

        minVersion = max(versionsRange.minVersion, minVersion)
        maxVersion = min(versionsRange.maxVersion, maxVersion)
    }

    /**
     * Check that a source with this name actually exists.
     *
     * @param sourceName the name of the source to validate.
     * @throws SourceNotFoundException if the source does not exist.
     * @throws VersionConflictException if the source is not supported in the current version range.
     */
    private fun validateSource(sourceName: String) {
        val versionsRange = VersionRegistry.getSourceVersions(sourceName)

        minVersion = max(versionsRange.minVersion, minVersion)
        maxVersion = min(versionsRange.maxVersion, maxVersion)
    }

    companion object {

        /**
         * Parses the expression and returns all the versions for which the expression is valid.
         *
         * @param expression the string expression to parse.
         * @param ctx the validation context holding the versioning and scope data.
         * @return the set of versions for which the expression is valid.
         * @throws ExpressionParsingException if the tokens cannot be parsed into a valid
         *     * expression.
         */
        fun getValidationResult(
            expression: String,
            ctx: WatchFaceElementContext = WatchFaceElementContext.Companion.emptyContext(),
        ): ValidationResult {
            val tokens = Tokenizer.tokeniseString(expression) + Token.EOF
            val parser = ExpressionParser(tokens, ctx)

            // TODO(b/433917558) collect informed ValidationErrors in the expression parser, add a
            // ExpressionVersionEliminationError when versions are eliminated
            try {
                parser.parse()
                return ValidationResult.PartialSuccess(
                    (parser.minVersion..parser.maxVersion).toSet(),
                    emptyMap(),
                )
            } catch (e: ExpressionParsingException) {
                return ValidationResult.Failure(
                    ExpressionSyntaxError(
                        "Error in expression: '$expression'. ${e.message ?: "Syntax Error"}",
                        ctx.elementPath,
                    )
                )
            }
        }

        /**
         * Parses a string expression into an [Expression] object.
         *
         * @param expression the string expression to parse.
         * @param ctx the validation context holding the versioning and scope data.
         * @return the parsed [Expression].
         * @throws ExpressionParsingException if the tokens cannot be parsed into a valid
         *   expression.
         */
        fun parse(
            expression: String,
            ctx: WatchFaceElementContext = WatchFaceElementContext.Companion.emptyContext(),
        ): Expression {
            val tokens = Tokenizer.tokeniseString(expression) + Token.EOF
            return parse(tokens, ctx)
        }

        /**
         * Parses a list of tokens into an [Expression] object.
         *
         * @param tokens the list of tokens to parse.
         * @param ctx the validation context holding the versioning and scope data.
         * @return the parsed [Expression].
         * @throws ExpressionParsingException if the tokens cannot be parsed into a valid
         *   expression.
         */
        fun parse(
            tokens: Sequence<Token>,
            ctx: WatchFaceElementContext = WatchFaceElementContext.Companion.emptyContext(),
        ): Expression {
            val parser = ExpressionParser(tokens, ctx)
            return parser.parse()
        }
    }
}
