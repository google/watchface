package com.google.wear.watchface.validator

import java.io.FileNotFoundException
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document

object XmlReader {
    /** Reads an Xml file and returns it as a w3c.dom.Document object */
    fun fromResource(resourcePath: String): Document {
        val xmlStream = object {}.javaClass.getResourceAsStream(resourcePath)

        if (xmlStream != null) {
            return readFromInputStream(xmlStream)
        } else {
            throw FileNotFoundException()
        }
    }

    fun fromFilePath(filePath: String): Document {
        val xmlStream = java.io.File(filePath).inputStream()

        return readFromInputStream(xmlStream)
    }

    /** Helper function for building the Document object from an xml input stream */
    fun readFromInputStream(inputStream: InputStream): Document {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

        return builder.parse(inputStream)
    }
}
