package com.google.wear.watchface.dfx.memory;

import com.google.common.io.Files;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import com.google.devrel.gmscore.tools.apk.arsc.TypeChunk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents all the resources of interest in the Android package.
 *
 * These resources are loaded from the resources.arsc file. Where obfuscation has been applied, the
 * mapping is derived, for the creation of the ArscResource objects. For example, for a logical
 * resource res/raw/watchface.xml, the package may in fact store this as res/aB.xml. The
 * resources.arsc file contains this mapping, and the ArscTable provides a list of resources with
 * their logical types, names and data.
 *
 * Note that more than one ArscResource object can exist for the given dimensions. For example, if
 * there is a drawable and a drawable-fr folder, then there may be multiple ArscResource entries for
 * drawables with the same type, name and extension. The configuration detail, e.g. "fr" or
 * "default", is not currently exposed in the ArscResource objects as it isn't used.
 */
public class ArscTable {
    // Only certain resource types are of interest to the evaluator, notably, not string resources.
    private static final Set<String> RESOURCE_TYPES = Set.of("raw", "xml", "drawable", "font", "asset");
    private static final String RESOURCES_FILE_NAME = "resources.arsc";
    private final List<ArscResource> resourceTable;

    private ArscTable(List<ArscResource> resources) {
        this.resourceTable = resources;
    }

    /**
     * Creates the table from a path to an AAB structure on the file system.
     * @param aabPath The path to the root of the AAB directory.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static ArscTable createFromAabDirectory(Path aabPath) throws IOException {
        Path arscPath = Paths.get(aabPath.toString(), RESOURCES_FILE_NAME);
        File arscFile = new File(arscPath.toAbsolutePath().toString());
        if (!arscFile.exists()) {
            throw new FileNotFoundException("Resources file not found");
        }
        InputStream is = new FileInputStream(arscFile);
        return createTable(is, null, aabPath);
    }

    /**
     * Creates the table from a AAB or APK file.
     * @param zipFile The zip file object representing the AAB/APK.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static ArscTable createFromAndroidPackage(ZipFile zipFile) throws IOException {
        ZipEntry arscEntry = new ZipEntry(RESOURCES_FILE_NAME);
        InputStream is = zipFile.getInputStream(arscEntry);
        return createTable(is, zipFile, null);
    }

    /**
     * Creates the table from an InputStream.
     *
     * @param is The InputStream
     * @param zipFile The source zip file. This must be populated when creating the table from a AAB
     *                or APK archive. Where the filesystem approach is taken, this should be null.
     * @param aabRoot The source AAB directory path. This should be null when creating the table
     *                from an AAB/APK archive.
     * @return The constructed table.
     * @throws IOException when errors loading resources occur.
     */
    private static ArscTable createTable(InputStream is, ZipFile zipFile, Path aabRoot) throws IOException {
        BinaryResourceFile resources = BinaryResourceFile.fromInputStream(is);
        ResourceTableChunk table = (ResourceTableChunk) resources.getChunks().get(0);
        StringPoolChunk stringPool = table.getStringPool();

        List<TypeChunk> chunks = table.getPackages()
                .stream()
                .flatMap(p -> p.getTypeChunks().stream())
                .toList();

        List<ArscResource> resourcesList = chunks.stream().flatMap(c -> c.getEntries().values().stream())
                .filter(t -> RESOURCE_TYPES.contains(t.typeName()))
                .filter(t -> t.value().type() == BinaryResourceValue.Type.STRING)
                .map(entry -> {
                    Path path = Path.of(stringPool.getString(entry.value().data()));
                    byte[] data = null;
                    try {
                        // Loading the resource data requires a different approach depending on
                        // whether the data is in a zip file or directly on the file system.
                        if (zipFile != null) {
                            data = zipFile.getInputStream(new ZipEntry(path.toString())).readAllBytes();
                        } else {
                            Path absolutePath = Paths.get(aabRoot.toString(), path.toString());
                            data = java.nio.file.Files.readAllBytes(absolutePath);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return new ArscResource(
                            entry.parent().getTypeName(),
                            entry.key(),
                            Files.getFileExtension(path.toString()),
                            path,
                            data
                    );
                })
                .toList();

        return new ArscTable(resourcesList);
    }

    public List<ArscResource> getAllResources() {
        return this.resourceTable;
    }
}
