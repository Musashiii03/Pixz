package com.example.pixz;

import java.io.File;
import java.lang.ref.WeakReference;

import javafx.scene.image.Image;

/**
 * Model class representing a media file (image or video)
 * Uses WeakReference for thumbnail to prevent memory leaks
 */
public class MediaItem {
    private final File file;
    private WeakReference<Image> thumbnailRef;
    private final MediaType type;
    private int width;
    private int height;

    public enum MediaType {
        IMAGE, VIDEO
    }

    public MediaItem(File file, MediaType type) {
        this.file = file;
        this.type = type;
    }

    public File getFile() {
        return file;
    }

    /**
     * Get thumbnail image
     * Returns null if image was reclaimed by GC
     * Caller should reload from cache if needed
     */
    public Image getThumbnail() {
        return thumbnailRef != null ? thumbnailRef.get() : null;
    }

    /**
     * Set thumbnail image using WeakReference
     * Allows GC to reclaim memory when needed
     */
    public void setThumbnail(Image thumbnail) {
        this.thumbnailRef = thumbnail != null ? new WeakReference<>(thumbnail) : null;
    }

    public MediaType getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getName() {
        return file.getName();
    }

    public String getPath() {
        return file.getAbsolutePath();
    }
}
