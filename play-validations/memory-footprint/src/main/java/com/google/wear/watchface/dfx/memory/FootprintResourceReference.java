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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A reference to a drawable resource that must be counted towards the memory footprint in either
 * ambient or active.
 */
class FootprintResourceReference {

    /** The type of the reference. */
    enum Type {
        /**
         * A "Total" reference counts the total footprint of that drawable in the memory footprint.
         */
        TOTAL,
        /**
         * A "BiggestFrame" reference counts only the biggest frame of the drawable in the memory
         * footprint. It is relevant for GIFs and WEBPs, which can have more than one frame, but in
         * ambient we want to count only the biggest frame of that resource in the memory footprint
         * value because animations are paused in ambient.
         */
        BIGGEST_FRAME
    }

    /**
     * Constructs a FootprintResourceReference object with the type of "BiggestFrame".
     *
     * @param resourceName the name of the referenced resource.
     */
    static FootprintResourceReference biggestFrameOf(String resourceName) {
        return new FootprintResourceReference(resourceName, Type.BIGGEST_FRAME);
    }

    /**
     * Constructs a FootprintResourceReference object with the type of "Total".
     *
     * @param resourceName the name of the referenced resource.
     */
    static FootprintResourceReference totalOf(String resourceName) {
        return new FootprintResourceReference(resourceName, Type.TOTAL);
    }

    private final String name;

    private final Type type;

    private FootprintResourceReference(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /** The name of the referenced resource. */
    public String getName() {
        return name;
    }

    /**
     * The type of the reference.
     *
     * @see Type
     */
    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FootprintResourceReference)) return false;
        FootprintResourceReference that = (FootprintResourceReference) o;
        return Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    /**
     * Gets the memory footprint of the current resource reference from all the resource details
     * from the watch face, based on the current reference type.
     *
     * @param memoryFootprints a map holding the resource details found in the watch face package.
     *     The keys match resource reference names.
     * @return the memory footprint in bytes of this reference, based on its type. If it is a
     *     reference of type {@link Type#TOTAL}, then returns the total footprint of this resource.
     *     If it is of type {@link Type#BIGGEST_FRAME}, return just the footprint of this resource's
     *     biggest frame.
     * @throws TestFailedException if any of the assets referenced by the watch face does not exist
     *     in the watch face package.
     */
    long getFootprintBytes(Map<String, DrawableResourceDetails> memoryFootprints) {
        DrawableResourceDetails drawableResourceDetails = memoryFootprints.get(getName());
        if (drawableResourceDetails == null) {
            throw new TestFailedException(
                    String.format("Asset %s was not found in the watch face package", getName()));
        }
        switch (getType()) {
            case TOTAL:
                return drawableResourceDetails.getTotalFootprintBytes();
            case BIGGEST_FRAME:
                return drawableResourceDetails.getBiggestFrameFootprintBytes();
            default:
                throw new IllegalStateException(
                        String.format("Resource reference of type %s not supported", getType()));
        }
    }

    /**
     * Calculates the memory footprint of the given resource references, in bytes.
     *
     * @param memoryFootprints the memory footprint table with all the resources.
     * @param resources the resource references to be evaluated.
     * @throws TestFailedException if any of the resources referenced by the watch face does not
     *     exist in the watch face package.
     */
    static long sumDrawableResourceFootprintBytes(
            Map<String, DrawableResourceDetails> memoryFootprints,
            Set<FootprintResourceReference> resources) {
        return resources.stream()
                .mapToLong(asset -> asset.getFootprintBytes(memoryFootprints))
                .sum();
    }
}
