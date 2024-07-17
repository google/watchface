package com.google.wear.watchface.dfx.memory;

import com.google.common.io.Files;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceFile;
import com.google.devrel.gmscore.tools.apk.arsc.BinaryResourceValue;
import com.google.devrel.gmscore.tools.apk.arsc.Chunk;
import com.google.devrel.gmscore.tools.apk.arsc.ResourceTableChunk;
import com.google.devrel.gmscore.tools.apk.arsc.StringPoolChunk;
import com.google.devrel.gmscore.tools.apk.arsc.TypeChunk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Represents all the resources of interest in the Android package.
 * <p>
 * Where obfuscation has been applied, the mapping is derived, for the creation of the
 * AndroidResource objects. For example, for a logical resource res/raw/watchface.xml, an APK
 * may in fact store this as res/aB.xml. The resources.arsc file contains this mapping, and the
 * AndroidResourceTable provides a list of resources with their logical types, names and data.
 * <p>
 * Note that more than one AndroidResource object can exist for the given dimensions. For example,
 * if there is a drawable and a drawable-fr folder, then there may be multiple AndroidResource
 * entries for drawables with the same type, name and extension. The configuration detail, e.g.
 * "fr" or "default", is not currently exposed in the AndroidResource objects as it isn't used.
 */
public class AndroidResourceTable {
    // Only certain resource types are of interest to the evaluator, notably, not string resources.
    private static final Set<String> RESOURCE_TYPES = Set.of("raw", "xml", "drawable", "font", "asset");
    private static final String RESOURCES_FILE_NAME = "resources.arsc";
    private final List<AndroidResource> resourceTable;

    private AndroidResourceTable(List<AndroidResource> resources) {
        this.resourceTable = resources;
    }

    /**
     * Creates the table from a path to an AAB structure on the file system.
     *
     * @param aabPath The path to the root of the AAB directory.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static AndroidResourceTable createFromAabDirectory(Path aabPath) throws IOException {
        Stream<Path> childrenFilesStream = java.nio.file.Files.walk(aabPath);
        ArrayList<AndroidResource> resources = new ArrayList<>();
        int relativePathOffset = aabPath.toString().length() + 1;

        childrenFilesStream
            .forEach(
                filePath -> {
                    if (AndroidResource.isValidResourcePath(filePath)) {
                        try {
                            resources.add(AndroidResource.fromPath(
                                Paths.get(filePath.toString().substring(relativePathOffset)),
                                java.nio.file.Files.readAllBytes(filePath)
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (filePath.endsWith("manifest/AndroidManifest.xml")) {
                        try {
                            resources.add(new AndroidResource(
                                "xml",
                                "AndroidManifest",
                                "xml",
                                Paths.get(filePath.toString().substring(relativePathOffset)),
                                java.nio.file.Files.readAllBytes(filePath)
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            );
        return new AndroidResourceTable(resources);
    }

    /**
     * Creates the table from an APK file.
     *
     * @param zipFile The zip file object representing the APK.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static AndroidResourceTable createFromApkFile(ZipFile zipFile) throws IOException {
        ZipEntry arscEntry = new ZipEntry(RESOURCES_FILE_NAME);

        AndroidResourceTable table;
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
     * Creates the table from an APK file.
     *
     * @param aabZipFile The zip file object representing the APK.
     * @return The constructed table.
     */
    static AndroidResourceTable createFromAabFile(ZipFile aabZipFile) {
        ArrayList<AndroidResource> resources = new ArrayList<>();
        aabZipFile.stream()
            .forEach(
                zipEntry -> {
                    Path zipEntryPath = Paths.get(zipEntry.getName());
                    if (AndroidResource.isValidResourcePath(zipEntryPath)) {
                        try {
                            resources.add(AndroidResource.fromPath(
                                zipEntryPath,
                                aabZipFile.getInputStream(zipEntry).readAllBytes()
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (zipEntry.getName().endsWith("manifest/AndroidManifest.xml")) {
                        try {
                            resources.add(new AndroidResource(
                                "xml",
                                "AndroidManifest",
                                "xml",
                                Paths.get(zipEntry.getName()),
                                aabZipFile.getInputStream(zipEntry).readAllBytes()
                            ));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            );
        return new AndroidResourceTable(resources);
    }

    /**
     * Creates the table from a base split entry within an archive
     *
     * @param baseSplitZipStream The zip entry for the base split.
     * @return The constructed table.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static AndroidResourceTable createFromMokkaZip(ZipInputStream baseSplitZipStream) throws IOException {
        List<AndroidResource> resources = new ArrayList<>();

        ZipEntry zipEntry;
        while ((zipEntry = baseSplitZipStream.getNextEntry()) != null) {
            String name = zipEntry.getName();
            byte[] fileData = readAllBytes(baseSplitZipStream);
            if (AndroidResource.isValidResourcePath(name)) {
                resources.add(AndroidResource.fromPath(name, fileData));
            }
        }
        return new AndroidResourceTable(resources);
    }

    /** Read all bytes from an input stream to a new byte array. */
    static byte[] readAllBytes(InputStream inputStream) throws IOException {
        int len;
        byte[] buffer = new byte[1024];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
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
    private static AndroidResourceTable createTable(InputStream is, Function<Path, byte[]> fileDataProducer) throws IOException {
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

        List<AndroidResource> resourcesList = typeChunks.stream()
                .flatMap(c -> c.getEntries().values().stream())
                .filter(t -> RESOURCE_TYPES.contains(t.typeName()))
                .filter(t -> t.value().type() == BinaryResourceValue.Type.STRING)
                .map(entry -> {
                    Path path = Path.of(stringPool.getString(entry.value().data()));
                    byte[] data = fileDataProducer.apply(path);
                    return new AndroidResource(
                            entry.parent().getTypeName(),
                            entry.key(),
                            Files.getFileExtension(path.toString()),
                            path,
                            data
                    );
                })
                .toList();

        return new AndroidResourceTable(resourcesList);
    }

    public List<AndroidResource> getAllResources() {
        return this.resourceTable;
    }
}
