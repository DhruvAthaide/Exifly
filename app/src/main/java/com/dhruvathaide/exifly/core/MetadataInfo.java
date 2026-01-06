package com.dhruvathaide.exifly.core;

public class MetadataInfo {
    public final String gpsCoordinates; // e.g., "37.7749, -122.4194"
    public final String deviceModel;
    public final String dateTime;
    public final String rawTags;

    public MetadataInfo(String gpsCoordinates, String deviceModel, String dateTime, String rawTags) {
        this.gpsCoordinates = gpsCoordinates;
        this.deviceModel = deviceModel;
        this.dateTime = dateTime;
        this.rawTags = rawTags;
    }

    public boolean hasRisk() {
        return (gpsCoordinates != null) || (deviceModel != null && !deviceModel.isEmpty()) || (dateTime != null && !dateTime.isEmpty());
    }
}
