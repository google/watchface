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

import java.util.Objects;

/** Represents the memory footprint calculation result. */
class MemoryFootprint {

    private final long totalBytes;

    private final long maxActiveBytes;

    private final long maxAmbientBytes;

    /**
     * @param totalBytes the total memory footprint of all the assets in the watch face, in bytes.
     * @param maxActiveBytes the maximum sum memory footprint of the assets that can be active at
     *     any point in time on the watch face in active mode, in bytes.
     * @param maxAmbientBytes the maximum sum memory footprint of the assets that can be active at
     *     any point in time on the watch face in ambient mode, in bytes.
     */
    MemoryFootprint(long totalBytes, long maxActiveBytes, long maxAmbientBytes) {
        this.totalBytes = totalBytes;
        this.maxActiveBytes = maxActiveBytes;
        this.maxAmbientBytes = maxAmbientBytes;
    }

    /**
     * The maximum footprint in bytes of the drawable assets that are active at any point in time on
     * the watch face in active mode.
     */
    public long getMaxActiveBytes() {
        return maxActiveBytes;
    }

    /**
     * The maximum footprint in bytes of the drawable assets that are active at any point in time on
     * the watch face in ambient mode.
     */
    public long getMaxAmbientBytes() {
        return maxAmbientBytes;
    }

    /** The total footprint in bytes of all the assets in the watch face package. */
    public long getTotalBytes() {
        return totalBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryFootprint)) return false;
        MemoryFootprint that = (MemoryFootprint) o;
        return totalBytes == that.totalBytes
                && maxActiveBytes == that.maxActiveBytes
                && maxAmbientBytes == that.maxAmbientBytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalBytes, maxActiveBytes, maxAmbientBytes);
    }

    @Override
    public String toString() {
        return "MemoryFootprint{"
                + "totalBytes="
                + totalBytes
                + ", maxActiveBytes="
                + maxActiveBytes
                + ", maxAmbientBytes="
                + maxAmbientBytes
                + '}';
    }

    /** Converts bytes to binary megabytes. */
    static double toMB(long bytes) {
        return ((double) bytes) / 1024 / 1024;
    }

    /** Converts binary megabytes to bytes with 3 digits of precision. */
    static long toBytes(double megaBytes) {
        return (long) (megaBytes * 1024) * 1024;
    }

    /**
     * Validates that the maximum footprint is less than the allowed footprint.
     *
     * @param settings The EvaluationSettings object, used to determine the maximum allowed memory
     *     footprint in ambient and in active mode.
     * @throws TestFailedException if the watch face a maximum memory footprint that is greater than
     *     the allowed footprint.
     */
    void validate(EvaluationSettings settings) {
        if (maxAmbientBytes > settings.getAmbientLimitBytes()
                || maxActiveBytes > settings.getActiveLimitBytes()) {
            throw new TestFailedException(
                    String.format(
                            "Watch Face has a memory footprint of %,.2f MB in ambient and "
                                    + "%,.2f MB in active, which is more than the allowed "
                                    + "%,.2f MB in ambient and %.2f MB in active.",
                            toMB(maxAmbientBytes),
                            toMB(maxActiveBytes),
                            toMB(settings.getAmbientLimitBytes()),
                            toMB(settings.getActiveLimitBytes())));
        }
    }

    /**
     * Computes the maximum memory footprint between two MemoryFootprint objects by taking the
     * maximum of each component.
     */
    static MemoryFootprint max(MemoryFootprint left, MemoryFootprint right) {
        return new MemoryFootprint(
                /* totalBytes= */ Math.max(left.totalBytes, right.totalBytes),
                /* maxActiveBytes= */ Math.max(left.maxActiveBytes, right.maxActiveBytes),
                /* maxAmbientBytes= */ Math.max(left.maxAmbientBytes, right.maxAmbientBytes));
    }
}
