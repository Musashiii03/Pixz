package com.example.pixz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Manages session persistence - saves and restores folder selections
 */
public class SessionManager {
    private static final String SESSION_FILE_NAME = "wingallery-session.txt";
    private static final String APP_DIR_NAME = ".wingallery";

    /**
     * Get the session file path in user's home directory
     */
    private static Path getSessionFilePath() {
        String userHome = System.getProperty("user.home");
        Path appDir = Paths.get(userHome, APP_DIR_NAME);

        // Create app directory if it doesn't exist
        try {
            if (!Files.exists(appDir)) {
                Files.createDirectories(appDir);
            }
        } catch (IOException e) {
            // Failed to create directory
        }

        return appDir.resolve(SESSION_FILE_NAME);
    }

    /**
     * Save folder paths to session file
     */
    public static void saveSession(Set<String> folderPaths) {
        Path sessionFile = getSessionFilePath();

        try (BufferedWriter writer = Files.newBufferedWriter(sessionFile)) {
            for (String folderPath : folderPaths) {
                // Only save folders that still exist
                if (new File(folderPath).exists()) {
                    writer.write(folderPath);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load folder paths from session file
     */
    public static Set<String> loadSession() {
        Set<String> folderPaths = new LinkedHashSet<>();
        Path sessionFile = getSessionFilePath();

        if (!Files.exists(sessionFile)) {
            return folderPaths;
        }

        try (BufferedReader reader = Files.newBufferedReader(sessionFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    File folder = new File(line);
                    // Only restore folders that still exist
                    if (folder.exists() && folder.isDirectory()) {
                        folderPaths.add(line);
                    }
                }
            }
        } catch (IOException e) {
            // Failed to load session
        }

        return folderPaths;
    }

    /**
     * Clear the session file
     */
    public static void clearSession() {
        Path sessionFile = getSessionFilePath();
        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            // Failed to clear session
        }
    }
}
