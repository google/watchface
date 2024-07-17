package com.google.wear.watchface.dfx.memory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a resource, from an AAB or APK.
 */
public class AndroidResource {
    private static final Pattern VALID_RESOURCE_PATH = Pattern.compile(".*res/([^-/]+).*/([^.]+)(\\.|)(.*|)$");
    private static final int VALID_RESOURCE_GROUPS = 4;

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

    public AndroidResource(
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

    public String getResourceName() {
        return resourceName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public byte[] getData() {
        return data;
    }

    // TODO: This should be improved to parse res/xml/watch_face_info.xml where present, so as not
    // to assume all XML files in the res/raw directory are watch face XML files.
    public Boolean isWatchFaceXml() {
        return "xml".equals(extension) && "raw".equals(resourceType);
    }

    public Boolean isDrawable() { return "drawable".equals(resourceType); }

    public Boolean isFont() { return resourceType.equals("font"); }

    public Boolean isAsset() { return "asset".equals(resourceType); }

    public Boolean isRaw() { return "raw".equals(resourceType); }

    static AndroidResource fromPath(Path filePath, byte[] data) {
        String pathWithFwdSlashes = filePath.toString().replace('\\', '/');
        Matcher m = VALID_RESOURCE_PATH.matcher(pathWithFwdSlashes);
        if (m.matches() && m.groupCount() == VALID_RESOURCE_GROUPS) {
            String resType = m.group(1);
            String resName = m.group(2);
            String ext = m.group(4);
            return new AndroidResource(
                    resType,
                    resName,
                    ext,
                    filePath,
                    data
            );
        }
        throw new RuntimeException("Not a valid resource file: " + m.matches());
    }

    static AndroidResource fromPath(String filePath, byte[] data) {
        return fromPath(Paths.get(filePath), data);
    }

    static Boolean isValidResourcePath(Path filePath) {
        Matcher m = VALID_RESOURCE_PATH.matcher(filePath.toString());
        return m.matches() && m.groupCount() == VALID_RESOURCE_GROUPS;
    }

    static Boolean isValidResourcePath(String filePath) {
        return isValidResourcePath(Paths.get(filePath));
    }
}
