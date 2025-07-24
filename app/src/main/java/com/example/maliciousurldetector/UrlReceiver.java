package com.example.maliciousurldetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UrlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND)) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (url != null && !url.isEmpty()) {

                // ❌ REMOVE this check to always scan
                // if (ScannedUrlStore.isAlreadyScanned(context, url)) {
                //     Log.d("UrlReceiver", "URL already scanned: " + url);
                //     return;
                // }

                Intent serviceIntent = new Intent(context, URLCheckService.class);
                serviceIntent.putExtra("malicious_url", url);
                serviceIntent.putExtra("timestamp", System.currentTimeMillis());
                context.startService(serviceIntent);
            }
        }
    }
}
