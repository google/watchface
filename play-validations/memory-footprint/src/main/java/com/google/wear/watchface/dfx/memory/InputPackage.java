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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Exposes the files of a watch face package as a stream of watch face package files. It is backed
 * by a closeable resource, reading the watch face package zip, so it must be closed after consuming
 * the package.
 */
interface InputPackage extends AutoCloseable {
    /**
     * Returns a stream of resource representations form the watch face package. The stream must not
     * be consumed more than once. The InputPackage must be closed only after consuming the stream
     * of files.
     */
    Stream<ArscResource> getWatchFaceFiles();

    /** Close the backing watch face package resource. */
    void close();

    /** Creates an input package from a declarative watch face package. */
    static InputPackage open(String packagePath) throws IOException {
        File packageFile = new File(packagePath);
        if (!packageFile.exists()) {
            throw new IllegalArgumentException(
                    String.format("Package path %s does not exist", packagePath));
        }
        if (packageFile.isDirectory()) {
            return openFromAabDirectory(packageFile);
        } else if (packagePath.endsWith("aab") || packagePath.endsWith("apk")) {
            return openFromAndroidPackage(packagePath);
        } else {
            throw new IllegalArgumentException("Incorrect file type");
        }
    }

    /**
     * Creates an input package from a directory containing the structure of a Declarative Watch
     * Face AAB. Each file is relative to the base module of the directory.
     */
    static InputPackage openFromAabDirectory(File aabDirectory) throws IOException {
        Path rootPath = aabDirectory.toPath();
        return new InputPackage() {
            @Override
            public Stream<ArscResource> getWatchFaceFiles() {
                try {
                    ArscTable table = ArscTable.createFromAabDirectory(rootPath);
                    return table.getAllResources().stream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void close() {

            }
        };
    }

    /**
     * Creates an input package from a declarative watch face AAB. Each file is relative to the base
     * module of the app bundle. Every other module will be ignored.
     */
    static InputPackage openFromAndroidPackage(String aabPath) throws IOException {
        final ZipFile zipFile = new ZipFile(aabPath);
        return new InputPackage() {
            @Override
            public Stream<ArscResource> getWatchFaceFiles() {
                try {
                    ArscTable table = ArscTable.createFromAndroidPackage(zipFile);
                    return table.getAllResources().stream();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void close() {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
