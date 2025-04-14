package com.kritavya.photogallery;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageItem {
    private File file;
    private Uri fileUri;
    private String name;
    private String path;
    private long size;
    private long dateModified;
    private boolean isExternalUri;
    private Context context;

    // Constructor for File-based items (from app directory)
    public ImageItem(File file) {
        this.file = file;
        this.name = file.getName();
        this.path = file.getAbsolutePath();
        this.size = file.length();
        this.dateModified = file.lastModified();
        this.isExternalUri = false;
    }

    // Constructor for DocumentFile-based items (from external directories)
    public ImageItem(Uri fileUri, String fileName, long fileSize, long lastModified, Context context) {
        this.fileUri = fileUri;
        this.name = fileName;
        this.path = fileUri.toString();
        this.size = fileSize;
        this.dateModified = lastModified;
        this.isExternalUri = true;
        this.context = context;
    }

    public File getFile() {
        return file;
    }
    
    public Uri getUri() {
        if (isExternalUri) {
            return fileUri;
        } else {
            return Uri.fromFile(file);
        }
    }
    
    public boolean isExternalUri() {
        return isExternalUri;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }
    
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", size / 1024.0);
        } else {
            return String.format(Locale.getDefault(), "%.2f MB", size / (1024.0 * 1024));
        }
    }

    public long getDateModified() {
        return dateModified;
    }
    
    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(dateModified));
    }
    
    public boolean delete() {
        if (isExternalUri) {
            try {
                return DocumentsContract.deleteDocument(context.getContentResolver(), fileUri);
            } catch (Exception e) {
                return false;
            }
        } else {
            return file != null && file.delete();
        }
    }
} 