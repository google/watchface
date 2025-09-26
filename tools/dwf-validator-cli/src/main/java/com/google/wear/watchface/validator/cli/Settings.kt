package com.google.wear.watchface.validator.cli

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException

class Settings(val sourcePath: String, val rawXml: Boolean = false) {
    companion object {
        val cliInvokeCommand = "java -jar dwf-validator-cli.jar"

        fun parseFromArguments(arguments: Array<String>): Settings? {
            val sourcePathOption =
                Option.builder()
                    .longOpt("source")
                    .desc("Path to the watch face package to be validated.")
                    .hasArg()
                    .required()
                    .build()

            val rawXmlOption =
                Option.builder("x")
                    .longOpt("raw-xml")
                    .desc("Flag to indicate the source is a raw xml file rather than an apk.")
                    .build()

            val options = Options()
            options.addOption(sourcePathOption)
            options.addOption(rawXmlOption)

            val parser = DefaultParser()
            try {
                val line = parser.parse(options, arguments)
                return Settings(line.getOptionValue(sourcePathOption), line.hasOption(rawXmlOption))
            } catch (e: ParseException) {
                println("Error: " + e.localizedMessage)
                HelpFormatter().printHelp(cliInvokeCommand, options, true)
                return null
            }
        }
    }
}
