package com.example.maliciousurldetector;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

public class UrlScanner {

    private static final String TAG = "UrlScanner";

    public interface ScanCallback {
        void onResult(boolean isMalicious, String source);
        void onError(String error);
    }

    /**
     * ✅ Enhanced scan method with real-time detection
     * - Always performs fresh scans (no caching)
     * - Uses both Google Safe Browsing and VirusTotal
     * - Implements proper error handling
     */
    public static void scan(Context context, String url, ScanCallback callback) {
        Log.d(TAG, "🔍 Starting fresh scan for URL: " + url);
        
        // Force fresh scan - bypass all caches
        scanWithFreshApis(context, url, callback);
    }

    /**
     * ✅ Performs fresh API calls without cache dependencies
     */
    private static void scanWithFreshApis(Context context, String url, ScanCallback callback) {
        // First try Google Safe Browsing with cache bypass
        SafeBrowsingHelper.checkUrlWithCacheBypass(context, url, new SafeBrowsingHelper.ResultCallback() {
            @Override
            public void onResult(boolean isDangerous) {
                if (isDangerous) {
                    Log.w(TAG, "🚨 Malicious detected by Google Safe Browsing: " + url);
                    callback.onResult(true, "Google Safe Browsing");
                } else {
                    // If Safe Browsing says it's clean, double-check with VirusTotal
                    Log.d(TAG, "✅ Safe Browsing says clean, checking VirusTotal...");
                    scanWithVirusTotal(context, url, callback);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Safe Browsing error, falling back to VirusTotal: " + error);
                // If Safe Browsing fails, use VirusTotal as primary
                scanWithVirusTotal(context, url, callback);
            }
        });
    }

    /**
     * ✅ VirusTotal scan with fresh API call
     */
    private static void scanWithVirusTotal(Context context, String url, ScanCallback callback) {
        VirusTotalApi vt = new VirusTotalApi(context);
        vt.scanUrlFresh(url, new VirusTotalApi.VirusTotalCallback() {
            @Override
            public void onResult(boolean isMalicious, JSONObject fullResult) {
                if (isMalicious) {
                    Log.w(TAG, "🚨 Malicious detected by VirusTotal: " + url);
                    callback.onResult(true, "VirusTotal");
                } else {
                    Log.i(TAG, "✅ URL verified as safe by both APIs: " + url);
                    callback.onResult(false, "Both APIs Verified Safe");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "VirusTotal scan failed: " + errorMessage);
                callback.onError("Both APIs failed: " + errorMessage);
            }
        });
    }

    /**
     * ✅ Emergency scan - for critical real-time detection
     * This method prioritizes speed and multiple verification
     */
    public static void emergencyScan(Context context, String url, ScanCallback callback) {
        Log.w(TAG, "🚨 Emergency scan initiated for: " + url);
        
        // Run both APIs in parallel for fastest detection
        final boolean[] safeBrowsingChecked = {false};
        final boolean[] virusTotalChecked = {false};
        final boolean[] resultReported = {false};
        
        // Google Safe Browsing check
        SafeBrowsingHelper.checkUrlWithCacheBypass(context, url, new SafeBrowsingHelper.ResultCallback() {
            @Override
            public void onResult(boolean isDangerous) {
                safeBrowsingChecked[0] = true;
                if (isDangerous && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onResult(true, "Google Safe Browsing (Emergency)");
                } else if (safeBrowsingChecked[0] && virusTotalChecked[0] && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onResult(false, "Emergency Scan Complete");
                }
            }

            @Override
            public void onError(String error) {
                safeBrowsingChecked[0] = true;
                if (safeBrowsingChecked[0] && virusTotalChecked[0] && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onError("Emergency scan partially failed");
                }
            }
        });
        
        // VirusTotal check (parallel)
        VirusTotalApi vt = new VirusTotalApi(context);
        vt.scanUrlFresh(url, new VirusTotalApi.VirusTotalCallback() {
            @Override
            public void onResult(boolean isMalicious, JSONObject fullResult) {
                virusTotalChecked[0] = true;
                if (isMalicious && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onResult(true, "VirusTotal (Emergency)");
                } else if (safeBrowsingChecked[0] && virusTotalChecked[0] && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onResult(false, "Emergency Scan Complete");
                }
            }

            @Override
            public void onError(String errorMessage) {
                virusTotalChecked[0] = true;
                if (safeBrowsingChecked[0] && virusTotalChecked[0] && !resultReported[0]) {
                    resultReported[0] = true;
                    callback.onError("Emergency scan partially failed");
                }
            }
        });
    }
}
