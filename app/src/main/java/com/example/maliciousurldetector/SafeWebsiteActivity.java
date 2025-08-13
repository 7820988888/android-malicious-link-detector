package com.example.maliciousurldetector;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class SafeWebsiteActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_website);

        Button shoppingBtn = findViewById(R.id.shoppingSitesBtn);
        Button foodBtn = findViewById(R.id.foodSitesBtn);
        Button secureAppBtn = findViewById(R.id.secureAppBtn); // ✅ new button

        shoppingBtn.setOnClickListener(v -> startActivity(new Intent(this, ShoppingSitesActivity.class)));
        foodBtn.setOnClickListener(v -> startActivity(new Intent(this, FoodSitesActivity.class)));

        secureAppBtn.setOnClickListener(v -> {
            Uri playStoreUri = Uri.parse("https://play.google.com/store");
            Intent intent = new Intent(Intent.ACTION_VIEW, playStoreUri);
            intent.setPackage("com.android.vending"); // open in Play Store app if available
            startActivity(intent);
        });
    }
}