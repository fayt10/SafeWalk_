package com.example.safewalk_;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveWalkActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_DESTINATION      = "extra_destination";
    public static final String EXTRA_ARRIVAL_HOUR     = "extra_arrival_hour";
    public static final String EXTRA_ARRIVAL_MINUTE   = "extra_arrival_minute";
    public static final String EXTRA_CHECKIN_INTERVAL = "extra_checkin_interval";
    public static final String EXTRA_JOURNEY_ID       = "extra_journey_id";

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvDestination, tvTimer, tvDistance, tvETA;
    private Button btnPanic, btnEndWalk;

    private String journeyId = "";
    private String destination = "";
    private String userName = "";
    private String checkinInterval = "15 minutes";

    // Tracking
    private final List<LatLng> breadcrumbs = new ArrayList<>();
    private Location lastLocation = null;
    private float totalDistanceMeters = 0f;
    private long startTime = 0;
    private int elapsedSeconds = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    // Guardians phone list for SMS fallback
    private final List<String> guardianPhones = new ArrayList<>();

    // Shake detector
    private android.hardware.SensorManager sensorManager;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_walk);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        destination     = getIntent().getStringExtra(EXTRA_DESTINATION);
        checkinInterval = getIntent().getStringExtra(EXTRA_CHECKIN_INTERVAL);
        journeyId       = getIntent().getStringExtra(EXTRA_JOURNEY_ID);
        startTime       = System.currentTimeMillis();

        tvDestination = findViewById(R.id.tvDestination);
        tvTimer       = findViewById(R.id.tvTimer);
        tvDistance    = findViewById(R.id.tvDistance);
        tvETA         = findViewById(R.id.tvETA);
        btnPanic      = findViewById(R.id.btnPanic);
        btnEndWalk    = findViewById(R.id.btnEndWalk);

        if (destination != null) tvDestination.setText("To: " + destination);

        // Map
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Load user info and guardian phones
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid != null) {
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        userName = doc.getString("name") != null ? doc.getString("name") : "User";
                    });

            db.collection("users").document(uid).collection("guardians").get()
                    .addOnSuccessListener(snap -> {
                        for (var doc : snap.getDocuments()) {
                            String phone = doc.getString("phone");
                            if (phone != null && !phone.isEmpty()) {
                                guardianPhones.add(phone);
                            }
                        }
                        // Notify guardians journey started (SMS if no internet)
                        for (String phone : guardianPhones) {
                            SmsHelper.sendJourneyStartSms(this, phone, userName, destination);
                        }
                    });
        }

        // Timer
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                elapsedSeconds++;
                int min = elapsedSeconds / 60;
                int sec = elapsedSeconds % 60;
                tvTimer.setText(String.format("%02d:%02d", min, sec));
                timerHandler.postDelayed(this, 1000);
            }
        });

        // Panic button
        btnPanic.setOnClickListener(v -> showPanicDialog());

        // End walk
        btnEndWalk.setOnClickListener(v -> endWalk());

        // Shake to alert
        sensorManager = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(() -> runOnUiThread(() -> {
            Toast.makeText(this, "Shake detected! Sending alert...", Toast.LENGTH_SHORT).show();
            sendPanicAlert();
        }));
        sensorManager.registerListener(shakeDetector,
                sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER),
                android.hardware.SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        gMap.setMyLocationEnabled(true);

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000).build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location == null) return;

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Calculate distance
                if (lastLocation != null) {
                    float dist = lastLocation.distanceTo(location);
                    totalDistanceMeters += dist;
                    updateDistanceDisplay();
                }
                lastLocation = location;

                // Breadcrumb trail
                breadcrumbs.add(latLng);
                updateMap(latLng);

                // Update Firestore location
                updateFirestoreLocation(location);

                // SMS location update every 10 points if no internet
                if (breadcrumbs.size() % 10 == 0) {
                    for (String phone : guardianPhones) {
                        SmsHelper.sendLocationUpdateSms(ActiveWalkActivity.this,
                                phone, userName, location.getLatitude(), location.getLongitude());
                    }
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void updateMap(LatLng latLng) {
        gMap.clear();
        gMap.addMarker(new MarkerOptions().position(latLng).title("You are here"));
        if (breadcrumbs.size() > 1) {
            gMap.addPolyline(new PolylineOptions()
                    .addAll(breadcrumbs)
                    .width(8f)
                    .color(android.graphics.Color.parseColor("#1A237E")));
        }
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
    }

    private void updateDistanceDisplay() {
        if (totalDistanceMeters < 1000) {
            tvDistance.setText(String.format("%.0f m", totalDistanceMeters));
        } else {
            tvDistance.setText(String.format("%.2f km", totalDistanceMeters / 1000));
        }
    }

    private void updateFirestoreLocation(Location location) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("journeyId", journeyId);
        data.put("timestamp", System.currentTimeMillis());
        db.collection("locations").document(uid).set(data);
    }

    private void showPanicDialog() {
        new AlertDialog.Builder(this)
                .setTitle("PANIC ALERT")
                .setMessage("Send emergency alert to ALL guardians?")
                .setPositiveButton("YES, SEND ALERT", (d, w) -> sendPanicAlert())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPanicAlert() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        // Save to Firestore
        Map<String, Object> alert = new HashMap<>();
        alert.put("userId", uid);
        alert.put("type", "PANIC");
        alert.put("journeyId", journeyId);
        alert.put("timestamp", System.currentTimeMillis());
        if (lastLocation != null) {
            alert.put("lat", lastLocation.getLatitude());
            alert.put("lng", lastLocation.getLongitude());
        }
        db.collection("alerts").add(alert);

        // Update journey alert count
        if (!journeyId.isEmpty()) {
            db.collection("journeys").document(journeyId)
                    .update("alertsTriggered",
                            com.google.firebase.firestore.FieldValue.increment(1));
        }

        // SMS fallback
        if (lastLocation != null) {
            for (String phone : guardianPhones) {
                SmsHelper.sendPanicSms(this, phone, userName,
                        lastLocation.getLatitude(), lastLocation.getLongitude());
            }
        }

        Toast.makeText(this, "Alert sent to all guardians!", Toast.LENGTH_LONG).show();
    }

    private void endWalk() {
        // Save completed journey data
        if (!journeyId.isEmpty()) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("status", "completed");
            updates.put("endTime", System.currentTimeMillis());
            updates.put("distanceMeters", totalDistanceMeters);
            updates.put("durationMinutes", elapsedSeconds / 60);
            if (lastLocation != null) {
                updates.put("endLat", lastLocation.getLatitude());
                updates.put("endLng", lastLocation.getLongitude());
            }
            db.collection("journeys").document(journeyId).update(updates);
        }

        // SMS safe arrival
        for (String phone : guardianPhones) {
            SmsHelper.sendSafeArrivalSms(this, phone, userName, destination);
        }

        // Stop everything
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        timerHandler.removeCallbacksAndMessages(null);
        sensorManager.unregisterListener(shakeDetector);

        // Go to safe arrival screen
        android.content.Intent intent = new android.content.Intent(this, SafeArrivalActivity.class);
        intent.putExtra("journeyId", journeyId);
        intent.putExtra("distanceMeters", totalDistanceMeters);
        intent.putExtra("durationMinutes", elapsedSeconds / 60);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback);
        timerHandler.removeCallbacksAndMessages(null);
        if (sensorManager != null) sensorManager.unregisterListener(shakeDetector);
    }
}