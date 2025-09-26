package com.google.wear.watchface.validator.expression

import kotlin.text.get

/** A lexer/tokeniser for dwf arithmetic expressions */
object Tokenizer {
    // TODO(b/433461048): Fine-grained numeric token matching and typing.
    private const val NUMERIC_MATCHER = "\\d*\\.\\d+|\\d+"
    private const val COLOR_MATCHER = "#[0-9a-fA-F]{8}|#[0-9a-fA-F]{6}"
    private const val BOOLEAN_MATCHER = "true|false|True|False|TRUE|FALSE"
    private const val STRING_MATCHER = "\"[^\"]*\""
    private const val WORD_MATCHER = "[a-zA-Z][a-zA-Z0-9_\\.]*"
    private const val PAREN_MATCHER = "\\(|\\)|\\[|\\]"
    private const val WHITESPACE_MATCHER = "\\s+"
    private const val OPERATOR_MATCHER = "<=|>=|==|!=|&&|\\|\\||\\+|-|\\*|/|%|~|!|&|\\||<|>|\\?|:|,"

    private val matcher by lazy {
        Regex(
            "(?<number>$NUMERIC_MATCHER)|" +
                "(?<color>$COLOR_MATCHER)|" +
                "(?<boolean>$BOOLEAN_MATCHER)|" +
                "(?<string>$STRING_MATCHER)|" +
                "(?<word>$WORD_MATCHER)|" +
                "(?<operator>$OPERATOR_MATCHER)|" +
                "(?<paren>$PAREN_MATCHER)|" +
                "(?<whitespace>$WHITESPACE_MATCHER)|" +
                "(?<unknown>.)"
        )
    }

    /**
     * Converts a raw expression string into a list of [[Token]]s using RegEx.
     *
     * @param expression the raw expression string to be tokenised.
     */
    fun tokeniseString(expression: String): Sequence<Token> {
        return matcher.findAll(expression).mapNotNull { match: MatchResult ->
            when {
                match.groups["number"] != null -> Token.Number(match.value)
                match.groups["color"] != null -> Token.Color(match.value)
                match.groups["boolean"] != null -> Token.BooleanLiteral(match.value)
                match.groups["string"] != null -> Token.StringLiteral(match.value)
                match.groups["word"] != null -> Token.Word(match.value)
                match.groups["operator"] != null -> Token.Operator(match.value)
                match.groups["paren"] != null -> Token.Paren(match.value)
                match.groups["whitespace"] != null -> null
                match.groups["unknown"] != null -> Token.Unknown(match.value)
                else ->
                    throw IllegalStateException(
                        "Unreachable code reached for match: ${match.value}"
                    )
            }
        }
    }
}
