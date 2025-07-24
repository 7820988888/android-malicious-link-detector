package com.example.maliciousurldetector;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView messageTextView;
    private Button btnResendEmail, btnAlreadyVerified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        messageTextView = findViewById(R.id.messageTextView);
        btnResendEmail = findViewById(R.id.btnResendEmail);
        btnAlreadyVerified = findViewById(R.id.btnAlreadyVerified);

        if (currentUser == null) {
            Toast.makeText(this, "⚠️ Session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!currentUser.isEmailVerified()) {
            messageTextView.setText("📩 A verification link has been sent. Please check your email and click the link.");
        }

        btnResendEmail.setOnClickListener(v -> {
            currentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "✅ Email resent successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "❌ Failed to resend: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnAlreadyVerified.setOnClickListener(v -> {
            currentUser.reload().addOnCompleteListener(task -> {
                FirebaseUser updatedUser = mAuth.getCurrentUser();
                if (updatedUser != null && updatedUser.isEmailVerified()) {
                    Toast.makeText(this, "✅ Email verified successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "⚠️ Email not verified yet. Please check your inbox.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
