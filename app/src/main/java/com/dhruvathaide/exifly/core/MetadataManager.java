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
        try {
            android.util.Log.d("Exifly", "Starting extraction for: " + uri);
            byte[] imageBytes = readBytes(context, uri);
            
            if (imageBytes == null || imageBytes.length == 0) {
                android.util.Log.e("Exifly", "readBytes returned empty/null");
                return new MetadataInfo(null, null, null);
            }
            android.util.Log.d("Exifly", "Read bytes: " + imageBytes.length);

            // Adding filename hint helps detecting format
            ImageMetadata metadata = Imaging.getMetadata(imageBytes);

            if (metadata == null) {
                android.util.Log.e("Exifly", "Imaging.getMetadata returned null");
                return new MetadataInfo(null, null, null);
            }

            android.util.Log.d("Exifly", "Metadata Class: " + metadata.getClass().getSimpleName());

            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                
                // 1. GPS - Manual Extraction to avoid library crash on String[] tags
                TiffImageMetadata exif = jpegMetadata.getExif();
                String gpsInfo = null;
                
                if (exif != null) {

                    try {
                        // 1. Try Standard Way
                        TiffImageMetadata.GPSInfo gps = exif.getGPS();
                        if (gps != null) {
                            gpsInfo = String.format("%.4f, %.4f", 
                                gps.getLatitudeAsDegreesNorth(), 
                                gps.getLongitudeAsDegreesEast());
                             android.util.Log.d("Exifly", "Standard GPS Extraction success: " + gpsInfo);
                        }
                    } catch (Exception e) {
                        android.util.Log.w("Exifly", "Standard getGPS failed: " + e.getMessage());
                        // 2. Deep Search Fallback
                        gpsInfo = findGpsManually(exif);
                    }
                }
                
                if (gpsInfo == null) {
                     // Debug: Dump relevant tags to find out why we missed it
                     try {
                         java.util.List<? extends org.apache.commons.imaging.formats.tiff.TiffField> fields = exif.getAllFields();
                         for (org.apache.commons.imaging.formats.tiff.TiffField field : fields) {
                             if (field.getTag() == 0x0002 || field.getTag() == 0x0004 || field.getTag() == 0x0001 || field.getTag() == 0x0003) { // Lat/Lon tags
                                 android.util.Log.d("Exifly", "Tag 0x" + Integer.toHexString(field.getTag()) + ": " + field.getValueDescription());
                             }
                         }
                     } catch (Exception ignored) {}
                }

                if (gpsInfo != null) android.util.Log.d("Exifly", "Found GPS: " + gpsInfo);

                // 2. Device Model
                String model = null;
                try {
                    if (exif != null) {
                       // Safe retrieval that handles both String and String[]
                       Object val = exif.getFieldValue(TiffTagConstants.TIFF_TAG_MODEL);
                       if (val instanceof String) model = (String) val;
                       else if (val instanceof String[]) model = ((String[]) val)[0];
                       
                       if (model != null) android.util.Log.d("Exifly", "Found Model: " + model);
                    }
                } catch (Exception ignored) {}

                // 3. Date Time
                String date = null;
                try {
                     if (exif != null) {
                        // Safe retrieval
                        Object val = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                        if (val instanceof String) date = (String) val;
                        else if (val instanceof String[]) date = ((String[]) val)[0];
                        
                        if (date != null) android.util.Log.d("Exifly", "Found Date: " + date);
                    }
                } catch (Exception ignored) {}

                return new MetadataInfo(gpsInfo, model, date);
            } else {
                android.util.Log.w("Exifly", "Not an instance of JpegImageMetadata");
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("Exifly", "Extraction error", e);
        }
        return new MetadataInfo(null, null, null);
    }
    private static byte[] readBytes(Context context, Uri uri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                android.util.Log.e("Exifly", "openInputStream returned null for " + uri);
                return null;
            }
            
            int bufferSize = 4096;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (IOException e) {
            android.util.Log.e("Exifly", "readBytes IOException", e);
            throw e;
        }
    }
    private static String getSafeString(TiffImageMetadata exif, org.apache.commons.imaging.formats.tiff.taginfos.TagInfo tag) {
        try {
            Object val = exif.getFieldValue(tag);
            if (val instanceof String) return (String) val;
            if (val instanceof String[]) {
                String[] arr = (String[]) val;
                return arr.length > 0 ? arr[0] : null;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static double convertToDegrees(org.apache.commons.imaging.common.RationalNumber[] values) {
        if (values == null || values.length != 3) return 0.0;
        
        // Log raw values for debugging
        // android.util.Log.d("Exifly", "Rational[0]: " + values[0] + ", [1]: " + values[1] + ", [2]: " + values[2]);

        double degrees = values[0].doubleValue();
        double minutes = values[1].doubleValue();
        double seconds = values[2].doubleValue();
        
        // Check for validity before math
        if (Double.isInfinite(degrees) || Double.isNaN(degrees)) degrees = 0.0;
        if (Double.isInfinite(minutes) || Double.isNaN(minutes)) minutes = 0.0;
        if (Double.isInfinite(seconds) || Double.isNaN(seconds)) seconds = 0.0;
        
        return degrees + (minutes / 60.0) + (seconds / 3600.0);
    }
    private static String findGpsManually(TiffImageMetadata exif) {
        try {
            // Brute force: Iterate ALL fields to find the GPS tags, regardless of directory structure
            java.util.List<? extends org.apache.commons.imaging.formats.tiff.TiffField> fields = exif.getAllFields();
            
            org.apache.commons.imaging.common.RationalNumber[] latValues = null;
            org.apache.commons.imaging.common.RationalNumber[] longValues = null;
            String latRef = null;
            String longRef = null;

            for (org.apache.commons.imaging.formats.tiff.TiffField field : fields) {
                if (field.getTag() == 0x0002) { // GPSLatitude
                   Object val = field.getValue();
                   if (val instanceof org.apache.commons.imaging.common.RationalNumber[]) 
                       latValues = (org.apache.commons.imaging.common.RationalNumber[]) val;
                } else if (field.getTag() == 0x0004) { // GPSLongitude
                   Object val = field.getValue();
                   if (val instanceof org.apache.commons.imaging.common.RationalNumber[]) 
                       longValues = (org.apache.commons.imaging.common.RationalNumber[]) val;
                } else if (field.getTag() == 0x0001) { // GPSLatitudeRef
                   Object val = field.getValue();
                   if (val instanceof String) latRef = (String) val;
                   else if (val instanceof String[]) latRef = ((String[]) val).length > 0 ? ((String[]) val)[0] : null;
                } else if (field.getTag() == 0x0003) { // GPSLongitudeRef
                   Object val = field.getValue();
                   if (val instanceof String) longRef = (String) val;
                   else if (val instanceof String[]) longRef = ((String[]) val).length > 0 ? ((String[]) val)[0] : null;
                }
            }

            if (latValues != null && longValues != null && latRef != null && longRef != null) {
                double lat = convertToDegrees(latValues);
                double lon = convertToDegrees(longValues);
                
                if (latRef.equalsIgnoreCase("S")) lat = -lat;
                if (longRef.equalsIgnoreCase("W")) lon = -lon;
                
                if (!Double.isNaN(lat) && !Double.isNaN(lon)) {
                    android.util.Log.d("Exifly", "Deep search found non-NaN GPS.");
                    return String.format("%.4f, %.4f", lat, lon);
                }
            }
        } catch (Exception e) {
            android.util.Log.w("Exifly", "Deep search failed", e);
        }
        return null;
    }
}
