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
 * Exposes the files of a watch face package as a stream of watch face package files. It is backed
 * by a closeable resource, reading the watch face package zip, so it must be closed after consuming
 * the package.
 */
interface InputPackage : AutoCloseable {
    /**
     * Returns a stream of resource representations form the watch face package. The stream must not
     * be consumed more than once. The InputPackage must be closed only after consuming the stream
     * of files.
     */
    fun getWatchFaceFiles(): Sequence<AndroidResource>

    fun getManifest(): AndroidManifest?

    /** Close the backing watch face package resource. */
    override fun close()
}
