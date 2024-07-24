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

import java.io.File
import java.lang.Exception

fun main(args: Array<String>) {
    Settings.parseFromArguments(args)?.let { App.run(it) }
}

object App {
    fun run(settings: Settings) {
        try {
            val resRawDirectory = File(settings.sourcePath, "res/raw")
            if (!resRawDirectory.exists()) {
                System.err.println("Error can't open " + resRawDirectory.path)
                return
            }
            val xmlFiles = resRawDirectory.listFiles { f -> f.name.endsWith(".xml") }
            if (xmlFiles == null || xmlFiles.isEmpty()) {
                System.err.println("Failed to find any XML files in " + settings.sourcePath)
                return
            }
            println("Optimizing files in " + resRawDirectory.path)
            for (xmlFile in xmlFiles) {
                println("Optimizing " + xmlFile)
                Optimizer.optimize(xmlFile, settings)
            }
            println("Done")
        } catch (e: Exception) {
            System.err.println("Failed to optimize ${settings.sourcePath} due to $e")
            e.printStackTrace()
        }
    }
}
