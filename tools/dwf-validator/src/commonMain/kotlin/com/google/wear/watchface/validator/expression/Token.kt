package com.google.wear.watchface.validator.expression

import kotlin.jvm.JvmInline

/**
 * An interface for Expression Tokens
 *
 * @property value the raw string value associated with a token.
 */
sealed interface Token {
    val value: String

    @JvmInline value class Number(override val value: String) : Token

    @JvmInline value class Color(override val value: String) : Token

    @JvmInline value class BooleanLiteral(override val value: String) : Token

    @JvmInline value class StringLiteral(override val value: String) : Token

    @JvmInline value class Word(override val value: String) : Token

    @JvmInline value class Operator(override val value: String) : Token

    @JvmInline value class Paren(override val value: String) : Token

    @JvmInline value class Unknown(override val value: String) : Token

    data object EOF : Token {
        override val value = ""
    }
}
