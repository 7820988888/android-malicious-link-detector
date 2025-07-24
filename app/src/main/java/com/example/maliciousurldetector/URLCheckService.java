package com.example.maliciousurldetector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class URLCheckService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("malicious_url")) {
            String maliciousUrl = intent.getStringExtra("malicious_url");

            if (maliciousUrl != null && !maliciousUrl.isEmpty()) {
                Log.d("URLCheckService", "🔍 Checking URL: " + maliciousUrl);

                // 🧠 Optional: Remove this check to allow rescanning every time
                // if (ScannedUrlStore.isAlreadyScanned(maliciousUrl)) {
                //     Log.w("URLCheckService", "⚠️ Already scanned: " + maliciousUrl);
                //     NotificationHelper.showSecurityAlert(getApplicationContext(), maliciousUrl, "Previously Detected");
                //     return START_NOT_STICKY;
                // }

                // 🔄 Scan every time (even if URL is repeated)
                UrlScanner.scan(getApplicationContext(), maliciousUrl, new UrlScanner.ScanCallback() {
                    @Override
                    public void onResult(boolean isMalicious, String source) {
                        if (isMalicious) {
                            ScannedUrlStore.addMaliciousUrl(maliciousUrl);
                            NotificationHelper.showSecurityAlert(getApplicationContext(), maliciousUrl, source);
                            Log.w("URLCheckService", "🚨 Detected malicious URL from " + source);
                        } else {
                            Log.i("URLCheckService", "✅ URL is safe: " + maliciousUrl);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("URLCheckService", "❌ Error during scan: " + error);
                    }
                });
            }
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
