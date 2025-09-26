package com.google.wear.watchface.validator.cli

import com.google.wear.watchface.validator.JvmWatchFaceDocument
import com.google.wear.watchface.validator.WatchFaceValidator
import com.google.wear.watchface.validator.XmlReader
import com.google.wear.watchface.validator.error.ValidationResult
import com.google.wear.watchface.validator.specification.WFF_SPECIFICATION
import java.nio.file.NoSuchFileException
import java.util.zip.ZipFile
import kotlin.system.exitProcess

internal const val FAILURE = 1
internal const val SUCCESS = 0

/**
 * Command line application which validates .xml files in the res/raw/ directory of a specified .apk
 * file.
 *
 * The .apk file should be passed as the first argument to the application. If the raw watch face
 * xml file fails validation then the application will exit with exit code 1.
 */
fun main(args: Array<String>) {
    Settings.parseFromArguments(args)?.let { App.run(it) }
}

object App {
    /**
     * Runs the validator on a specified .apk file. The validator is invoked on the xml file in
     * res/raw/ and the results are printed to stderr.
     *
     * @param settings The settings parsed from the command line arguments.
     */
    fun run(settings: Settings) {
        val validationResult =
            if (settings.rawXml) {
                validateRawXml(settings.sourcePath)
            } else {
                validateApk(settings.sourcePath)
            }

        exitProcess(if (validationResult is ValidationResult.Failure) FAILURE else SUCCESS)
    }

    fun validateApk(apkPath: String): ValidationResult {
        val zipFile = ZipFile(apkPath)
        val entries = zipFile.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.name.startsWith("res/raw/") && entry.name.endsWith(".xml")) {
                val document =
                    JvmWatchFaceDocument.of(
                        XmlReader.readFromInputStream(zipFile.getInputStream(entry))
                    )
                val validationResult =
                    WatchFaceValidator(WFF_SPECIFICATION).getValidationResult(document)

                printValidationReport(zipFile.name, validationResult)
                return validationResult
            }
        }

        throw NoSuchFileException("No XML file found in res/raw/ in ${zipFile.name}")
    }

    fun validateRawXml(xmlPath: String): ValidationResult {
        val document = JvmWatchFaceDocument.of(XmlReader.fromFilePath(xmlPath))
        val validator = WatchFaceValidator(WFF_SPECIFICATION)
        val validationResult = validator.getValidationResult(document)

        printValidationReport(xmlPath, validationResult)
        return validationResult
    }

    private fun printValidationReport(fileName: String, validationResult: ValidationResult) {
        val messageBuilder = StringBuilder()

        when (validationResult) {
            is ValidationResult.Success ->
                messageBuilder.appendLine("Validation Succeeded for file: $fileName.")

            is ValidationResult.PartialSuccess ->
                messageBuilder
                    .appendLine(
                        "Validation Succeeded for file: $fileName with some invalid versions."
                    )
                    .appendLine(
                        "Valid for versions: ${validationResult.validVersions.joinToString(", ")}.\n"
                    )
                    .appendLine(ValidationFailureMessage(validationResult.errorMap))

            is ValidationResult.Failure ->
                messageBuilder
                    .appendLine("Validation Failed for file: $fileName.\n")
                    .appendLine(ValidationFailureMessage(validationResult.errorMap))
        }

        System.err.println(messageBuilder.toString())
    }
}
