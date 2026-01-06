package com.dhruvathaide.exifly;

import android.net.Uri;

public class ImageModel {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CLEANED = 1;
    public static final int STATUS_FAILED = 2;

    private final Uri uri;
    private int status;
    private com.dhruvathaide.exifly.core.MetadataInfo metadata;

    public ImageModel(Uri uri) {
        this.uri = uri;
        this.status = STATUS_PENDING;
    }

    public com.dhruvathaide.exifly.core.MetadataInfo getMetadata() {
        return metadata;
    }

    public void setMetadata(com.dhruvathaide.exifly.core.MetadataInfo metadata) {
        this.metadata = metadata;
    }

    public Uri getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }

    private Uri cleanedUri;

    public Uri getCleanedUri() {
        return cleanedUri;
    }

    public void setCleanedUri(Uri cleanedUri) {
        this.cleanedUri = cleanedUri;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
