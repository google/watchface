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

import com.samsung.watchface.utils.Log;
import com.samsung.watchface.utils.OptionParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class DWFValidationApplication {
    private final static String APPLICATION_NAME = "wff-validator";
    private final static String MAX_SUPPORTED_FORMAT_VERSION = "2";
    private WatchFaceXmlValidator validator;

    public static void main(String[] args) {
        DWFValidationApplication application = new DWFValidationApplication();
        OptionParser optionParser = new OptionParser(SupportedOptions.optionStrings);

        if (args.length >= 2) {
            try {
                if (Integer.parseInt(args[0]) > Integer.parseInt(MAX_SUPPORTED_FORMAT_VERSION)) {
                    Log.e("Maximum supported version is " + MAX_SUPPORTED_FORMAT_VERSION);
                    return;
                }
            } catch (NumberFormatException e) {
                printUsage();
                return;
            }

            List<String> params = new ArrayList<>(Arrays.asList(args));
            params.remove(0); // remove version arg from args
            List<String> xmlPathList = optionParser.parse(params.toArray(new String[0]));
            if (optionParser.hasOption(SupportedOptions.HELP.arg)) {
                printOptions();
                printUsage();
                System.exit(0);
            }

            boolean stopOnFail = optionParser.hasOption(SupportedOptions.STOP_ON_FAIL.arg);
            for (String xmlPath : xmlPathList) {
                if (!application.validateXml(xmlPath, args[0]) && stopOnFail) {
                    System.exit(1);
                }
            }
        } else {
            printUsage();
        }
    }

    public DWFValidationApplication() {
        Log.i("WFF Validation Application Version "+ getVersion() +
                ". Maximum Supported Format Version #" + MAX_SUPPORTED_FORMAT_VERSION);
    }

    private static void printOptions() {
        System.out.println("List of Options");
        Arrays.stream(SupportedOptions.values()).forEach(option -> {
           System.out.println(option.arg + " : " + option.description);
        });
    }

    private static void printUsage() {
        System.out.println("Usage : " +
                "java -jar " + APPLICATION_NAME + ".jar " +
                " <format-version> <any options> <your-watchface.xml> <more-watchface.xml> ...");
    }

    /**
     * Validate watch face format xml.
     *
     * @param watchFaceXmlFilePath path of the dwf-formatted xml file
     * @param targetFormatVersion version of the dwf-formatted xml file
     * @return true if valid, or else false
     */
    public boolean validateXml(String watchFaceXmlFilePath, String targetFormatVersion) {
        if (validator == null) {
            validator = new WatchFaceXmlValidator();
        }

        if (validator.isSupportedVersion(targetFormatVersion)) {
            if (validator.validate(watchFaceXmlFilePath, targetFormatVersion)) {
                Log.i("✅ "+" PASSED : " + watchFaceXmlFilePath +
                        " is valid against watch face format version #" + targetFormatVersion);
                return true;
            } else {
                Log.i("❌ "+" FAILED : " + watchFaceXmlFilePath +
                        " is NOT valid against watch face format version #" + targetFormatVersion);
                return false;
            }
        } else {
            Log.i("Not supported version : " + targetFormatVersion);
            return false;
        }
    }

    /**
     * Get the version of the application from the MANIFEST.MF file.
     *
     * @return The version of the application
     */
    private static String getVersion() {
        String className = DWFValidationApplication.class.getName().replace('.', '/') + ".class";
        String classJar = DWFValidationApplication.class.getResource("/" + className).toString();
        if (classJar.startsWith("jar:")) {
            String manifestPath = classJar.substring(0, classJar.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest = null;
            try {
                manifest = new Manifest(new URL(manifestPath).openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Attributes attr = manifest.getMainAttributes();
            return attr.getValue("Version");
        }
        throw new RuntimeException("Version not found in manifest");
    }
}
