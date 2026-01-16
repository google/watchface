package com.google.wear.watchface.validator.cli

import com.google.wear.watchface.validator.error.AttributeValueError
import com.google.wear.watchface.validator.error.ContentError
import com.google.wear.watchface.validator.error.ErrorMap
import com.google.wear.watchface.validator.error.ExpressionSyntaxError
import com.google.wear.watchface.validator.error.ExpressionVersionEliminationError
import com.google.wear.watchface.validator.error.IllegalAttributeError
import com.google.wear.watchface.validator.error.IllegalTagError
import com.google.wear.watchface.validator.error.RequiredConditionFailedError
import com.google.wear.watchface.validator.error.TagOccurrenceError
import com.google.wear.watchface.validator.error.UnknownError
import com.google.wear.watchface.validator.error.ValidationError
import com.google.wear.watchface.validator.error.VersionEliminationError

internal const val GLOBAL_ERROR_KEY = 0
internal const val BULLET = "  -  "
internal const val INDENT = "        "

/**
 * Class for formatting validation failure messages.
 *
 * @param errorMap The map of errors to format.
 */
class ValidationFailureMessage(private val errorMap: ErrorMap) {
    private val stringBuilder = StringBuilder()

    override fun toString(): String {
        errorMap.forEach { (key, errors) ->
            stringBuilder.appendLine(
                if (key == GLOBAL_ERROR_KEY) "Global Errors:\n" else "Error: Version $key Failed:\n"
            )

            val pathMap = errors.groupBy(ValidationError::elementPath)

            pathMap.forEach { (elementPath, validationErrors) ->
                stringBuilder
                    .appendLine("${elementPath.joinToString(" > ")}:")
                    .appendErrors(validationErrors)
            }
        }
        return stringBuilder.toString()
    }

    private fun StringBuilder.appendErrors(errors: List<ValidationError>) =
        errors.forEach { error: ValidationError ->
            when (error) {
                is IllegalTagError -> this.appendLine(BULLET + "Illegal Tag: \"${error.tagName}\"")

                is RequiredConditionFailedError ->
                    this.appendLine(BULLET + "Requirement Failed:")
                        .appendLine(wrapErrorMessage(error.conditionMessage))

                is ExpressionSyntaxError ->
                    this.appendLine(BULLET + "Expression syntax error:")
                        .appendLine(wrapErrorMessage(error.errorMessage))

                is ExpressionVersionEliminationError ->
                    this.appendLine(BULLET + "Version Eliminated:")
                        .appendLine(
                            wrapErrorMessage(
                                "\"${error.expressionResource}\" is exclusive to versions: " +
                                    error.permittedVersions.joinToString(", ")
                            )
                        )

                is VersionEliminationError ->
                    this.appendLine(BULLET + "Version Eliminated:")
                        .appendLine(
                            wrapErrorMessage(
                                "Condition: \"${error.conditionMessage}\" passed which is exclusive to versions: " +
                                    error.permittedVersions.joinToString(", ")
                            )
                        )

                is IllegalAttributeError ->
                    this.appendLine(BULLET + "Illegal Attribute: \"${error.attributeName}\"")

                is ContentError ->
                    this.appendLine(BULLET + "Illegal Content:")
                        .appendLine(wrapErrorMessage(error.errorMessage))

                is AttributeValueError ->
                    this.appendLine(BULLET + "Illegal Attribute Value:")
                        .appendLine(
                            wrapErrorMessage(
                                "Attribute: \"${error.attributeName}\" has illegal value: \"${error.attributeValue}\"."
                            )
                        )
                        .appendLine(wrapErrorMessage(error.errorMessage))

                is TagOccurrenceError ->
                    this.appendLine(BULLET + "Illegal Tag Occurrence:")
                        .appendLine(
                            wrapErrorMessage(
                                "Tag: \"${error.tagName}\" occurs ${error.actualCount} times, but must occur between ${error.expectedRange} times."
                            )
                        )

                is UnknownError ->
                    this.appendLine(BULLET + "Unknown error:")
                        .appendLine(wrapErrorMessage("\"${error.errorMessage}\""))
            }
        }

    /** Helper function for spreading long error messages across multiple lines */
    private fun wrapErrorMessage(message: String, wordsPerLine: Int = 15): String {
        val words = message.split(" ")
        return words.chunked(wordsPerLine).joinToString("") { wordList ->
            INDENT + wordList.joinToString(" ") + "\n"
        }
    }
}
