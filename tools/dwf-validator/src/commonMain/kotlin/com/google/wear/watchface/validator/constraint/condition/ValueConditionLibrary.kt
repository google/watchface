package com.google.wear.watchface.validator.constraint.condition

typealias ValueConditionFunction = (String) -> Boolean

interface ValueConditions {
    /** Condition asserting that an attribute is 'equal' to a given value. */
    fun equals(expectedValue: String): ValueCondition

    /** Condition asserting that an attribute is an integer. */
    fun integer(): ValueCondition

    /** Condition asserting that an attribute is an integer within a given range. */
    fun integer(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): ValueCondition

    /** Condition asserting that an attribute is a float. */
    fun float(): ValueCondition

    /** Condition asserting that an attribute is a float within a given range. */
    fun float(
        min: Float = Float.NEGATIVE_INFINITY,
        max: Float = Float.POSITIVE_INFINITY,
    ): ValueCondition

    /** Condition asserting that an attribute is a color in #AARRGGBB or #RRGGBB format. */
    fun color(): ValueCondition

    /** Condition asserting that an attribute is a time in HH:MM:SS format. */
    fun time(): ValueCondition

    /** Condition asserting that an attribute is non-empty. */
    fun nonEmpty(): ValueCondition

    /** Condition asserting that an attribute is a string of length n. */
    fun stringOfLength(n: Int): ValueCondition

    /** Condition asserting that an attribute is a vector of n floats. */
    fun floatVector(n: Int): ValueCondition

    /** Condition asserting that an attribute is a vector of floats of any length. */
    fun floatVector(): ValueCondition

    /** Condition asserting that an attribute is a vector of colors of any length. */
    fun argbVector(): ValueCondition

    /** Condition asserting that an attribute is a vector of colors with a maximum length. */
    fun argbVector(maxLength: Int): ValueCondition

    /** Condition asserting that an attribute is a boolean ('true' or 'false'). */
    fun boolean(): ValueCondition

    /** Condition asserting that an attribute is a data source in a [DATA.SOURCE] format. */
    fun dataSource(): ValueCondition

    /** Condition asserting that an attribute is one of a predefined set of string options. */
    fun enum(vararg options: String) = enum(options.toSet())

    /** Condition asserting that an attribute is one of a predefined set of string options. */
    fun enum(options: Set<String>): ValueCondition

    /** Condition asserting that an attribute is a valid time text format. */
    fun timeTextFormat(): ValueCondition

    /** Condition asserting that an attribute is a deep link in a valid format. */
    fun deepLink(): ValueCondition

    /** Condition asserting that an attribute is a valid expression. */
    fun validExpression(): ExpressionCondition

    /** Combines two conditions with a logical AND */
    infix fun ValueCondition.and(other: ValueCondition) =
        ValueCondition(
            "${this.errorMessage} AND ${other.errorMessage}",
            { value -> this.check(value) && other.check(value) },
        )

    /** Combines two conditions with a logical OR */
    infix fun ValueCondition.or(other: ValueCondition) =
        ValueCondition(
            "${this.errorMessage} OR ${other.errorMessage}",
            { value -> this.check(value) || other.check(value) },
        )
}

object ValueConditionLibrary : ValueConditions {
    private const val ARGB_MATCHER = "#([A-Fa-f0-9]{8}|[A-Fa-f0-9]{6})"
    private const val TIME_MATCHER = "([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]"
    private const val OPTIONAL_DECIMAL_MATCHER = "([-+]?[0-9]*(\\.[0-9]+)?)"
    private const val DATA_SOURCE_MATCHER = "\\[[A-Z0-9]+([._]\\w+)*\\]"
    private const val TIME_TEXT_FORMAT_MATCHER =
        "h{2}_(10|1)|m{2}_(10|1)|s{2}_(10|1)|((h{1,2}):m{1,2}:s{1,2}|(h{1,2}):m{1,2})|(h{1,2})|m{1,2}|s{1,2}"
    private const val DEEP_LINK_MATCHER =
        "([a-zA-Z][a-zA-Z0-9+.-]*):\\/\\/([^?#]*)(?:\\?([^#]*))?(?:#(.*))?"

