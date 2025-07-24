package com.example.maliciousurldetector;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SubscriptionActivity extends AppCompatActivity {

    private Button selectFreeTrial, selectMonthly, selectPro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription); // Make sure this XML filename is correct

        // Match the button IDs exactly as defined in XML
        selectFreeTrial = findViewById(R.id.selectFreeTrial);
        selectMonthly = findViewById(R.id.selectMonthly);
        selectPro = findViewById(R.id.selectPro);

        selectFreeTrial.setOnClickListener(v -> {
            Toast.makeText(this, "Free Trial Activated for 3 Trials", Toast.LENGTH_SHORT).show();
            // TODO: Add logic to activate trial
        });

        selectMonthly.setOnClickListener(v -> {
            Toast.makeText(this, "Subscribed for 1 Month (₹200)", Toast.LENGTH_SHORT).show();
            // TODO: Add payment logic
        });

        selectPro.setOnClickListener(v -> {
            Toast.makeText(this, "Subscribed for 3 Months (₹350)", Toast.LENGTH_SHORT).show();
            // TODO: Add payment logic
        });
    }
}
