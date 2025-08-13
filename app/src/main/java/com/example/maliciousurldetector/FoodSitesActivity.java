package com.example.maliciousurldetector;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class FoodSitesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_sites);

        findViewById(R.id.btnZomato).setOnClickListener(v -> openWebsite("https://www.zomato.com"));
        findViewById(R.id.btnSwiggy).setOnClickListener(v -> openWebsite("https://www.swiggy.com"));
        findViewById(R.id.btnBlinkit).setOnClickListener(v -> openWebsite("https://www.blinkit.com"));
        findViewById(R.id.btnZepto).setOnClickListener(v -> openWebsite("https://www.zepto.com/"));
    }

    private void openWebsite(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }
}
