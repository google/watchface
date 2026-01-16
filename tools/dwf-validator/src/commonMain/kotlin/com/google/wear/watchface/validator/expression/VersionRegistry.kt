package com.google.wear.watchface.validator.expression

import com.google.wear.watchface.validator.MAX_WFF_VERSION

object VersionRegistry {
    private val FUNCTIONS =
        mapOf(
            "round{1}" to VersionRange(1),
            "floor{1}" to VersionRange(1),
            "ceil{1}" to VersionRange(1),
            "fract{1}" to VersionRange(1),
            "sin{1}" to VersionRange(1),
            "cos{1}" to VersionRange(1),
            "tan{1}" to VersionRange(1),
            "asin{1}" to VersionRange(1),
            "acos{1}" to VersionRange(1),
            "atan{1}" to VersionRange(1),
            "abs{1}" to VersionRange(1),
            "clamp{3}" to VersionRange(1),
            "rand{2}" to VersionRange(1),
            "log{1}" to VersionRange(1),
            "log2{1}" to VersionRange(1),
            "log10{1}" to VersionRange(1),
            "sqrt{1}" to VersionRange(1),
            "cbrt{1}" to VersionRange(1),
            "exp{1}" to VersionRange(1),
            "expm1{1}" to VersionRange(1),
            "deg{1}" to VersionRange(1),
            "rad{1}" to VersionRange(1),
            "pow{2}" to VersionRange(1),
            "numberFormat{2}" to VersionRange(1),
            "icuText{1}" to VersionRange(1),
            "icuText{2}" to VersionRange(2),
            "icuBestText{1}" to VersionRange(1),
            "icuBestText{2}" to VersionRange(2),
            "subText{3}" to VersionRange(1),
            "textLength{1}" to VersionRange(1),
            "colorRgb{3}" to VersionRange(4),
            "colorArgb{4}" to VersionRange(4),
            "extractColorFromColors{3}" to VersionRange(4),
            "extractColorFromWeightedColors{4}" to VersionRange(4),
        )

