package com.example.safewalk_;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class JourneyHistoryAdapter
        extends RecyclerView.Adapter<JourneyHistoryAdapter.ViewHolder> {

    private final List<Map<String, Object>> journeys;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault());

    public JourneyHistoryAdapter(List<Map<String, Object>> journeys) {
        this.journeys = journeys;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journey_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> j = journeys.get(position);

        String dest = j.containsKey("destination") ? (String) j.get("destination") : "Unknown";
        holder.tvDestination.setText(dest);

        Object dist = j.get("distanceMeters");
        float km = dist instanceof Number ? ((Number) dist).floatValue() / 1000f : 0;
        holder.tvDistance.setText(String.format("%.2f km", km));

        Object dur = j.get("durationMinutes");
        long mins = dur instanceof Number ? ((Number) dur).longValue() : 0;
        holder.tvDuration.setText(mins + " min");

        Object ts = j.get("startTime");
        if (ts instanceof Number) {
            holder.tvDate.setText(sdf.format(new Date(((Number) ts).longValue())));
        }

        Object alerts = j.get("alertsTriggered");
        int alertCount = alerts instanceof Number ? ((Number) alerts).intValue() : 0;
        holder.tvAlerts.setText(alertCount > 0 ? alertCount + " alerts" : "Safe");
        holder.tvAlerts.setTextColor(alertCount > 0 ?
                android.graphics.Color.parseColor("#B71C1C") :
                android.graphics.Color.parseColor("#2E7D32"));
    }

    @Override
    public int getItemCount() { return journeys.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDestination, tvDistance, tvDuration, tvDate, tvAlerts;
        ViewHolder(View v) {
            super(v);
            tvDestination = v.findViewById(R.id.tvJourneyDest);
            tvDistance    = v.findViewById(R.id.tvJourneyDist);
            tvDuration    = v.findViewById(R.id.tvJourneyDuration);
            tvDate        = v.findViewById(R.id.tvJourneyDate);
            tvAlerts      = v.findViewById(R.id.tvJourneyAlerts);
        }
    }
}