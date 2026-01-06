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

        InputStream input = resolver.openInputStream(sourceUri);
        ByteArrayOutputStream cleaned = new ByteArrayOutputStream();

        new ExifRewriter().removeExifMetadata(input, cleaned);
        input.close();

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

        OutputStream out = resolver.openOutputStream(outputUri);
        out.write(cleaned.toByteArray());
        out.flush();
        out.close();

        return outputUri;
    }
}
