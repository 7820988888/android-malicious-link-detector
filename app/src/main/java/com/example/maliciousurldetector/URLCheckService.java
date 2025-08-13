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
                Log.d("URLCheckService", "🔍 Checking URL via VirusTotal: " + maliciousUrl);

                // VirusTotalHelper वापरत आहे
                VirusTotalHelper.checkUrl(getApplicationContext(), maliciousUrl, new VirusTotalHelper.ResultCallback() {
                    @Override
                    public void onResult(boolean isMalicious) {
                        if (isMalicious) {
                            ScannedUrlStore.addMaliciousUrl(maliciousUrl);
                            NotificationHelper.showSecurityAlert(getApplicationContext(), maliciousUrl, "VirusTotal");
                            Log.w("URLCheckService", "🚨 VirusTotal detected malicious URL");
                        } else {
                            Log.i("URLCheckService", "✅ VirusTotal marked URL safe");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("URLCheckService", "❌ VirusTotal error: " + error);
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
