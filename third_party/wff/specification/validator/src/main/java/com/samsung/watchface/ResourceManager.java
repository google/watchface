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
import java.util.Objects;

class ResourceManager {
    private static final String RESOURCE_ZIP_XSD_DOCS = "/docs.zip";
    private static final int BUFFER_SIZE = 4096;

    private final File xsdTempDirectory;

    ResourceManager() {
        // load resources
        String zipFilePath = getResourceAsFile(RESOURCE_ZIP_XSD_DOCS).toString();
        xsdTempDirectory = createTempDirectory();
        UnzipUtility.unzip(zipFilePath, xsdTempDirectory.toString());
    }

    File getXsdFile(String version) {
        return new File(xsdTempDirectory + File.separator +
                version + File.separator + "watchface.xsd");
    }

    private File getResourceAsFile(String resource) {
        File file;
        URL res = getClass().getResource(resource);
        if (res == null) {
            throw new RuntimeException("No resource File : " + resource);
        }
        if (res.getProtocol().equals("jar")) {
            try {
                file = File.createTempFile("dwf_temp", "tmp");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            file.deleteOnExit();

            try (InputStream input = getClass().getResourceAsStream(resource);
                 OutputStream output = Files.newOutputStream(file.toPath())) {
                byte[] bytes = new byte[BUFFER_SIZE];
                int read;
                while ((read = Objects.requireNonNull(input).read(bytes)) != -1) {
                    output.write(bytes, 0, read);
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        } else { // for IDE
            file = new File(res.getFile());
        }

        if (!file.exists()) {
            throw new RuntimeException(
                    "Error: Cannot read Resource File " + file + "(" + resource + ")!");
        }
        return file;
    }

    private File createTempDirectory() {
        try {
            File file = new File(Files.createTempDirectory("validator").toString());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
