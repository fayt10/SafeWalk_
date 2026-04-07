package com.example.safewalk_;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ActiveWalkActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION = "extra_destination";
    public static final String EXTRA_ARRIVAL_HOUR = "extra_arrival_hour";
    public static final String EXTRA_ARRIVAL_MINUTE = "extra_arrival_minute";
    public static final String EXTRA_CHECKIN_INTERVAL = "extra_checkin_interval";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_walk);

        // Receive data from Intent
        String destination = getIntent().getStringExtra(EXTRA_DESTINATION);
        int hour = getIntent().getIntExtra(EXTRA_ARRIVAL_HOUR, 0);
        int minute = getIntent().getIntExtra(EXTRA_ARRIVAL_MINUTE, 0);
        String interval = getIntent().getStringExtra(EXTRA_CHECKIN_INTERVAL);
    }
}