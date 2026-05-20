// UnmappedRegionLogger.java
package org.cloudcompare.backend.util;

import org.cloudcompare.backend.catalog.Provider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;

public final class UnmappedLogger {

    private static final Path LOG_PATH =
            Paths.get("/app/logs/unmapped-regions.txt");
    private UnmappedLogger() {}

    public static void log(Provider provider, String regionName) {
        if (provider == null || regionName == null || regionName.isBlank()) return;

        String line = provider.name() + ": " + regionName.trim() + " @ " + Instant.now() + System.lineSeparator();

        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.write(LOG_PATH, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            Logger.LogError("Failed to write unmapped region to log: " + e.getMessage());
        }
    }
}
