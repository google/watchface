package com.google.wear.watchface.validator.expression

sealed interface Expression

/** Ternary Operations (alternative to if/else expressions) */
data class Ternary(val condition: Expression, val ifTrue: Expression, val ifFalse: Expression) :
    Expression

/** Boolean Binary Operations */
data class BitwiseOr(val left: Expression, val right: Expression) : Expression

data class LogicalOr(val left: Expression, val right: Expression) : Expression

data class BitwiseAnd(val left: Expression, val right: Expression) : Expression

data class LogicalAnd(val left: Expression, val right: Expression) : Expression

/** Comparison Operations */
data class Equal(val left: Expression, val right: Expression) : Expression

data class NotEqual(val left: Expression, val right: Expression) : Expression

data class LessThan(val left: Expression, val right: Expression) : Expression

data class LessThanOrEqual(val left: Expression, val right: Expression) : Expression

data class GreaterThan(val left: Expression, val right: Expression) : Expression

data class GreaterThanOrEqual(val left: Expression, val right: Expression) : Expression

/** Binary Operations */
data class Add(val left: Expression, val right: Expression) : Expression

data class Sub(val left: Expression, val right: Expression) : Expression

data class Mul(val left: Expression, val right: Expression) : Expression

data class Div(val left: Expression, val right: Expression) : Expression

data class Mod(val left: Expression, val right: Expression) : Expression

/** Unary Operations */
data class Pos(val operand: Expression) : Expression

data class Neg(val operand: Expression) : Expression

data class BitwiseNot(val operand: Expression) : Expression

data class LogicalNot(val operand: Expression) : Expression

/** Atomic Expressions */
data class FunctionCall(val name: String, val arguments: List<Expression>) : Expression

data class NumLiteral(val value: Double) : Expression

data class NumList(val value: List<Double>) : Expression

data class Color(val value: String) : Expression

data class ColorList(val value: List<String>) : Expression

data class BooleanLiteral(val value: Boolean) : Expression

data class StringLiteral(val value: String) : Expression

data class Variable(val name: String) : Expression
