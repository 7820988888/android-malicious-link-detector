package com.example.maliciousurldetector;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class URLCheckService extends Service {

    private static final String TAG = "URLCheckService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "❌ Service started with null intent");
            return START_NOT_STICKY;
        }

        // Handle both old and new parameter names for compatibility
        String urlToCheck = intent.getStringExtra("url");
        if (urlToCheck == null) {
            urlToCheck = intent.getStringExtra("malicious_url"); // Legacy support
        }

        if (urlToCheck != null && !urlToCheck.trim().isEmpty()) {
            urlToCheck = urlToCheck.trim();
            
            String source = intent.getStringExtra("source");
            String priority = intent.getStringExtra("priority");
            long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
            
            Log.i(TAG, "🔍 Starting URL scan - Source: " + source + ", Priority: " + priority);
            Log.i(TAG, "🔗 URL to scan: " + urlToCheck);

            // ✅ ALWAYS scan - removed the already scanned check for real-time detection
            // Real-time protection requires fresh scans every time, even for repeated URLs
            
            if ("real_time".equals(priority)) {
                // High priority real-time scan
                performEmergencyScan(urlToCheck, source, timestamp);
            } else {
                // Standard comprehensive scan
                performStandardScan(urlToCheck, source, timestamp);
            }
        } else {
            Log.w(TAG, "❌ No valid URL provided to service");
        }

        return START_NOT_STICKY;
    }

    /**
     * ✅ Perform emergency scan for real-time detection
     */
    private void performEmergencyScan(String url, String source, long timestamp) {
        Log.w(TAG, "🚨 Emergency scan initiated for: " + url);
        
        UrlScanner.emergencyScan(getApplicationContext(), url, new UrlScanner.ScanCallback() {
            @Override
            public void onResult(boolean isMalicious, String detectionSource) {
                if (isMalicious) {
                    Log.w(TAG, "🚨 EMERGENCY DETECTION: Malicious URL found!");
                    Log.w(TAG, "  - URL: " + url);
                    Log.w(TAG, "  - Detected by: " + detectionSource);
                    Log.w(TAG, "  - Source: " + source);
                    Log.w(TAG, "  - Timestamp: " + timestamp);
                    
                    // Store in history
                    ScannedUrlStore.addMaliciousUrl(url);
                    
                    // Trigger emergency alerts
                    NotificationHelper.showSecurityAlert(getApplicationContext(), url, detectionSource + " (Emergency)");
                    AlarmUtils.triggerAlarm(getApplicationContext(), url);
                    
                } else {
                    Log.i(TAG, "✅ Emergency scan complete: URL is safe - " + url);
                }
                
                // Stop service after scan
                stopSelf();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Emergency scan failed for: " + url);
                Log.e(TAG, "  - Error: " + error);
                
                // Still try to notify user of scan attempt
                NotificationHelper.showScanError(getApplicationContext(), url, error);
                
                stopSelf();
            }
        });
    }

    /**
     * ✅ Perform standard comprehensive scan
     */
    private void performStandardScan(String url, String source, long timestamp) {
        Log.d(TAG, "🔍 Standard scan initiated for: " + url);
        
        UrlScanner.scan(getApplicationContext(), url, new UrlScanner.ScanCallback() {
            @Override
            public void onResult(boolean isMalicious, String detectionSource) {
                if (isMalicious) {
                    Log.w(TAG, "🚨 THREAT DETECTED: Malicious URL found!");
                    Log.w(TAG, "  - URL: " + url);
                    Log.w(TAG, "  - Detected by: " + detectionSource);
                    Log.w(TAG, "  - Source: " + source);
                    Log.w(TAG, "  - Timestamp: " + timestamp);
                    
                    // Store in history for tracking
                    ScannedUrlStore.addMaliciousUrl(url);
                    
                    // Trigger comprehensive alerts
                    NotificationHelper.showSecurityAlert(getApplicationContext(), url, detectionSource);
                    SafeBrowsingHelper.triggerAllAlerts(getApplicationContext(), url);
                    
                } else {
                    Log.i(TAG, "✅ Standard scan complete: URL verified as safe - " + url);
                    
                    // Optional: Show safe notification (can be disabled in settings)
                    // NotificationHelper.showSafeNotification(getApplicationContext(), url);
                }
                
                stopSelf();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Standard scan failed for: " + url);
                Log.e(TAG, "  - Error: " + error);
                Log.e(TAG, "  - Source: " + source);
                
                // Notify user of scan failure
                NotificationHelper.showScanError(getApplicationContext(), url, error);
                
                stopSelf();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "🛑 URLCheckService destroyed");
        super.onDestroy();
    }
}
