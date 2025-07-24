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

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Details about a drawable resource that are relevant for the memory footprint calculation. */
class DrawableResourceDetails {
    private static final int CHANNEL_MASK_A = 0xff000000;

    /**
     * A lookup table used for computing the loss of precision where an 8bit value is quantized to a
     * 5bit value.
     */
    private static final int QUANTIZATION_ERROR_LUT5[] =
            create8bppToNbppQuantizationErrorLookUpTable(5);

    /**
     * A lookup table used for computing the loss of precision where an 8bit value is quantized to a
     * 6bit value.
     */
    private static final int QUANTIZATION_ERROR_LUT6[] =
            create8bppToNbppQuantizationErrorLookUpTable(6);

    /** This corresponds to an average difference in luminosity of 5/10th of an 8bit value. */
    private static final double MAX_ACCEPTABLE_QUANTIZATION_ERROR = 0.5f;

    public static class Bounds {
        int left;
        int top;
        int right;
        int bottom;

        Bounds() {}

        Bounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        int getWidth() {
            return right - left;
        }

        int getHeight() {
            return bottom - top;
        }

        Bounds computeUnion(Bounds other) {
            Bounds result = new Bounds();
            result.left = min(left, other.left);
            result.top = min(top, other.top);
            result.right = max(right, other.right);
            result.bottom = max(bottom, other.bottom);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Bounds)) return false;
            Bounds bounds = (Bounds) o;
            return left == bounds.left
                    && top == bounds.top
                    && right == bounds.right
                    && bottom == bounds.bottom;
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, top, right, bottom);
        }

        @Override
        public java.lang.String toString() {
            return "Bounds{"
                    + "left="
                    + left
                    + ", top="
                    + top
                    + ", right="
                    + right
                    + ", bottom="
                    + bottom
                    + '}';
        }
    }

    /**
     * Evaluates the memory footprint of a drawable asset. The memory footprint is defined as
     * number_of_frames * width * height * 4 bytes, or how much memory does storing that resource
     * take in its uncompressed format.
     *
     * @param resource the resource from a watch face package.
     * @param imageProcessor the image processing implementation.
     * @return the memory footprint of that asset file or {@code Optional.empty()} if the file is
     *     not a drawable asset.
     * @throws java.lang.IllegalArgumentException when the image cannot be processed.
     */
    static Optional<DrawableResourceDetails> fromPackageResource(
            AndroidResource resource, ImageProcessor imageProcessor) {
        // For fonts we assume the raw size of the resource is the MCU footprint.
        if (resource.isFont()) {
            return Optional.of(
                    new Builder()
                            .setName(resource.getResourceName())
                            .setNumberOfImages(1)
                            .setBiggestFrameFootprintBytes(resource.getData().length)
                            .build());
        }

        boolean isPossibleImage = resource.isAsset() || resource.isDrawable() || resource.isRaw();

        if (!isPossibleImage) {
            return Optional.empty();
        }

        String sha1;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            sha1 = byteArray2Hex(md.digest(resource.getData()));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("Error while processing image %s", resource.getFilePath()), e);
        }

        ImageProcessor.ImageReader reader =
                imageProcessor.createImageReader(new ByteArrayInputStream(resource.getData()));

        if (reader == null) {
            return Optional.empty();
        }

        int numberOfImages = reader.getNumImages();
        int maxWidth = 0;
        int maxHeight = 0;
        double maxQuantizationError = 0.0;
        DrawableResourceDetails.Bounds accumulatedBounds = null;

        for (int i = 0; i < numberOfImages; i++) {
            ImageProcessor.ImageData image = reader.read(i);

            Bounds bounds = computeBounds(image);
            if (numberOfImages == 1) {
                maxWidth = image.getWidth();
                maxHeight = image.getHeight();
            }

            if (bounds != null) {
                // In case of multiple frames, largest one is taken.
                // Note: Only visible pixels are taken to compute the width and height.
                maxWidth = max(bounds.getWidth(), maxWidth);
                maxHeight = max(bounds.getHeight(), maxHeight);

                if (accumulatedBounds == null) {
                    accumulatedBounds = bounds;
                } else {
                    accumulatedBounds = accumulatedBounds.computeUnion(bounds);
                }
            }

            QuantizationStats stats = computeQualtizationStats(image);
            maxQuantizationError = max(maxQuantizationError, stats.getVisibleError());
        }

        long biggestFrameMemoryFootprint = ((long) maxWidth) * maxHeight * 4;
        boolean canBeQuantized = (maxQuantizationError < MAX_ACCEPTABLE_QUANTIZATION_ERROR);

        return Optional.of(
                new Builder()
                        .setName(resource.getResourceName())
                        .setNumberOfImages(numberOfImages)
                        .setBiggestFrameFootprintBytes(biggestFrameMemoryFootprint)
                        .setBounds(accumulatedBounds)
                        .setWidth(maxWidth)
                        .setHeight(maxHeight)
                        .setSha1(sha1)
                        .setCanUseRGB565(canBeQuantized)
                        .build());
    }

    private final String name;
    private final long numberOfImages;
    private long biggestFrameFootprintBytes;
    private final Bounds bounds;
    private int width;
    private int height;
    private final String sha1;
    private final boolean canUseRGB565;

    private DrawableResourceDetails(
            String name,
            long numberOfImages,
            long biggestFrameFootprintBytes,
            Bounds bounds,
            int width,
            int height,
            String sha1,
            boolean canUseRGB565) {
        this.name = name;
        this.numberOfImages = numberOfImages;
        this.biggestFrameFootprintBytes = biggestFrameFootprintBytes;
        this.bounds = bounds;
        this.width = width;
        this.height = height;
        this.sha1 = sha1;
        this.canUseRGB565 = canUseRGB565;
    }

    String getName() {
        return name;
    }

    /** The SHA-1 digest in hexadecimal. */
    String getSha1() {
        return sha1;
    }

    /**
     * Whether this image can be quantised into an RGB565 image with out loosing too much visual
     * fidelity.
     */
    boolean canUseRGB565() {
        return canUseRGB565;
    }

    /**
     * The total footprint of a drawable is defined as 4 * width * height * number_of_frames, where
     * the number_of_frames is 1 for static images and a positive number fog GIFs and WEBPs. It
     * represents the number of bytes that are necessary to store this drawable in memory in its
     * uncompressed form.
     */
    long getTotalFootprintBytes() {
        return biggestFrameFootprintBytes * numberOfImages;
    }

    /**
     * The biggest frame footprint is the size of the biggest frame of this drawable. For static
     * images (JPEGs, PNGs, BMPs) it is equal to the total footprint, while for GIFs and PNGs it is
     * smaller than the total footprint.
     */
    long getBiggestFrameFootprintBytes() {
        return biggestFrameFootprintBytes;
    }

    public Bounds getBounds() {
        return bounds;
    }

    /** The actual width of the drawable resource file. */
    int getWidth() {
        return width;
    }

    /** The actual height of the drawable resource file. */
    int getHeight() {
        return height;
    }

    public void setOptimizedSizeAndBytes(long optimizedBytes, int width, int height) {
        this.biggestFrameFootprintBytes = optimizedBytes;
        this.width = width;
        this.height = height;
    }

    /**
     * Computes a maximal resource details object, containing the maximum between the current total
     * footprint and the other total footprint and between the current biggest frame footprint and
     * the other biggest frame footprint.
     */
    public DrawableResourceDetails maxResourceDetails(DrawableResourceDetails other) {
        if (!name.equals(other.name)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot compute max resource details for %s and %s. Maximum "
                                    + "resource details can only be computed for the same resource",
                            name, other.name));
        }
        return DrawableResourceDetails.builder()
                .setName(name)
                .setNumberOfImages(1)
                .setBiggestFrameFootprintBytes(
                        max(
                                biggestFrameFootprintBytes * numberOfImages,
                                other.biggestFrameFootprintBytes * other.numberOfImages))
                .build();
    }

    @Override
    public java.lang.String toString() {
        return "DrawableResourceDetails{"
                + "name='"
                + name
                + '\''
                + ", numberOfImages="
                + numberOfImages
                + ", biggestFrameFootprintBytes="
                + biggestFrameFootprintBytes
                + ", bounds="
                + bounds
                + ", width="
                + width
                + ", height="
                + height
                + ", sha1='"
                + sha1
                + "', canUseRGB565="
                + canUseRGB565
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DrawableResourceDetails)) return false;
        DrawableResourceDetails that = (DrawableResourceDetails) o;
        return numberOfImages == that.numberOfImages
                && biggestFrameFootprintBytes == that.biggestFrameFootprintBytes
                && width == that.width
                && height == that.height
                && Objects.equals(name, that.name)
                && Objects.equals(bounds, that.bounds)
                && Objects.equals(sha1, that.sha1)
                && canUseRGB565 == that.canUseRGB565;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                numberOfImages,
                biggestFrameFootprintBytes,
                bounds,
                width,
                height,
                sha1,
                canUseRGB565);
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private String name;
        private long numberOfImages;
        private long biggestFrameFootprintBytes;
        private Bounds bounds;
        private int width;
        private int height;
        private String sha1;
        private boolean canUseRGB565;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNumberOfImages(long numberOfImages) {
            this.numberOfImages = numberOfImages;
            return this;
        }

        public Builder setBiggestFrameFootprintBytes(long biggestFrameFootprintBytes) {
            this.biggestFrameFootprintBytes = biggestFrameFootprintBytes;
            return this;
        }

        public Builder setBounds(Bounds bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setSha1(String sha1) {
            this.sha1 = sha1;
            return this;
        }

        public Builder setCanUseRGB565(boolean canUseRGB565) {
            this.canUseRGB565 = canUseRGB565;
            return this;
        }

        public DrawableResourceDetails build() {
            return new DrawableResourceDetails(
                    name,
                    numberOfImages,
                    biggestFrameFootprintBytes,
                    bounds,
                    width,
                    height,
                    sha1,
                    canUseRGB565);
        }
    }

    /**
     * Reads the image with the specified index and then computes the {@link Bounds} of the visible
     * pixels.
     *
     * @param image the {@link ImageProcessor.ImageData}
     * @return The {@link Bounds} of the visible pixels.
     */
    private static Bounds computeBounds(ImageProcessor.ImageData image) {
        Bounds bounds = new Bounds();

        // Scan from the top down to find the first non-transparent row.
        int height = image.getHeight();
        int y;
        for (y = 0; y < height; y++) {
            if (!isRowFullyTransparent(image, y)) {
                bounds.top = y;
                break;
            }
        }

        if (y == height) {
            // The image is fully transparent.
            return null;
        }

        // Scan from the bottom up to find the first non-transparent row.
        for (y = height; y > 0; ) {
            y--;
            if (!isRowFullyTransparent(image, y)) {
                bounds.bottom = y + 1;
                break;
            }
        }

        // Scan from left to right to find the first non-transparent column.
        int width = image.getWidth();
        int x;
        for (x = 0; x < width; x++) {
            if (!isColumnFullyTransparent(image, x, bounds.top, bounds.bottom)) {
                bounds.left = x;
                break;
            }
        }

        for (x = width; x > 0; ) {
            x--;
            if (!isColumnFullyTransparent(image, x, bounds.top, bounds.bottom)) {
                bounds.right = x + 1;
                break;
            }
        }

        return bounds;
    }

    private static boolean isRowFullyTransparent(ImageProcessor.ImageData image, int y) {
        int width = image.getWidth();
        for (int x = 0; x < width; x++) {
            if (!isFullyTransparent(image.getRgb(x, y))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isColumnFullyTransparent(
            ImageProcessor.ImageData image, int x, int top, int bottom) {
        for (int y = top; y < bottom; y++) {
            if (!isFullyTransparent(image.getRgb(x, y))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFullyTransparent(int argb) {
        return (argb & CHANNEL_MASK_A) == 0;
    }

    private static class QuantizationStats {
        long visiblePixels = 0;
        long visiblePixelQuantizationErrorSum = 0;

        double getVisibleError() {
            return (double) visiblePixelQuantizationErrorSum / (double) visiblePixels;
        }
    }

    private static QuantizationStats computeQualtizationStats(ImageProcessor.ImageData image) {
        int width = image.getWidth();
        int height = image.getHeight();
        QuantizationStats quantizationStats = new QuantizationStats();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRgb(x, y);
                int a = (argb >> 24) & 0xff;
                if (a < 255) {
                    continue;
                }

                quantizationStats.visiblePixels++;

                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;

                quantizationStats.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT5[r];
                quantizationStats.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT6[g];
                quantizationStats.visiblePixelQuantizationErrorSum += QUANTIZATION_ERROR_LUT5[b];
            }
        }
        return quantizationStats;
    }

    /** Constructs a table of the error introduced by quantizing an 8 bit value to a N bit value. */
    private static int[] create8bppToNbppQuantizationErrorLookUpTable(int n) {
        int[] table = new int[256];
        int bitsLost = 8 - n;
        int twoPowN = 1 << bitsLost;
        int halfTwoPowN = 1 << (bitsLost - 1);
        for (int i = 0; i < 256; i++) {
            // This rounds i to the nearest n-bit value before converting back to an 8 bit value.
            int quantizedValue = min(((i + halfTwoPowN) / twoPowN) * twoPowN, 255);

            // Record the error due to quantization in the table. This has a saw-tooth pattern where
            // n-bit values that correspond directly to 8 bit ones have an error of 0, rising to a
            // maximum error of halfPlusOne in between.
            table[i] = abs(i - quantizedValue);
        }
        return table;
    }

    private static String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    static DrawableResourceDetails findInMap(
            Map<String, DrawableResourceDetails> resourceMemoryMap, String resourceName) {
        DrawableResourceDetails details = resourceMemoryMap.get(resourceName);
        if (details == null) {
            throw new TestFailedException(
                    String.format(
                            "Asset %s was not found in the watch face package", resourceName));
        }
        return details;
    }
}
