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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    private static final int BUFFER_SIZE = 4096;

    public static void unzip(String zipFilePath, String destDirectory) {
        tryCreateDirectory(destDirectory);

        try (FileInputStream fileIn = new FileInputStream(zipFilePath);
             ZipInputStream zipIn = new ZipInputStream(fileIn)) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                final String filePath = destDirectory + File.separator + entry.getName();
                if (entry.isDirectory()) {
                    tryCreateDirectory(filePath);
                } else {
                    tryExtractFile(zipIn, filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void tryCreateDirectory(String directory) {
        File destDir = new File(directory);
        if (!destDir.exists()) {
            if (!destDir.mkdir()) {
                throw new RuntimeException("Couldn't create directory : " + directory);
            }
        }
    }

    private static void tryExtractFile(ZipInputStream zipIn, String filePath) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath);
             BufferedOutputStream outStream = new BufferedOutputStream(fileOutputStream)) {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read;
            while ((read = zipIn.read(bytesIn)) != -1) {
                outStream.write(bytesIn, 0, read);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}