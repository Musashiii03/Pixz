package com.example.pixz;

import java.io.File;
import java.util.Collection;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class MetadataInspector {
    public static void main(String[] args) {
        // Hardcoded path based on previous knowledge or just current dir
        // The user previously uploaded an image from a workspace.
        // I will assume the gallery loads from the user's workspace root or a known
        // test dir.
        // I'll scan the current directory and subdirectories for mp4 files.

        File root = new File("s:/Programming/Pixz");
        inspectDirectory(root);
    }

    private static void inspectDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                inspectDirectory(file);
            } else if (file.getName().toLowerCase().endsWith(".mp4") || file.getName().toLowerCase().endsWith(".mov")) {
                inspectFile(file);
            }
        }
    }

    private static void inspectFile(File file) {
        System.out.println("Inspecting: " + file.getAbsolutePath());
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    if (tag.getTagName().toLowerCase().contains("rotat") ||
                            tag.getTagName().toLowerCase().contains("orient")) {
                        System.out.println(
                                "  [" + directory.getName() + "] " + tag.getTagName() + " = " + tag.getDescription());
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("  Error: " + t.getMessage());
            t.printStackTrace();
        }
        System.out.println("--------------------------------------------------");
    }
}
