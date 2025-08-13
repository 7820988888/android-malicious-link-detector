package com.example.maliciousurldetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UrlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (url != null && !url.isEmpty()) {

                // ✅ Remove duplicate check — Always scan the URL
                Log.d("UrlReceiver", "Received URL to scan: " + url);

                Intent serviceIntent = new Intent(context, URLCheckService.class);
                serviceIntent.putExtra("malicious_url", url);
                serviceIntent.putExtra("timestamp", System.currentTimeMillis());
                context.startService(serviceIntent);
            }
        }
    }
}
