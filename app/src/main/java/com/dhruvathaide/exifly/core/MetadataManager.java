package com.dhruvathaide.exifly.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

import java.io.*;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

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
    public static MetadataInfo extractMetadata(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            ImageMetadata metadata = Imaging.getMetadata(is, "image.jpg");

            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                
                // 1. GPS
                TiffImageMetadata exif = jpegMetadata.getExif();
                String gpsInfo = null;
                
                if (exif != null && exif.getGPS() != null) {
                    try {
                        org.apache.commons.imaging.formats.tiff.TiffImageMetadata.GPSInfo gps = exif.getGPS();
                         // Format nicely: "34.05, -118.24"
                        gpsInfo = String.format("%.4f, %.4f", 
                            gps.getLatitudeAsDegreesNorth(), 
                            gps.getLongitudeAsDegreesEast());
                    } catch (Exception e) {}
                }

                // 2. Device Model
                String model = null;
                try {
                    if (exif != null && exif.findField(TiffTagConstants.TIFF_TAG_MODEL) != null) {
                       Object val = exif.getFieldValue(TiffTagConstants.TIFF_TAG_MODEL);
                       if (val instanceof String) model = (String) val;
                       else if (val instanceof String[]) model = ((String[]) val)[0];
                    }
                } catch (Exception ignored) {}

                // 3. Date Time
                String date = null;
                try {
                     if (exif != null && exif.findField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL) != null) {
                        Object val = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                        if (val instanceof String) date = (String) val;
                        else if (val instanceof String[]) date = ((String[]) val)[0];
                    }
                } catch (Exception ignored) {}

                return new MetadataInfo(gpsInfo, model, date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new MetadataInfo(null, null, null);
    }
}
