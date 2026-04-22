package com.example.safewalk_;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnSignUp;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // If already logged in, skip straight to MainActivity
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            goToMain();
            return;
        }

        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);
        btnSignUp  = findViewById(R.id.btnSignUp);

        btnLogin.setOnClickListener(v -> loginUser());
        btnSignUp.setOnClickListener(v -> registerUser());
    }

    private void loginUser() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> goToMain())
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Sign In");
                    Toast.makeText(this,
                            "Login failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void registerUser() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating account…");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> goToMain())
                .addOnFailureListener(e -> {
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Create Account");
                    Toast.makeText(this,
                            "Sign-up failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
