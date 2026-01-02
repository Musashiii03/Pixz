package com.example.pixz;

import java.io.File;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * Utility class for reading media metadata, specifically orientation/rotation
 */
public class MediaMetadataUtils {

    /**
     * Get the rotation in degrees for a media file (Image or Video)
     * Returns 0 if no rotation metadata is found
     */
    public static int getRotation(File file) {
        if (ThumbnailGenerator.isImageFile(file)) {
            return getImageOrientation(file);
        } else if (ThumbnailGenerator.isVideoFile(file)) {
            return getVideoRotation(file);
        }
        return 0;
    }

    private static int getImageOrientation(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);

            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

                // Convert EXIF orientation to degrees
                switch (orientation) {
                    case 6:
                        return 90; // Rotate 90 CW (Right)
                    case 3:
                        return 180; // Rotate 180
                    case 8:
                        return 270; // Rotate 270 CW (Left)
                    default:
                        return 0; // Normal or unknown
                }
            }
        } catch (Throwable t) {
            System.err.println("Error reading image orientation for " + file.getName() + ": " + t.getMessage());
            // t.printStackTrace(); // Optional: enable for deep debugging
        }
        return 0;
    }

    private static int getVideoRotation(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);

            Integer priorityRotation = null;
            Integer fallbackRotation = null;

            // Search for Rotation tag in all directories
            for (com.drew.metadata.Directory dir : metadata.getDirectories()) {
                String dirName = dir.getName();

                // Ignore thumbnail directories for rotation
                if (dirName.contains("Thumbnail"))
                    continue;

                for (com.drew.metadata.Tag tag : dir.getTags()) {
                    String tagName = tag.getTagName().toLowerCase();

                    if (tagName.contains("rotation")) {
                        int rotation = 0;
                        boolean parsed = false;

                        try {
                            rotation = dir.getInt(tag.getTagType());
                            parsed = true;
                        } catch (Exception e) {
                            String desc = tag.getDescription().replaceAll("[^0-9-]", "");
                            try {
                                if (!desc.isEmpty()) {
                                    rotation = Integer.parseInt(desc);
                                    parsed = true;
                                }
                            } catch (NumberFormatException nfe) {
                            }
                        }

                        if (parsed) {
                            logDebug("DEBUG: Found rotation " + rotation + " in " + dirName + " (" + tagName + ")");

                            // Normalize rotation to 0-359 range first
                            rotation = ((rotation % 360) + 360) % 360;

                            if (dirName.contains("MP4") || dirName.contains("QuickTime")
                                    || dirName.contains("Header")) {
                                // Prefer non-zero rotation if we find conflicts within priority directories
                                if (priorityRotation == null || (priorityRotation == 0 && rotation != 0)) {
                                    priorityRotation = rotation;
                                }
                            } else {
                                if (fallbackRotation == null || (fallbackRotation == 0 && rotation != 0)) {
                                    fallbackRotation = rotation;
                                }
                            }
                        }
                    } else if (tagName.contains("orientation")) {
                        // Handle orientation logic...
                        try {
                            int val = dir.getInt(tag.getTagType());
                            int degrees = 0;
                            switch (val) {
                                case 6:
                                    degrees = 90;
                                    break;
                                case 3:
                                    degrees = 180;
                                    break;
                                case 8:
                                    degrees = 270;
                                    break;
                            }
                            if (degrees != 0) {
                                logDebug("DEBUG: Found orientation " + val + " (" + degrees + "deg) in " + dirName);
                                if (fallbackRotation == null || (fallbackRotation == 0 && degrees != 0)) {
                                    fallbackRotation = degrees;
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }

            // Normalize rotation values
            // Video rotation metadata typically indicates how much to rotate clockwise to display correctly
            Integer finalRotation = null;
            
            if (priorityRotation != null && priorityRotation != 0) {
                finalRotation = priorityRotation;
                logDebug(file.getName() + " -> Selected Priority Rotation: " + priorityRotation);
            } else if (fallbackRotation != null && fallbackRotation != 0) {
                finalRotation = fallbackRotation;
                logDebug(file.getName() + " -> Selected Fallback Rotation: " + fallbackRotation
                        + " (Overrides 0 Priority)");
            } else if (priorityRotation != null) {
                finalRotation = priorityRotation;
                logDebug(file.getName() + " -> Selected Priority Rotation: " + priorityRotation);
            }
            
            if (finalRotation != null) {
                // Normalize to 0-359 range
                finalRotation = ((finalRotation % 360) + 360) % 360;
                
                // CRITICAL FIX: Video metadata rotation needs to be INVERTED
                // Metadata says "the video was recorded rotated X degrees" 
                // So we need to rotate it BACK by -X degrees (or 360-X) to display correctly
                // Example: Video recorded at 180° (upside down) -> rotate by -180° (or 180°) to fix
                // Example: Video recorded at 90° CW -> rotate by 270° CW (or -90°) to fix
                
                if (finalRotation != 0) {
                    int correctedRotation = (360 - finalRotation) % 360;
                    logDebug(file.getName() + " -> Original rotation: " + finalRotation + 
                            ", Corrected (inverted): " + correctedRotation);
                    return correctedRotation;
                }
                
                logDebug(file.getName() + " -> Final rotation: " + finalRotation);
                return finalRotation;
            }

            logDebug(file.getName() + " -> No 'Rotation' tag found.");
            return 0;

        } catch (Throwable t) {
            logDebug(file.getName() + " -> Error reading rotation: " + t.getMessage());
        }
        return 0;
    }

    public static void logDebug(String message) {
        try (java.io.PrintWriter out = new java.io.PrintWriter(
                new java.io.FileWriter("s:\\Programming\\Pixz\\debug.log", true))) {
            out.println(new java.util.Date() + ": " + message);
        } catch (Exception e) {
            // Ignore logging errors
        }
    }
}
