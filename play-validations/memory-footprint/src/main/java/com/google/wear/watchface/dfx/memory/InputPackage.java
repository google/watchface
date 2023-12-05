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

    /** Represents a file in a watch face package (apk or aab). */
    class PackageFile {

        private final Path filePath;

        private final byte[] data;

        PackageFile(Path filePath, byte[] data) {
            this.filePath = filePath;
            this.data = data;
        }

        /** File path relative to the root of the declarative watch face package. */
        public Path getFilePath() {
            return filePath;
        }

        /** File raw data. */
        public byte[] getData() {
            return data;
        }
    }

    /**
     * Returns a stream of file representations form the watch face package. The stream must not be
     * consumed more than once. The InputPackage must be closed only after consuming the stream of
     * files.
     */
    Stream<PackageFile> getWatchFaceFiles();

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
        } else if (packagePath.endsWith("zip")) {
            // TODO(b/279866804): if open sourcing, skip the zip case because it is irrelevant
            // for the outside world.
            return openFromMokkaZip(packagePath);
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
        Stream<Path> childrenFilesStream = Files.walk(rootPath);
        return new InputPackage() {
            @Override
            public Stream<PackageFile> getWatchFaceFiles() {
                return childrenFilesStream
                        .filter(childPath -> childPath.toFile().isFile())
                        .map(
                                childPath -> {
                                    byte[] fileContent;
                                    try {
                                        fileContent = Files.readAllBytes(childPath);
                                    } catch (IOException e) {
                                        throw new RuntimeException(
                                                "Cannot read file " + childPath, e);
                                    }
                                    return new PackageFile(
                                            rootPath.relativize(childPath), fileContent);
                                });
            }

            @Override
            public void close() {
                childrenFilesStream.close();
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
            public Stream<PackageFile> getWatchFaceFiles() {
                return zipFile.stream()
                        .map(
                                entry -> {
                                    byte[] fileData;
                                    try {
                                        fileData = readAllBytes(zipFile.getInputStream(entry));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return new PackageFile(Paths.get(entry.getName()), fileData);
                                });
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

    /**
     * Creates an input package from a zip file containing the base split apk, as produced by mokka.
     * TODO(b/279866804): Ignore this if open sourcing this method to not leak mokka details.
     */
    static InputPackage openFromMokkaZip(String zipPath) throws IOException {
        ZipFile mokkaZip = new ZipFile(zipPath);

        try {
            Pattern baseSplitPattern = Pattern.compile(".*base[-_]split.*");
            Optional<? extends ZipEntry> baseSplitApk =
                    mokkaZip.stream()
                            .filter(x -> baseSplitPattern.matcher(x.getName()).matches())
                            .findFirst();
            if (!baseSplitApk.isPresent()) {
                throw new InvalidTestRunException("Zip file does not contain a base split apk");
            }
            ZipInputStream baseSplitApkZip =
                    new ZipInputStream(mokkaZip.getInputStream(baseSplitApk.get()));
            Iterator<PackageFile> iterator =
                    new Iterator<PackageFile>() {
                        private ZipEntry zipEntry;

                        @Override
                        public boolean hasNext() {
                            try {
                                zipEntry = baseSplitApkZip.getNextEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return zipEntry != null;
                        }

                        @Override
                        public PackageFile next() {
                            byte[] entryData;
                            try {
                                entryData = readAllBytes(baseSplitApkZip);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return new PackageFile(Paths.get(zipEntry.getName()), entryData);
                        }
                    };
            return new InputPackage() {
                @Override
                public Stream<PackageFile> getWatchFaceFiles() {
                    return StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                            false);
                }

                @Override
                public void close() {
                    try {
                        mokkaZip.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } catch (Exception e) {
            mokkaZip.close();
            throw e;
        }
    }

    /** Read all bytes from an input stream to a new byte array. */
    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        int len;
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }

    static boolean pathMatchesGlob(Path path, String glob) {
        return path.getFileSystem().getPathMatcher("glob:" + glob).matches(path);
    }
}
