package com.google.wear.watchface.validator.expression

/** Exceptions thrown in the expression parser to categorise errors. */
sealed class ExpressionParsingException(message: String) : Exception(message)

class UnknownTokenException(message: String) : ExpressionParsingException(message)

class UnexpectedTokenException(message: String) : ExpressionParsingException(message)

class TokensNotConsumedException(message: String) : ExpressionParsingException(message)

class EndOfFileException(message: String) : ExpressionParsingException(message)

class MissingTokenException(message: String) : ExpressionParsingException(message)

class FunctionNotFoundException(message: String) : ExpressionParsingException(message)

class SourceNotFoundException(message: String) : ExpressionParsingException(message)

class VersionConflictException(message: String) : ExpressionParsingException(message)
