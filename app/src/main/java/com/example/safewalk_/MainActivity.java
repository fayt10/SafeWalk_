package com.example.safewalk_;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting, tvStatus, tvSubStatus;
    private CardView btnStartWalk, btnPanic, btnGuardians, btnHistory;
    private MapView mapPreview;
    private GoogleMap gMap;

    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Views
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStatus = findViewById(R.id.tvStatus);
        tvSubStatus = findViewById(R.id.tvSubStatus);

        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnPanic = findViewById(R.id.btnPanic);
        btnGuardians = findViewById(R.id.btnGuardians);
        btnHistory = findViewById(R.id.btnHistory);

        mapPreview = findViewById(R.id.mapPreview);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                return true;

            } else if (id == R.id.nav_profile) {
                Toast.makeText(this, "Profile section", Toast.LENGTH_SHORT).show();
                return true;

            } else if (id == R.id.nav_history) {
                Toast.makeText(this, "History section", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        // Setup MapView
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapPreview.onCreate(mapViewBundle);

        mapPreview.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                gMap = googleMap;
            }
        });

        // Set greeting
        updateGreeting("Janel");

        // ✅ Correct button click (ONLY navigation)
        btnStartWalk.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StartWalk.class);
            startActivity(intent);
        });

        btnPanic.setOnClickListener(v -> triggerPanic());
        btnGuardians.setOnClickListener(v -> openGuardians());
        btnHistory.setOnClickListener(v -> openHistory());
    }

    private void updateGreeting(String userName) {
        tvGreeting.setText("Good evening, " + userName);
    }

    private void triggerPanic() {
        Toast.makeText(this, "Panic/SOS activated!", Toast.LENGTH_LONG).show();
    }

    private void openGuardians() {
        Toast.makeText(this, "Opening Guardians management", Toast.LENGTH_SHORT).show();
    }

    private void openHistory() {
        Toast.makeText(this, "Opening Walk History", Toast.LENGTH_SHORT).show();
    }

    // MapView lifecycle methods
    @Override
    protected void onStart() {
        super.onStart();
        mapPreview.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapPreview.onResume();
    }

    @Override
    protected void onPause() {
        mapPreview.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapPreview.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mapPreview.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapPreview.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);

        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapPreview.onSaveInstanceState(mapViewBundle);
    }
}