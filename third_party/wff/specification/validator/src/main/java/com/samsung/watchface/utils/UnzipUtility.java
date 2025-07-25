/*
 * Copyright 2023 Samsung Electronics Co., Ltd All Rights Reserved
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

package com.samsung.watchface.utils;

import static java.nio.file.Files.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    private static final int BUFFER_SIZE = 4096;

    /**
     * Unzips a ZIP file from an InputStream to a destination directory.
     *
     * @param inputStream     The stream of the ZIP file to unzip.
     * @param destinationDir  The directory where files will be extracted.
     * @throws IOException    If an I/O error occurs.
     */
    public static void unzip(InputStream inputStream, Path destinationDir) throws IOException {
        // Create a buffer for reading/writing data
        byte[] buffer = new byte[BUFFER_SIZE];

        // Use try-with-resources to ensure the stream is closed automatically
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                Path newFilePath = destinationDir.resolve(zipEntry.getName());

                // SECURITY CHECK: Prevent Zip Slip vulnerability
                if (!newFilePath.toAbsolutePath().normalize().startsWith(destinationDir.toAbsolutePath().normalize())) {
                    throw new IOException("Bad zip entry: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    // If the entry is a directory, create it
                    if (!isDirectory(newFilePath)) {
                        createDirectories(newFilePath);
                    }
                } else {
                    // If the entry is a file, write it out
                    // Ensure parent directories exist
                    Path parentDir = newFilePath.getParent();
                    if (parentDir != null && !isDirectory(parentDir)) {
                        createDirectories(parentDir);
                    }

                    // Write the file content
                    try (FileOutputStream fos = new FileOutputStream(newFilePath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
    }
}