package com.example.safewalk_;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

    private TextView tvStatus, tvSubStatus;


    // State
    private int selectedHour = -1;
    private int selectedMinute = -1;

    // Saved location addresses — replace with real data from your DB/preferences
    private static final String HOME_ADDRESS   = "123 Maple St, Quezon City";
    private static final String SCHOOL_ADDRESS = "University of the Philippines, Diliman";
    private static final String WORK_ADDRESS   = "Bonifacio Global City, Taguig";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_walk);

        initViews();
        setupCheckinSpinner();
        setupClickListeners();

        tvStatus = findViewById(R.id.tvStatus);
        tvSubStatus = findViewById(R.id.tvSubStatus);
        btnBeginWalk = findViewById(R.id.btnBeginWalk);

        btnBeginWalk.setOnClickListener(v -> startWalk());
    }

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Click Listeners
    // -------------------------------------------------------------------------

    private void setupClickListeners() {

        // Quick location buttons
        btnHome.setOnClickListener(v -> setDestination(HOME_ADDRESS));
        btnSchool.setOnClickListener(v -> setDestination(SCHOOL_ADDRESS));
        btnWork.setOnClickListener(v -> setDestination(WORK_ADDRESS));

        // Arrival time picker
        btnPickTime.setOnClickListener(v -> showTimePicker());
        tvArrivalTime.setOnClickListener(v -> showTimePicker());

        // Guardian management
        btnAddGuardian.setOnClickListener(v -> openAddGuardian());
        btnManageGuardians.setOnClickListener(v -> openManageGuardians());

        // Begin Walk
        btnBeginWalk.setOnClickListener(v -> beginWalk());
    }

    // -------------------------------------------------------------------------
    // Actions
    // -------------------------------------------------------------------------

    /**
     * Fills the destination field with a saved location address.
     */
    private void setDestination(String address) {
        etDestination.setText(address);
        etDestination.setSelection(address.length()); // move cursor to end
    }

    /**
     * Opens a Material TimePickerDialog and updates the arrival time display.
     */
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int currentHour   = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        // Use previously selected time if available
        int initHour   = (selectedHour >= 0)   ? selectedHour   : currentHour;
        int initMinute = (selectedMinute >= 0)  ? selectedMinute : currentMinute;

        TimePickerDialog dialog = new TimePickerDialog(
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
        );
        dialog.show();
    }

    /**
     * Formats hours and minutes into a readable 12-hour string, e.g. "9:05 PM".
     */
    private String formatTime(int hour, int minute) {
        String period = (hour >= 12) ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, period);
    }

    /**
     * Opens the Add Guardian screen (replace with your actual target Activity).
     */
    private void openAddGuardian() {
        // TODO: replace GuardianPickerActivity with your actual class
        Intent intent = new Intent(this, GuardianPickerActivity.class);
        startActivityForResult(intent, REQUEST_ADD_GUARDIAN);
    }

    /**
     * Opens the Manage Guardians screen.
     */
    private void openManageGuardians() {
        Intent intent = new Intent(this, ManageGuardiansActivity.class);
        startActivity(intent);
    }

    /**
     * Validates fields and launches the active walk screen.
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

        // TODO: replace ActiveWalkActivity with your actual class
        Intent intent = new Intent(this, ActiveWalkActivity.class);
        intent.putExtra(ActiveWalkActivity.EXTRA_DESTINATION,      destination);
        intent.putExtra(ActiveWalkActivity.EXTRA_ARRIVAL_HOUR,     selectedHour);
        intent.putExtra(ActiveWalkActivity.EXTRA_ARRIVAL_MINUTE,   selectedMinute);
        intent.putExtra(ActiveWalkActivity.EXTRA_CHECKIN_INTERVAL, checkinInterval);
        startActivity(intent);
    }

    // -------------------------------------------------------------------------
    // Activity Result (guardian selection)
    // -------------------------------------------------------------------------

    private static final int REQUEST_ADD_GUARDIAN = 1001;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_GUARDIAN && resultCode == RESULT_OK && data != null) {
            // Update the guardian count label after adding a new guardian
            int count = data.getIntExtra("guardian_count", 0);
            updateGuardianCount(count);
        }
    }

    /**
     * Updates the guardian count label displayed under the avatar stack.
     */
    private void updateGuardianCount(int count) {
        if (count == 0) {
            tvGuardianCount.setText("No guardians selected");
        } else if (count == 1) {
            tvGuardianCount.setText("1 guardian notified");
        } else {
            tvGuardianCount.setText(count + " guardians notified");
        }
    }

    private void startWalk() {
        tvStatus.setText("Walking in progress...");
        tvSubStatus.setText("Journey started just now");

        Toast.makeText(this, "Walk started!", Toast.LENGTH_SHORT).show();
    }
}