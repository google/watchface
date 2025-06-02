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

package com.google.wear.watchface.dfx.memory;

/** Annotates test results to make it easier for humans to read them. */
class TestResultFormatter {

    static String formatSuccess(String successMessage) {
        return String.format("[MEMORY_FOOTPRINT]: ✅PASS✅ %s ✅ ", successMessage);
    }

    static String formatFailure(String failedMessage) {
        return String.format("[MEMORY_FOOTPRINT]: ❌FAIL❌ %s ❌ ", failedMessage);
    }

    static String formatException(String exceptionMessage) {
        return String.format(
                "%s\n%s",
                "❗❗❗❗ Something went wrong. Please retry or seek assistance.❗❗❗❗",
                exceptionMessage);
    }
}
