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

/**
 * Represents the memory footprint calculation result.
 *
 * @property totalBytes the total memory footprint of all the assets in the watch face, in bytes.
 * @property maxActiveBytes the maximum sum memory footprint of the assets that can be active at any
 *   point in time on the watch face in active mode, in bytes.
 * @property maxAmbientBytes the maximum sum memory footprint of the assets that can be active at
 *   any point in time on the watch face in ambient mode, in bytes.
 */
data class MemoryFootprint(
    val totalBytes: Long,
    val maxActiveBytes: Long,
    val maxAmbientBytes: Long
) {
    /**
     * Validates that the maximum footprint is less than the allowed footprint.
     *
     * @param settings The EvaluationSettings object, used to determine the maximum allowed memory
     *   footprint in ambient and in active mode.
     * @throws TestFailedException if the watch face a maximum memory footprint that is greater than
     *   the allowed footprint.
     */
    fun validate(settings: EvaluationSettings) {
        if (
            maxAmbientBytes > settings.ambientLimitBytes ||
                maxActiveBytes > settings.activeLimitBytes
        ) {
            throw TestFailedException(
                String.format(
                    "Watch Face has a memory footprint of %,.2f MB in ambient and " +
                        "%,.2f MB in active, which is more than the allowed " +
                        "%,.2f MB in ambient and %.2f MB in active.",
                    toMB(maxAmbientBytes),
                    toMB(maxActiveBytes),
                    toMB(settings.ambientLimitBytes),
                    toMB(settings.activeLimitBytes)
                )
            )
        }
    }

    companion object {
        /** Converts bytes to binary megabytes. */
        @JvmStatic fun toMB(bytes: Long): Double = bytes.toDouble() / 1024 / 1024

        /** Converts binary megabytes to bytes with 3 digits of precision. */
        @JvmStatic fun toBytes(megaBytes: Double): Long = (megaBytes * 1024 * 1024).toLong()

        /**
         * Computes the maximum memory footprint between two MemoryFootprint objects by taking the
         * maximum of each component.
         */
        @JvmStatic
        fun max(left: MemoryFootprint, right: MemoryFootprint): MemoryFootprint {
            return MemoryFootprint(
                totalBytes = maxOf(left.totalBytes, right.totalBytes),
                maxActiveBytes = maxOf(left.maxActiveBytes, right.maxActiveBytes),
                maxAmbientBytes = maxOf(left.maxAmbientBytes, right.maxAmbientBytes)
            )
        }
    }
}
