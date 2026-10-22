/*
 * Copyright 2026 Google LLC
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

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.normalizeResourceReference;

import org.junit.Test;

public class WatchFaceDocumentsTest {

    @Test
    public void normalizeResourceReference_stripsDrawableTypePrefix() {
        assertThat(normalizeResourceReference("@drawable/shape_blob_ring"))
                .isEqualTo("shape_blob_ring");
    }

    @Test
    public void normalizeResourceReference_stripsPackageAndTypePrefix() {
        assertThat(normalizeResourceReference("@com.example.watchface:drawable/image"))
                .isEqualTo("image");
    }

    @Test
    public void normalizeResourceReference_keepsBareName() {
        assertThat(normalizeResourceReference("shape_blob_ring")).isEqualTo("shape_blob_ring");
    }

    @Test
    public void normalizeResourceReference_keepsExpression() {
        assertThat(normalizeResourceReference("[COMPLICATION.SMALL_IMAGE]"))
                .isEqualTo("[COMPLICATION.SMALL_IMAGE]");
    }

    @Test
    public void normalizeResourceReference_keepsEmptyReference() {
        assertThat(normalizeResourceReference("")).isEmpty();
    }

    @Test
    public void normalizeResourceReference_keepsMalformedReference() {
        assertThat(normalizeResourceReference("@drawable/")).isEqualTo("@drawable/");
        assertThat(normalizeResourceReference("@image")).isEqualTo("@image");
    }
}
