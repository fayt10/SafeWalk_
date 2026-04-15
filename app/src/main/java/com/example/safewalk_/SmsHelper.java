package com.example.safewalk_;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.telephony.SmsManager;
import android.util.Log;

public class SmsHelper {

    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    public static void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.d("SmsHelper", "SMS sent to " + phoneNumber);
        } catch (Exception e) {
            Log.e("SmsHelper", "Failed to send SMS: " + e.getMessage());
        }
    }

    public static void sendPanicSms(Context context, String guardianPhone,
                                    String userName, double lat, double lng) {

        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lng;
        String msg = "[SafeWalk EMERGENCY] " + userName +
                " has triggered the panic button! Their location: " + mapsLink;
        sendSms(guardianPhone, msg);
    }

    public static void sendJourneyStartSms(Context context, String guardianPhone,
                                           String userName, String destination) {
        if (isInternetAvailable(context)) return;

        String msg = "[SafeWalk] " + userName +
                " has started walking to " + destination +
                ". They will share their location when internet is available.";
        sendSms(guardianPhone, msg);
    }

    public static void sendSafeArrivalSms(Context context, String guardianPhone,
                                          String userName, String destination) {

        String msg = "[SafeWalk] " + userName +
                " has arrived safely at " + destination + ". All is well!";
        sendSms(guardianPhone, msg);
    }

    public static void sendLocationUpdateSms(Context context, String guardianPhone,
                                             String userName, double lat, double lng) {
        if (isInternetAvailable(context)) return;

        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lng;
        String msg = "[SafeWalk] Location update for " + userName + ": " + mapsLink;
        sendSms(guardianPhone, msg);
    }

    public static void sendOverdueSms(Context context, String guardianPhone,
                                      String userName, String destination,
                                      double lat, double lng) {
        String mapsLink = "https://maps.google.com/?q=" + lat + "," + lng;
        String msg = "[SafeWalk] " + userName + " has not arrived at "
                + destination + " yet. Last known location: " + mapsLink;
        sendSms(guardianPhone, msg); // No internet check — always send
    }
}