    private val SOURCES: Map<String, VersionRange> =
        mapOf(
            "UTC_TIMESTAMP" to VersionRange(1),
            "MILLISECOND" to VersionRange(1),
            "SECOND" to VersionRange(1),
            "SECOND_Z" to VersionRange(1),
            "SECOND_TENS_DIGIT" to VersionRange(2),
            "SECOND_UNITS_DIGIT" to VersionRange(2),
            "SECOND_MILLISECOND" to VersionRange(1),
            "SECONDS_IN_DAY" to VersionRange(1),
            "SECONDS_SINCE_EPOCH" to VersionRange(3),
            "MINUTE" to VersionRange(1),
            "MINUTE_Z" to VersionRange(1),
            "MINUTE_TENS_DIGIT" to VersionRange(2),
            "MINUTE_UNITS_DIGIT" to VersionRange(2),
            "MINUTE_SECOND" to VersionRange(1),
            "MINUTES_SINCE_EPOCH" to VersionRange(3),
            "HOUR_0_11" to VersionRange(1),
            "HOUR_0_11_Z" to VersionRange(1),
            "HOUR_0_11_MINUTE" to VersionRange(1),
            "HOUR_1_12" to VersionRange(1),
            "HOUR_1_12_Z" to VersionRange(1),
            "HOUR_1_12_MINUTE" to VersionRange(1),
            "HOUR_0_23" to VersionRange(1),
            "HOUR_0_23_Z" to VersionRange(1),
            "HOUR_0_23_MINUTE" to VersionRange(1),
            "HOUR_1_24" to VersionRange(1),
            "HOUR_1_24_Z" to VersionRange(1),
            "HOUR_1_24_MINUTE" to VersionRange(1),
            "HOUR_TENS_DIGIT" to VersionRange(2),
            "HOUR_UNITS_DIGIT" to VersionRange(2),
            "HOURS_SINCE_EPOCH" to VersionRange(3),
            "DAY" to VersionRange(1),
            "DAY_Z" to VersionRange(1),
            "DAY_HOUR" to VersionRange(1),
            "DAY_0_30" to VersionRange(1),
            "DAY_0_30_HOUR" to VersionRange(1),
            "DAY_OF_YEAR" to VersionRange(1),
            "DAY_OF_WEEK" to VersionRange(1),
            "DAY_OF_WEEK_F" to VersionRange(1),
            "DAY_OF_WEEK_S" to VersionRange(1),
            "FIRST_DAY_OF_WEEK" to VersionRange(2),
            "MONTH" to VersionRange(1),
            "MONTH_Z" to VersionRange(1),
            "MONTH_F" to VersionRange(1),
            "MONTH_S" to VersionRange(1),
            "DAYS_IN_MONTH" to VersionRange(1),
            "MONTH_DAY" to VersionRange(1),
            "MONTH_0_11" to VersionRange(1),
            "MONTH_0_11_DAY" to VersionRange(1),
            "YEAR" to VersionRange(1),
            "YEAR_S" to VersionRange(1),
            "YEAR_MONTH" to VersionRange(1),
            "YEAR_MONTH_DAY" to VersionRange(1),
            "WEEK_IN_MONTH" to VersionRange(1),
            "WEEK_IN_YEAR" to VersionRange(1),
            "IS_24_HOUR_MODE" to VersionRange(1),
            "IS_DAYLIGHT_SAVING_TIME" to VersionRange(1),
            "TIMEZONE" to VersionRange(1),
            "TIMEZONE_ABB" to VersionRange(1),
            "TIMEZONE_ID" to VersionRange(1),
            "TIMEZONE_OFFSET" to VersionRange(1),
            "TIMEZONE_OFFSET_MINUTES" to VersionRange(3),
            "TIMEZONE_OFFSET_DST" to VersionRange(1),
            "TIMEZONE_OFFSET_MINUTES_DST" to VersionRange(3),
            "AMPM_STATE" to VersionRange(1),
            "AMPM_POSITION" to VersionRange(1),
            "AMPM_STRING" to VersionRange(1),
            "MOON_PHASE_POSITION" to VersionRange(1),
            "MOON_PHASE_TYPE" to VersionRange(1),
            "MOON_PHASE_TYPE_STRING" to VersionRange(1),
            "LANGUAGE_LOCALE_NAME" to VersionRange(1),
            "STEP_COUNT" to VersionRange(1),
            "STEP_GOAL" to VersionRange(1),
            "STEP_PERCENT" to VersionRange(1),
            "HEART_RATE" to VersionRange(1),
            "HEART_RATE_Z" to VersionRange(1),
            "ACCELEROMETER_IS_SUPPORTED" to VersionRange(1),
            "ACCELEROMETER_X" to VersionRange(1),
            "ACCELEROMETER_Y" to VersionRange(1),
            "ACCELEROMETER_Z" to VersionRange(1),
            "ACCELEROMETER_ANGLE_X" to VersionRange(1),
            "ACCELEROMETER_ANGLE_Y" to VersionRange(1),
            "ACCELEROMETER_ANGLE_Z" to VersionRange(1),
            "ACCELEROMETER_ANGLE_XY" to VersionRange(1),
            "BATTERY_PERCENT" to VersionRange(1),
            "BATTERY_CHARGING_STATUS" to VersionRange(1),
            "BATTERY_IS_LOW" to VersionRange(1),
            "BATTERY_TEMPERATURE_CELSIUS" to VersionRange(1),
            "BATTERY_TEMPERATURE_FAHRENHEIT" to VersionRange(1),
            "UNREAD_NOTIFICATION_COUNT" to VersionRange(1),
            "WEATHER.IS_AVAILABLE" to VersionRange(2),
            "WEATHER.IS_ERROR" to VersionRange(2),
            "WEATHER.CONDITION" to VersionRange(2),
            "WEATHER.CONDITION_NAME" to VersionRange(2),
            "WEATHER.IS_DAY" to VersionRange(2),
            "WEATHER.TEMPERATURE" to VersionRange(2),
            "WEATHER.TEMPERATURE_UNIT" to VersionRange(2),
            "WEATHER.DAY_TEMPERATURE_LOW" to VersionRange(2),
            "WEATHER.DAY_TEMPERATURE_HIGH" to VersionRange(2),
            "WEATHER.CHANCE_OF_PRECIPITATION" to VersionRange(2),
            "WEATHER.UV_INDEX" to VersionRange(2),
            "WEATHER.LAST_UPDATED" to VersionRange(2),
            "WEATHER.HOURS.{index}.IS_AVAILABLE" to VersionRange(2),
            "WEATHER.HOURS.{index}.CONDITION" to VersionRange(2),
            "WEATHER.HOURS.{index}.CONDITION_NAME" to VersionRange(2),
            "WEATHER.HOURS.{index}.IS_DAY" to VersionRange(2),
            "WEATHER.HOURS.{index}.TEMPERATURE" to VersionRange(2),
            "WEATHER.HOURS.{index}.UV_INDEX" to VersionRange(2),
            "WEATHER.DAYS.{index}.IS_AVAILABLE" to VersionRange(2),
            "WEATHER.DAYS.{index}.CONDITION_DAY" to VersionRange(2),
            "WEATHER.DAYS.{index}.CONDITION_DAY_NAME" to VersionRange(2),
            "WEATHER.DAYS.{index}.CONDITION_NIGHT" to VersionRange(2),
            "WEATHER.DAYS.{index}.CONDITION_NIGHT_NAME" to VersionRange(2),
            "WEATHER.DAYS.{index}.TEMPERATURE_LOW" to VersionRange(2),
            "WEATHER.DAYS.{index}.TEMPERATURE_HIGH" to VersionRange(2),
            "WEATHER.DAYS.{index}.CHANCE_OF_PRECIPITATION" to VersionRange(2),
            "WEATHER.DAYS.{index}.CHANCE_OF_PRECIPITATION_NIGHT" to VersionRange(2),
            "WEATHER.DAYS.{index}.UV_INDEX" to VersionRange(2),
        )

    /**
     * Returns the supported versions for a given function name and arity.
     *
     * @param function the function to validate
     * @return The supported versions for the function as [[VersionRange]].
     * @throws FunctionNotFoundException if the function is not found.
     */
    fun getFunctionVersions(function: FunctionCall): VersionRange {
        val arity: Int = function.arguments.size
        return FUNCTIONS["${function.name}{$arity}"]
            ?: throw FunctionNotFoundException(
                "Function ${function.name} with arity $arity not found."
            )
    }

    /**
     * Returns the supported versions for a given source name.
     *
     * Source names often include an index. For example, "WEATHER.HOURS.6.IS_AVAILABLE". This method
     * ignores the index and retrieves the versions for the base source name.
     *
     * @param sourceName The name of the source.
     * @return The supported versions for the source. as [[VersionRange]]. Returns the full version
     *   range if source is not found.
     */
    fun getSourceVersions(sourceName: String): VersionRange {
        val indexMatcher = Regex("\\.\\d+")
        // TODO(b/433921752): index range check
        return SOURCES[sourceName.replace(indexMatcher, ".{index}")]
            ?: VersionRange(1, MAX_WFF_VERSION)
    }
}
