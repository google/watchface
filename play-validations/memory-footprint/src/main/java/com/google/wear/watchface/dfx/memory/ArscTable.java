package com.google.wear.watchface.dfx.memory;

import com.google.common.io.Files;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
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
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents all the resources of interest in the Android package.
 * <p>
 * These resources are loaded from the resources.arsc file. Where obfuscation has been applied, the
 * mapping is derived, for the creation of the ArscResource objects. For example, for a logical
 * resource res/raw/watchface.xml, the package may in fact store this as res/aB.xml. The
 * resources.arsc file contains this mapping, and the ArscTable provides a list of resources with
 * their logical types, names and data.
 * <p>
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
     *
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
        ArscTable table;
        try (InputStream is = new FileInputStream(arscFile)) {
            Function<Path, byte[]> fn = (Path path) -> {
                try {
                    Path absolutePath = Paths.get(aabPath.toString(), path.toString());
                    return java.nio.file.Files.readAllBytes(absolutePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            table = createTable(is, fn);
        }
        return table;
    }

    /**
     * Creates the table from a AAB or APK file.
     *
     * @param zipFile The zip file object representing the AAB/APK.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static ArscTable createFromAndroidPackage(ZipFile zipFile) throws IOException {
        ZipEntry arscEntry = new ZipEntry(RESOURCES_FILE_NAME);

        ArscTable table;
        try (InputStream is = zipFile.getInputStream(arscEntry)) {
            Function<Path, byte[]> fn = (Path path) -> {
                try {
                    return zipFile.getInputStream(new ZipEntry(path.toString())).readAllBytes();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            table = createTable(is, fn);
        }
        return table;
    }

    /**
     * Creates the table from an InputStream.
     *
     * @param is               The InputStream
     * @param fileDataProducer A lambda that returns the raw bytes of a file, given a path, which
     *                         may represent a zip file, or a directory (for example), that is the
     *                         source of those bytes.
     * @return The constructed table.
     * @throws IOException when errors loading resources occur.
     */
    private static ArscTable createTable(InputStream is, Function<Path, byte[]> fileDataProducer) throws IOException {
        BinaryResourceFile resources = BinaryResourceFile.fromInputStream(is);
        List<Chunk> chunks = resources.getChunks();
        if (chunks.isEmpty()) {
            throw new IOException("no chunks");
        }
        if (!(chunks.get(0) instanceof ResourceTableChunk)) {
            throw new IOException("no res table chunk");
        }
        ResourceTableChunk table = (ResourceTableChunk) chunks.get(0);
        StringPoolChunk stringPool = table.getStringPool();

        List<TypeChunk> typeChunks = table.getPackages()
                .stream()
                .flatMap(p -> p.getTypeChunks().stream())
                .toList();

        List<ArscResource> resourcesList = typeChunks.stream()
                .flatMap(c -> c.getEntries().values().stream())
                .filter(t -> RESOURCE_TYPES.contains(t.typeName()))
                .filter(t -> t.value().type() == BinaryResourceValue.Type.STRING)
                .map(entry -> {
                    Path path = Path.of(stringPool.getString(entry.value().data()));
                    byte[] data = fileDataProducer.apply(path);
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