    override fun equals(expectedValue: String) =
        ValueCondition("Value must equal '$expectedValue'.", { value -> value == expectedValue })

    override fun integer() =
        ValueCondition("Value must be an integer.", { value -> value.toIntOrNull() != null })

    override fun integer(min: Int, max: Int) =
        ValueCondition(
            "Value must be an integer in the range [$min, $max].",
            { value -> value.toIntOrNull() != null && value.toInt() in min..max },
        )

    override fun float() =
        ValueCondition("Value must be a float.", { value -> value.toFloatOrNull() != null })

    override fun float(min: Float, max: Float) =
        ValueCondition(
            "Value must be a float in the range [$min, $max].",
            { value -> value.toFloatOrNull() != null && value.toFloat() in min..max },
        )

    override fun color() =
        ValueCondition(
            "Value must be a color in #AARRGGBB or #RRGGBB format.",
            { value -> value.matches(Regex("^$ARGB_MATCHER$")) },
        )

    override fun time() =
        ValueCondition(
            "Value must be a time in hh:mm:ss format.",
            { value -> value.matches(Regex("^$TIME_MATCHER$")) },
        )

    override fun nonEmpty() =
        ValueCondition("Value cannot be empty.", { value -> value.isNotEmpty() })

    override fun stringOfLength(n: Int): ValueCondition =
        ValueCondition("Value must be a string of length $n.", { value -> value.length == n })

    override fun floatVector(n: Int) =
        ValueCondition(
            "Value must be a space separated list of $n floats.",
            { value ->
                val vectorMatcher =
                    Regex("^($OPTIONAL_DECIMAL_MATCHER(\\s$OPTIONAL_DECIMAL_MATCHER){${n-1}})$")
                value.matches(vectorMatcher)
            },
        )

    override fun floatVector() =
        ValueCondition(
            "Value must be a space separated list of floats.",
            { value ->
                val vectorMatcher =
                    Regex("^($OPTIONAL_DECIMAL_MATCHER(\\s$OPTIONAL_DECIMAL_MATCHER)*)$")
                value.matches(vectorMatcher)
            },
        )

    override fun argbVector() =
        ValueCondition(
            "Value must be a space separated list of colors in #AARRGGBB or #RRGGBB format.",
            { value ->
                val colorVectorMatcher = Regex("^($ARGB_MATCHER(\\s$ARGB_MATCHER)*)$")
                value.matches(colorVectorMatcher)
            },
        )

    override fun argbVector(maxLength: Int) =
        ValueCondition(
            "Value must be a space separated list of up to $maxLength colors in #AARRGGBB or #RRGGBB format.",
            { value ->
                val colorVectorMatcher =
                    Regex("^($ARGB_MATCHER(\\s$ARGB_MATCHER){0,${maxLength - 1}})$")
                value.matches(colorVectorMatcher)
            },
        )

    override fun boolean() =
        ValueCondition(
            "Value must be 'true' or 'false'.",
            { value -> value.lowercase() == "true" || value.lowercase() == "false" },
        )

    override fun dataSource() =
        ValueCondition(
            "Value must be a source in the [SOURCE.DATA] format.",
            { value -> value.matches(Regex("^$DATA_SOURCE_MATCHER$")) },
        )

    override fun enum(options: Set<String>) =
        ValueCondition(
            "Value must be in {${options.joinToString(", ") }}}",
            { value -> value in options },
        )

    override fun timeTextFormat() =
        ValueCondition(
            "Value must be a valid time text format.",
            { value -> value.matches(Regex("^$TIME_TEXT_FORMAT_MATCHER$")) },
        )

    override fun deepLink() =
        ValueCondition(
            "Value must be a deep link in a valid format: app://open.my.app",
            { value -> value.matches(Regex("^$DEEP_LINK_MATCHER$")) },
        )

    override fun validExpression() =
        ExpressionCondition(errorMessage = "Must be a valid expression")
}
