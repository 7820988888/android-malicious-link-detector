package com.example.maliciousurldetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UrlReceiver extends BroadcastReceiver {

    private static final String TAG = "UrlReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String url = null;
        String action = intent.getAction();

        Log.d(TAG, "📨 Received broadcast with action: " + action);

        // Handle different intent actions
        if ("com.example.maliciousurldetector.CHECK_URL".equals(action)) {
            // Custom action from clipboard listener
            url = intent.getStringExtra("url");
            Log.d(TAG, "🔗 URL from clipboard: " + url);
        } else if (Intent.ACTION_SEND.equals(action)) {
            // Shared URL from other apps
            if ("text/plain".equals(intent.getType())) {
                url = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.d(TAG, "📤 Shared URL: " + url);
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            // Direct URL opening
            if (intent.getData() != null) {
                url = intent.getData().toString();
                Log.d(TAG, "🌐 Direct URL: " + url);
            }
        }

        if (url != null && !url.trim().isEmpty()) {
            url = url.trim();
            
            // Check if it's a valid URL
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Log.i(TAG, "✅ Valid URL detected, starting scan: " + url);
                
                // ✅ ALWAYS scan - removed the already scanned check
                // Real-time detection requires fresh scans every time
                startUrlCheckService(context, url);
            } else {
                Log.d(TAG, "❌ Invalid URL format: " + url);
            }
        } else {
            Log.d(TAG, "❌ No URL found in intent");
        }
    }

    /**
     * ✅ Start the URL checking service with enhanced parameters
     */
    private void startUrlCheckService(Context context, String url) {
        try {
            Intent serviceIntent = new Intent(context, URLCheckService.class);
            serviceIntent.putExtra("url", url);
            serviceIntent.putExtra("timestamp", System.currentTimeMillis());
            serviceIntent.putExtra("source", "broadcast_receiver");
            serviceIntent.putExtra("priority", "real_time"); // Mark as real-time detection
            
            context.startService(serviceIntent);
            Log.d(TAG, "🚀 URL check service started for: " + url);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start URL check service: " + e.getMessage());
        }
    }
}
