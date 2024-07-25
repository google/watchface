/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.clockwork.wff.optimizer

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException

class Settings(val sourcePath: String, val verbose: Boolean = false) {
    companion object {
        val cliInvokeCommand = "java -jar wff-optimizer.jar"

        fun parseFromArguments(arguments: Array<String>): Settings? {
            val sourcePathOption =
                Option.builder()
                    .longOpt("source")
                    .desc("Path to the watch face package to be optimized. Required.")
                    .hasArg()
                    .required()
                    .build()

            val verboseOption =
                Option.builder()
                    .longOpt("verbose")
                    .desc("Verbose logging, default is false.")
                    .build()

            val options = Options()
            options.addOption(sourcePathOption)
            options.addOption(verboseOption)

            val parser = DefaultParser()
            try {
                val line = parser.parse(options, arguments)
                return Settings(
                    line.getOptionValue(sourcePathOption),
                    line.hasOption(verboseOption)
                )
            } catch (e: ParseException) {
                System.out.println("Error: " + e.getLocalizedMessage())
                HelpFormatter().printHelp(cliInvokeCommand, options, true)
                return null
            }
        }
    }
}
