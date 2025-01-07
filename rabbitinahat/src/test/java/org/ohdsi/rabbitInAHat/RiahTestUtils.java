package org.ohdsi.rabbitInAHat;

import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class RiahTestUtils {
    public static void unzip(Path zipFileName, Path destination) throws IOException {

        if (!Files.exists(zipFileName)){
            throw new IOException("File not found: " + zipFileName);
        }
        try (ZipFile zipFile = new ZipFile(zipFileName.toFile(), ZipFile.OPEN_READ)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destination.resolve(entry.getName());
                if (entryPath.normalize().startsWith(destination.normalize())){
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (InputStream in = zipFile.getInputStream(entry)){
                            try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                                IOUtils.copy(in, out);
                            }
                        }
                    }
                }
            }
        }
    }

}
