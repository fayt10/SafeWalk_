package com.example.safewalk_;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SafeArrivalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_arrival);

        TextView tvSummary = findViewById(R.id.tvSummary);
        Button btnDone = findViewById(R.id.btnDone);

        float distance = getIntent().getFloatExtra("distanceMeters", 0f);
        int duration = getIntent().getIntExtra("durationMinutes", 0);

        String summary = String.format("You walked %.2f km in %d minutes.", distance / 1000, duration);
        tvSummary.setText(summary);

        btnDone.setOnClickListener(v -> {
            finish();
        });
    }
}
