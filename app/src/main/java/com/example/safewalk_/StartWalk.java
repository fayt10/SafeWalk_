package com.example.safewalk_;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;

// FIX: added missing Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Locale;

public class StartWalk extends AppCompatActivity {

    // Views
    private EditText etDestination;
    private TextView tvArrivalTime;
    private Button btnPickTime;
    private Button btnBeginWalk;
    private LinearLayout btnHome, btnSchool, btnWork;
    private ImageView btnAddGuardian, btnManageGuardians;
    private Spinner spinnerCheckin;
    private TextView tvGuardianCount;

    // State
    private int selectedHour   = -1;
    private int selectedMinute = -1;

    // Saved location addresses — replace with real data from your DB/preferences
    private static final String HOME_ADDRESS   = "123 Maple St, Quezon City";
    private static final String SCHOOL_ADDRESS = "University of the Philippines, Diliman";
    private static final String WORK_ADDRESS   = "Bonifacio Global City, Taguig";

    private static final int REQUEST_ADD_GUARDIAN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_walk);

        initViews();
        setupCheckinSpinner();
        setupClickListeners();
        // NOTE: btnBeginWalk listener is set once in setupClickListeners() only.
        //       No duplicate listener is set here.
    }

    // ── Initialization ────────────────────────────────────────────────────────

    private void initViews() {
        etDestination      = findViewById(R.id.etDestination);
        tvArrivalTime      = findViewById(R.id.tvArrivalTime);
        btnPickTime        = findViewById(R.id.btnPickTime);
        btnBeginWalk       = findViewById(R.id.btnBeginWalk);
        btnHome            = findViewById(R.id.btnHome);
        btnSchool          = findViewById(R.id.btnSchool);
        btnWork            = findViewById(R.id.btnWork);
        btnAddGuardian     = findViewById(R.id.btnAddGuardian);
        btnManageGuardians = findViewById(R.id.btnManageGuardians);
        spinnerCheckin     = findViewById(R.id.spinnerCheckin);
        tvGuardianCount    = findViewById(R.id.tvGuardianCount);
    }

    private void setupCheckinSpinner() {
        String[] intervals = {
                "5 minutes",
                "10 minutes",
                "15 minutes",
                "30 minutes",
                "1 hour"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                intervals
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCheckin.setAdapter(adapter);
        spinnerCheckin.setSelection(2); // default: 15 minutes
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        // Quick location buttons
        btnHome.setOnClickListener(v   -> setDestination(HOME_ADDRESS));
        btnSchool.setOnClickListener(v -> setDestination(SCHOOL_ADDRESS));
        btnWork.setOnClickListener(v   -> setDestination(WORK_ADDRESS));

        // Arrival time picker
        btnPickTime.setOnClickListener(v   -> showTimePicker());
        tvArrivalTime.setOnClickListener(v -> showTimePicker());

        // Guardian management
        btnAddGuardian.setOnClickListener(v     -> openAddGuardian());
        btnManageGuardians.setOnClickListener(v -> openManageGuardians());

        // FIX: single listener — calls beginWalk() which handles validation + Firebase
        btnBeginWalk.setOnClickListener(v -> beginWalk());
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Fills the destination field with a saved location address. */
    private void setDestination(String address) {
        etDestination.setText(address);
        etDestination.setSelection(address.length());
    }

    /** Opens a TimePickerDialog and updates the arrival time display. */
    private void showTimePicker() {
        Calendar calendar  = Calendar.getInstance();
        int currentHour    = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute  = calendar.get(Calendar.MINUTE);

        int initHour   = (selectedHour   >= 0) ? selectedHour   : currentHour;
        int initMinute = (selectedMinute >= 0) ? selectedMinute : currentMinute;

        new TimePickerDialog(
                this,
                R.style.TimePickerTheme,
                (view, hourOfDay, minute) -> {
                    selectedHour   = hourOfDay;
                    selectedMinute = minute;
                    tvArrivalTime.setText(formatTime(hourOfDay, minute));
                },
                initHour,
                initMinute,
                false // 12-hour format
        ).show();
    }

    /** Formats hours and minutes into a 12-hour string, e.g. "9:05 PM". */
    private String formatTime(int hour, int minute) {
        String period   = (hour >= 12) ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period);
    }

    /** Opens the Add Guardian screen. */
    private void openAddGuardian() {
        Intent intent = new Intent(this, GuardianPickerActivity.class);
        startActivityForResult(intent, REQUEST_ADD_GUARDIAN);
    }

    /** Opens the Manage Guardians screen. */
    private void openManageGuardians() {
        startActivity(new Intent(this, ManageGuardiansActivity.class));
    }

    /**
     * FIX: Validates inputs FIRST, then creates the Firestore journey document,
     * then launches ActiveWalkActivity — all in the correct order.
     * Previously: Firebase ran before validation, variables used before
     * definition, and the activity was launched twice.
     */
    private void beginWalk() {
        String destination = etDestination.getText().toString().trim();

        if (destination.isEmpty()) {
            etDestination.setError("Please enter a destination");
            etDestination.requestFocus();
            return;
        }

        if (selectedHour < 0) {
            Toast.makeText(this, "Please set an expected arrival time", Toast.LENGTH_SHORT).show();
            return;
        }

        String checkinInterval = spinnerCheckin.getSelectedItem().toString();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        JourneyData journey = new JourneyData(uid, destination, checkinInterval);
        journey.arrivalTime = selectedHour + ":" + selectedMinute;

        FirebaseFirestore.getInstance().collection("journeys").add(journey)
                .addOnSuccessListener(ref -> {
                    String journeyId = ref.getId();
                    ref.update("journeyId", journeyId);
                    Intent intent = new Intent(this, ActiveWalkActivity.class);
                    intent.putExtra(ActiveWalkActivity.EXTRA_DESTINATION, destination);
                    intent.putExtra(ActiveWalkActivity.EXTRA_ARRIVAL_HOUR, selectedHour);
                    intent.putExtra(ActiveWalkActivity.EXTRA_ARRIVAL_MINUTE, selectedMinute);
                    intent.putExtra(ActiveWalkActivity.EXTRA_CHECKIN_INTERVAL, checkinInterval);
                    intent.putExtra(ActiveWalkActivity.EXTRA_JOURNEY_ID, journeyId);
                    startActivity(intent);
                });
    }

    // ── Activity Result (guardian selection) ──────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_GUARDIAN && resultCode == RESULT_OK && data != null) {
            int count = data.getIntExtra("guardian_count", 0);
            updateGuardianCount(count);
        }
    }

    /** Updates the guardian count label displayed under the avatar stack. */
    private void updateGuardianCount(int count) {
        if (count == 0) {
            tvGuardianCount.setText("No guardians selected");
        } else if (count == 1) {
            tvGuardianCount.setText("1 guardian notified");
        } else {
            tvGuardianCount.setText(count + " guardians notified");
        }
    }
}