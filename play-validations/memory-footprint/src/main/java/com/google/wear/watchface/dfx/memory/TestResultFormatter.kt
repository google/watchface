/*
 * Copyright 2023 Google LLC
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

package com.google.wear.watchface.dfx.memory

/** Annotates test results to make it easier for humans to read them. */
internal object TestResultFormatter {
    @JvmStatic
    fun formatSuccess(successMessage: String): String {
        return "[MEMORY_FOOTPRINT]: ✅PASS✅ $successMessage ✅ "
    }

    @JvmStatic
    fun formatFailure(failedMessage: String): String {
        return "[MEMORY_FOOTPRINT]: ❌FAIL❌ $failedMessage ❌ "
    }

    @JvmStatic
    fun formatException(exceptionMessage: String): String {
        return "❗❗❗❗ Something went wrong. Please retry or seek assistance.❗❗❗❗\n$exceptionMessage"
    }
}
