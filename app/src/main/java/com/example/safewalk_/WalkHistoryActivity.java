package com.example.safewalk_;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.*;

public class WalkHistoryActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private TextView tvTotalWalks, tvTotalDistance, tvTotalTime,
            tvNightWalks, tvAvgDuration, tvPanicCount;
    private RecyclerView rvHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_history);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        tvTotalWalks   = findViewById(R.id.tvTotalWalks);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        tvTotalTime    = findViewById(R.id.tvTotalTime);
        tvNightWalks   = findViewById(R.id.tvNightWalks);
        tvAvgDuration  = findViewById(R.id.tvAvgDuration);
        tvPanicCount   = findViewById(R.id.tvPanicCount);
        rvHistory      = findViewById(R.id.rvWalkHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        loadJourneyData();
    }

    private void loadJourneyData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("journeys")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "completed")
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> journeys = new ArrayList<>();
                    float totalDist = 0;
                    long totalMins = 0;
                    int nightCount = 0;
                    int panicCount = 0;

                    for (var doc : snap.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        journeys.add(data);

                        // Aggregate stats
                        Object dist = data.get("distanceMeters");
                        if (dist instanceof Number) totalDist += ((Number) dist).floatValue();

                        Object dur = data.get("durationMinutes");
                        if (dur instanceof Number) totalMins += ((Number) dur).longValue();

                        Object alerts = data.get("alertsTriggered");
                        if (alerts instanceof Number) panicCount += ((Number) alerts).intValue();

                        // Check night walk
                        Object startTs = data.get("startTime");
                        if (startTs instanceof Number) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(((Number) startTs).longValue());
                            int hour = cal.get(Calendar.HOUR_OF_DAY);
                            if (hour >= 21 || hour < 5) nightCount++;
                        }
                    }

                    int total = journeys.size();
                    long avgMins = total > 0 ? totalMins / total : 0;

                    // Update stats UI
                    tvTotalWalks.setText(String.valueOf(total));
                    tvTotalDistance.setText(String.format("%.1f km", totalDist / 1000));
                    tvTotalTime.setText(totalMins + " min");
                    tvNightWalks.setText(String.valueOf(nightCount));
                    tvAvgDuration.setText(avgMins + " min");
                    tvPanicCount.setText(String.valueOf(panicCount));

                    // Set adapter
                    rvHistory.setAdapter(new JourneyHistoryAdapter(journeys));
                });
    }
}