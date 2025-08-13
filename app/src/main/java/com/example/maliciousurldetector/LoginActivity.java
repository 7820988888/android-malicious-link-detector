package com.example.maliciousurldetector;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class LoginActivity extends AppCompatActivity {

    EditText usernameOrEmailEditText, passwordEditText;
    Button loginButton, googleSignInButton;
    CheckBox rememberMeCheckBox, privacyPolicyCheckBox;
    TextView forgotPasswordText, tvRegisterNow;
    ImageView passwordToggle;

    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 100;

    private boolean isPasswordVisible = false;
    private SharedPreferences securePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // ✅ Google Sign-In Setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 🔧 UI Initialization
        usernameOrEmailEditText = findViewById(R.id.usernameOrEmailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        rememberMeCheckBox = findViewById(R.id.rememberMeCheckBox);
        privacyPolicyCheckBox = findViewById(R.id.privacyPolicyCheckBox);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        tvRegisterNow = findViewById(R.id.tvRegisterNow);
        passwordToggle = findViewById(R.id.passwordToggle);

        // 👁️ Toggle Password Visibility
        passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.hide);
            } else {
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.show);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordEditText.setSelection(passwordEditText.length());
        });

        // 🔐 Encrypted SharedPreferences Setup
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "secure_login_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        // 🔄 Load Saved Login
        if (securePrefs != null && securePrefs.getBoolean("remember", false)) {
            usernameOrEmailEditText.setText(securePrefs.getString("email", ""));
            passwordEditText.setText(securePrefs.getString("password", ""));
            rememberMeCheckBox.setChecked(true);
        }

        // 📧 Email/Password Login
        loginButton.setOnClickListener(v -> {
            String email = usernameOrEmailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "⚠️ Please fill in all fields!", Toast.LENGTH_SHORT).show();
            } else if (!privacyPolicyCheckBox.isChecked()) {
                Toast.makeText(this, "⚠️ Please accept Privacy Policy!", Toast.LENGTH_SHORT).show();
            } else {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // ✅ Save credentials if checked
                                if (rememberMeCheckBox.isChecked() && securePrefs != null) {
                                    securePrefs.edit()
                                            .putString("email", email)
                                            .putString("password", password)
                                            .putBoolean("remember", true)
                                            .apply();
                                } else if (securePrefs != null) {
                                    securePrefs.edit().clear().apply();
                                }

                                Toast.makeText(this, "✅ Login Successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "❌ Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("LoginError", "Firebase login failed", task.getException());
                            }
                        });
            }
        });

        // 🔐 Google Sign-In
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        // Forgot Password
        forgotPasswordText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Register Now
        tvRegisterNow.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN && data != null) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Google Sign-In failed", e);
                Toast.makeText(this, "❌ Google sign-in failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        if (account != null && !isFinishing() && !isDestroyed()) {
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "✅ Signed in as: " + mAuth.getCurrentUser().getDisplayName(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "❌ Google Sign-In failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("FirebaseAuth", "Google Sign-In error", task.getException());
                        }
                    });
        }
    }
}
