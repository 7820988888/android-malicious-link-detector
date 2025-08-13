package com.example.maliciousurldetector;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ShoppingSitesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_sites);

        findViewById(R.id.btnAmazon).setOnClickListener(v -> openWebsite("https://www.amazon.in"));
        findViewById(R.id.btnFlipkart).setOnClickListener(v -> openWebsite("https://www.flipkart.com"));
        findViewById(R.id.btnMyntra).setOnClickListener(v -> openWebsite("https://www.myntra.com"));
        findViewById(R.id.btnMeesho).setOnClickListener(v -> openWebsite("https://www.meesho.com"));
    }

    private void openWebsite(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }
}
