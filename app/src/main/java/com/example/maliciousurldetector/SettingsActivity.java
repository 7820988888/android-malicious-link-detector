package com.example.maliciousurldetector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotifications, switchRealtime, switchSafeBrowsing, switchDarkMode, switchSound;
    private Button btnClearHistory, btnSetAppLock;
    private Spinner languageSpinner;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ✅ Toolbar setup with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        // ✅ SharedPreferences
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // ✅ Bind UI
        switchNotifications = findViewById(R.id.switch_notifications);
        switchRealtime = findViewById(R.id.switch_realtime);
        switchSafeBrowsing = findViewById(R.id.switch_safe_browsing);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchSound = findViewById(R.id.switch_sound);
        btnClearHistory = findViewById(R.id.btn_clear_history);
        btnSetAppLock = findViewById(R.id.btn_set_applock);
        languageSpinner = findViewById(R.id.language_spinner);

        // ✅ Populate Spinner
        String[] languages = {"English", "Hindi", "Marathi"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        languageSpinner.setAdapter(adapter);

        // ✅ Load saved states
        switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        switchRealtime.setChecked(sharedPreferences.getBoolean("realtime", true));
        switchSafeBrowsing.setChecked(sharedPreferences.getBoolean("safebrowsing", true));
        switchDarkMode.setChecked(sharedPreferences.getBoolean("darkmode", false));
        switchSound.setChecked(sharedPreferences.getBoolean("sound", true));

        // ✅ Save preferences on toggle
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications", isChecked).apply();
        });

        switchRealtime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("realtime", isChecked).apply();
        });

        switchSafeBrowsing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("safebrowsing", isChecked).apply();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("darkmode", isChecked).apply();
            Toast.makeText(this, "Restart app to apply dark mode", Toast.LENGTH_SHORT).show();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("sound", isChecked).apply();
        });

        // ✅ Button actions
        btnClearHistory.setOnClickListener(v -> {
            Toast.makeText(this, "History cleared!", Toast.LENGTH_SHORT).show();
        });

        btnSetAppLock.setOnClickListener(v -> {
            Toast.makeText(this, "App lock feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    // ✅ Handle back button on toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
