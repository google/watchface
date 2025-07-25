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

package com.samsung.watchface;

import com.samsung.watchface.utils.UnzipUtility;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

class ResourceManager {
    private static final String RESOURCE_ZIP_XSD_DOCS = "/docs.zip";

    private final File xsdTempDirectory;

    ResourceManager() {
        xsdTempDirectory = createTempDirectory();
        UnzipUtility.unzip(
            this.getClass().getResourceAsStream(RESOURCE_ZIP_XSD_DOCS),
            xsdTempDirectory.toString()
        );
    }

    File getXsdFile(String version) {
        return new File(xsdTempDirectory + File.separator +
                version + File.separator + "watchface.xsd");
    }

    private File createTempDirectory() {
        try {
            Path dirPath = Files.createTempDirectory("validator");
            deleteFileAndContentOnExit(dirPath);
            return dirPath.toFile();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void deleteFileAndContentOnExit(Path dirPath) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (Files.exists(dirPath)) {
                try (Stream<Path> walk = Files.walk(dirPath)) {
                    walk.sorted(Comparator.reverseOrder()) // Reverse order to delete contents first
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Do nothing
                            }
                        });
                } catch (IOException e) {
                    System.err.println("Failed to delete temp dir " + dirPath);
                }
            }
        }));
    }
}
