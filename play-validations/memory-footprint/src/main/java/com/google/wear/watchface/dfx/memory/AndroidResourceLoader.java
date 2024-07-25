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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Represents all the resources of interest in the Android package.
 *
 * <p>Where obfuscation has been applied, the mapping is derived, for the creation of the
 * AndroidResource objects. For example, for a logical resource res/raw/watchface.xml, an APK may in
 * fact store this as res/aB.xml. The resources.arsc file contains this mapping, and the
 * AndroidResourceLoader provides a stream of resources with their logical types, names and data.
 *
 * <p>Note that more than one AndroidResource object can exist for the given dimensions. For
 * example, if there is a drawable and a drawable-fr folder, then there may be multiple
 * AndroidResource entries for drawables with the same type, name and extension. The configuration
 * detail, e.g. "fr" or "default", is not currently exposed in the AndroidResource objects as it
 * isn't used.
 */
public class AndroidResourceLoader {
    // Only certain resource types are of interest to the evaluator, notably, not string resources.
    private static final Set<String> RESOURCE_TYPES =
            Set.of("raw", "xml", "drawable", "font", "asset");
    private static final String RESOURCES_FILE_NAME = "resources.arsc";

    private AndroidResourceLoader() {}

    /**
     * Creates a resource stream from a path to an AAB structure on the file system.
     *
     * @param aabPath The path to the root of the AAB directory.
     * @return A stream of Android resource objects.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static Stream<AndroidResource> streamFromAabDirectory(Path aabPath) throws IOException {
        Stream<Path> childrenFilesStream = java.nio.file.Files.walk(aabPath);
        int relativePathOffset = aabPath.toString().length() + 1;

        return childrenFilesStream
                .map(
                        filePath -> {
                            AndroidResource resource = null;
                            if (AndroidResource.isValidResourcePath(filePath)) {
                                try {
                                    resource =
                                            AndroidResource.fromPath(
                                                    Paths.get(
                                                            filePath.toString()
                                                                    .substring(relativePathOffset)),
                                                    java.nio.file.Files.readAllBytes(filePath));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (filePath.endsWith("manifest/AndroidManifest.xml")) {
                                try {
                                    resource =
                                            new AndroidResource(
                                                    "xml",
                                                    "AndroidManifest",
                                                    "xml",
                                                    Paths.get(
                                                            filePath.toString()
                                                                    .substring(relativePathOffset)),
                                                    java.nio.file.Files.readAllBytes(filePath));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return resource;
                        })
                .filter(Objects::nonNull);
    }

    /**
     * Creates a stream of resource objects from the AAB file.
     *
     * @param aabZipFile The zip file object representing the AAB.
     * @return A stream of resource objects.
     */
    static Stream<AndroidResource> streamFromAabFile(ZipFile aabZipFile) {
        return aabZipFile.stream()
                .map(
                        zipEntry -> {
                            Path zipEntryPath = Paths.get(zipEntry.getName());
                            AndroidResource resource = null;
                            if (AndroidResource.isValidResourcePath(zipEntryPath)) {
                                try {
                                    resource =
                                            AndroidResource.fromPath(
                                                    zipEntryPath,
                                                    aabZipFile
                                                            .getInputStream(zipEntry)
                                                            .readAllBytes());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (zipEntry.getName()
                                    .endsWith("manifest/AndroidManifest.xml")) {
                                try {
                                    resource =
                                            new AndroidResource(
                                                    "xml",
                                                    "AndroidManifest",
                                                    "xml",
                                                    Paths.get(zipEntry.getName()),
                                                    aabZipFile
                                                            .getInputStream(zipEntry)
                                                            .readAllBytes());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            return resource;
                        })
                .filter(Objects::nonNull);
    }

    /**
     * Creates a resource stream from a base split entry within an archive
     *
     * @param baseSplitZipStream The zip entry for the base split.
     * @return A stream of resource objects.
     * @throws IOException when the resources file cannot be found, or other IO errors occur.
     */
    static Stream<AndroidResource> streamFromMokkaZip(ZipInputStream baseSplitZipStream)
            throws IOException {

        Iterator<AndroidResource> iterator =
                new Iterator<AndroidResource>() {
                    private ZipEntry zipEntry;

                    @Override
                    public boolean hasNext() {
                        try {
                            zipEntry = baseSplitZipStream.getNextEntry();
                            // Advance over entries in the zip that aren't relevant.
                            while (zipEntry != null
                                    && !AndroidResource.isValidResourcePath(zipEntry.getName())) {
                                zipEntry = baseSplitZipStream.getNextEntry();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return zipEntry != null;
                    }

                    @Override
                    public AndroidResource next() {
                        System.out.println(zipEntry);
                        byte[] entryData;
                        try {
                            entryData = readAllBytes(baseSplitZipStream);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return AndroidResource.fromPath(zipEntry.getName(), entryData);
                    }
                };

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Creates a resource stream from an APK zip file.
     *
     * <p>APK files can have their resources obfuscated, so it is necessary to extract the mapping
     * between the original path and the path in the obfuscated zip.
     *
     * @param zipFile The APK zip file
     * @return A stream of resource objects.
     * @throws IOException when errors loading resources occur.
     */
    static Stream<AndroidResource> streamFromApkFile(ZipFile zipFile) throws IOException {
        ZipEntry arscEntry = new ZipEntry(RESOURCES_FILE_NAME);
        InputStream is = zipFile.getInputStream(arscEntry);

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

        List<TypeChunk> typeChunks =
                table.getPackages().stream().flatMap(p -> p.getTypeChunks().stream()).toList();

        return typeChunks.stream()
                .flatMap(c -> c.getEntries().values().stream())
                .filter(t -> RESOURCE_TYPES.contains(t.typeName()))
                .filter(t -> t.value().type() == BinaryResourceValue.Type.STRING)
                .map(
                        entry -> {
                            Path path = Path.of(stringPool.getString(entry.value().data()));
                            byte[] data = null;
                            try {
                                data =
                                        zipFile.getInputStream(new ZipEntry(path.toString()))
                                                .readAllBytes();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            return new AndroidResource(
                                    entry.parent().getTypeName(),
                                    entry.key(),
                                    Files.getFileExtension(path.toString()),
                                    path,
                                    data);
                        });
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
}
