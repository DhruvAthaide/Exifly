package com.dhruvathaide.exifly;

import android.net.Uri;

public class ImageModel {
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CLEANED = 1;
    public static final int STATUS_FAILED = 2;

    private final Uri uri;
    private int status;

    public ImageModel(Uri uri) {
        this.uri = uri;
        this.status = STATUS_PENDING;
    }

    public Uri getUri() {
        return uri;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
