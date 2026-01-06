package com.dhruvathaide.exifly.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

import java.io.*;

public class MetadataManager {

    public static Uri stripExif(
            Context context,
            Uri sourceUri,
            String outputName
    ) throws Exception {

        ContentResolver resolver = context.getContentResolver();
        
        // 1. Prepare Output Destination
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, outputName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/CleanExif"
        );

        Uri outputUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );

        if (outputUri == null) {
            throw new IOException("Failed to create MediaStore entry");
        }

        try (InputStream input = resolver.openInputStream(sourceUri);
             OutputStream out = resolver.openOutputStream(outputUri)) {

            // 2. Stream directly from Input to Output via ExifRewriter
            // This avoids loading the whole image into RAM
            new ExifRewriter().removeExifMetadata(input, out);
            
            return outputUri;

        } catch (Exception e) {
            // Cleanup on failure to avoid 0-byte ghost files
            resolver.delete(outputUri, null, null);
            throw e;
        }
    }
}
