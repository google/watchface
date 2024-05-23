package com.google.wear.watchface.dfx.memory;

import java.nio.file.Path;

/**
 * Represents a resource, as defined in the resources.arsc file within an Android package.
 */
public class ArscResource {
    // Resource type, for example "raw", "asset", "drawable" etc.
    private final String resourceType;

    // Resource name, for example "watchface" for res/raw/watchface.xml.
    private final String resourceName;

    // File extension of the resource, for example "xml" for res/raw/watchface.xml
    private final String extension;

    // Path in the package. This is the obfuscated path to the actual data, where obfuscation has
    // been used, for example "res/raw/watchface.xml" may point to something like "res/li.xml".
    private final Path filePath;

    // The resource data itself.
    private final byte[] data;

    public ArscResource(
            String resourceType,
            String resourceName,
            String extension,
            Path filePath,
            byte[] data
    ) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.extension = extension;
        this.filePath = filePath;
        this.data = data;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getExtension() {
        return extension;
    }

    public Path getFilePath() {
        return filePath;
    }

    public byte[] getData() {
        return data;
    }

    public Boolean isWatchFaceXml() {
        return extension.equals("xml") && resourceType.equals("raw");
    }

    public Boolean isDrawable() {
        return resourceType.equals("drawable");
    }

    public Boolean isFont() {
        return resourceType.equals("font");
    }

    public Boolean isAsset() {
        return resourceType.equals("asset");
    }

    public Boolean isRaw() {
        return resourceType.equals("raw");
    }

    public Boolean isWatchFaceInfoFile() {
        return resourceType.equals("xml") &&
                extension.equals("xml") &&
                resourceName.equals("watch_face_info");
    }
}
