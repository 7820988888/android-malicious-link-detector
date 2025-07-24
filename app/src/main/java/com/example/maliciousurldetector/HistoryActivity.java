package com.example.maliciousurldetector;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.historyListView);

        // Dummy data — replace with Firebase or DB data later
        String[] scanHistory = {
                "https://malicious-site.com - ⚠️ Blocked",
                "https://safe-website.com - ✅ Safe",
                "https://fake-login.net - ⚠️ Blocked"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                scanHistory
        );

        historyListView.setAdapter(adapter);
    }
}
