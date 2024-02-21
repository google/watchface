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

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * Validator of the watchface.xml
 */
public class WatchFaceXmlValidator {
    private final ResourceManager resourceManager;

    public WatchFaceXmlValidator() {
        // load resources for validation via xsd documents
        resourceManager = new ResourceManager();
    }

    /**
     * Check whether the validator support the version or not
     *
     * @param version version code
     * @return true if supported, else false
     */
    public boolean isSupportedVersion(String version) {
        File xsdFile = resourceManager.getXsdFile(version);
        return xsdFile != null && xsdFile.exists();
    }

    /**
     * Validate watch face format xml via specified version of the watch face xsd file.
     *
     * @param xmlPath valid full path of the xml
     * @param version version of the watch face format
     * @return true if valid, else false
     */
    public boolean validate(String xmlPath, String version) {
        try {
            File xmlFile = new File(xmlPath);
            if (!isSupportedVersion(version)) {
                throw new RuntimeException("Validator not support the version #" + version);
            }
            if (!xmlFile.exists()) {
                throw new RuntimeException("xml path is invalid : " + xmlPath);
            }
            validateXMLSchema(
                resourceManager.getXsdFile(version).getCanonicalPath(), new StreamSource(xmlFile));
            return true;
        } catch (SAXParseException e) {
            String errorMessage = String.format(
                    "[Line %d:Column %d]: %s",
                    e.getLineNumber(),
                    e.getColumnNumber(),
                    e.getMessage()
                    );
            Log.e(errorMessage);
            return false;
        } catch (Exception e) {
            Log.e(e.getMessage());
            return false;
        }
    }

    /**
     * Validate watch face format xml via specified version of the watch face xsd file.
     *
     * @param xmlDocument the watchface layout document
     * @param version version of the watch face format
     * @return true if valid, else false
     */
    public boolean validate(Document xmlDocument, String version) {
        try {
            if (!isSupportedVersion(version)) {
                throw new RuntimeException("Validator not support the version #" + version);
            }
            validateXMLSchema(
                resourceManager.getXsdFile(version).getCanonicalPath(), new DOMSource(xmlDocument));
            return true;
        } catch (Exception e) {
            Log.e(e.getMessage());
            return false;
        }
    }

    private static void validateXMLSchema(String xsdPath, Source xmlSource) throws
            IllegalArgumentException, SAXException, IOException, NullPointerException {
        // https://stackoverflow.com/questions/20807066/how-to-validate-xml-against-xsd-1-1-in-java
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1");
        Schema schema = factory.newSchema(new File(xsdPath));
        Validator validator = schema.newValidator();
        validator.validate(xmlSource);
    }
}