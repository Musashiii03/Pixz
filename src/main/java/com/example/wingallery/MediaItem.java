package com.example.wingallery;

import javafx.scene.image.Image;
import java.io.File;

/**
 * Model class representing a media file (image or video)
 */
public class MediaItem {
    private final File file;
    private Image thumbnail;
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

    public Image getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Image thumbnail) {
        this.thumbnail = thumbnail;
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
