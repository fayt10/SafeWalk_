package com.example.safewalk_;

public class JourneyData {
    public String journeyId;
    public String userId;
    public String destination;
    public double startLat, startLng;
    public double endLat, endLng;
    public long startTime;
    public long endTime;
    public float distanceMeters;
    public String status; // "active", "completed", "panic"
    public int alertsTriggered;
    public String checkinInterval;
    public String arrivalTime;

    public JourneyData() {}

    public JourneyData(String userId, String destination, String checkinInterval) {
        this.userId = userId;
        this.destination = destination;
        this.checkinInterval = checkinInterval;
        this.startTime = System.currentTimeMillis();
        this.status = "active";
        this.distanceMeters = 0;
        this.alertsTriggered = 0;
    }

    // Duration in minutes
    public long getDurationMinutes() {
        long end = (endTime > 0) ? endTime : System.currentTimeMillis();
        return (end - startTime) / 60000;
    }

    // Distance in km
    public float getDistanceKm() {
        return distanceMeters / 1000f;
    }

    // Time of day category
    public String getTimeCategory() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 21) return "evening";
        return "night";
    }
}