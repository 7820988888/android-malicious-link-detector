package com.example.maliciousurldetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SafeBrowsingHelper {

    private static final String TAG = "SafeBrowsingHelper";

    public interface ResultCallback {
        void onResult(boolean isDangerous);
        void onError(String error);
    }

    /**
     * ✅ Legacy check method - maintained for compatibility
     */
    public static void checkUrl(Context context, String url, ResultCallback callback) {
        checkUrlWithCacheBypass(context, url, callback);
    }

    /**
     * ✅ Enhanced URL check with cache bypass for real-time detection
     */
    public static void checkUrlWithCacheBypass(Context context, String url, ResultCallback callback) {
        Log.d(TAG, "🔍 Starting fresh Safe Browsing check for: " + url);
        
        String apiKey = context.getString(R.string.google_api_key);
        String endpoint = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("client", new JSONObject()
                    .put("clientId", "malicious-url-detector")
                    .put("clientVersion", "1.0"));

            JSONObject threatInfo = new JSONObject();
            threatInfo.put("threatTypes", new JSONArray()
                    .put("MALWARE")
                    .put("SOCIAL_ENGINEERING")
                    .put("UNWANTED_SOFTWARE")
                    .put("POTENTIALLY_HARMFUL_APPLICATION")
                    .put("THREAT_TYPE_UNSPECIFIED")); // Added for broader detection

            threatInfo.put("platformTypes", new JSONArray()
                    .put("ANY_PLATFORM")
                    .put("ANDROID")); // Platform specific

            threatInfo.put("threatEntryTypes", new JSONArray()
                    .put("URL"));

            threatInfo.put("threatEntries", new JSONArray()
                    .put(new JSONObject().put("url", url)));

            requestBody.put("threatInfo", threatInfo);

            Log.d(TAG, "📤 Sending Safe Browsing request for: " + url);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    endpoint,
                    requestBody,
                    response -> {
                        boolean isDangerous = response.has("matches");
                        
                        if (isDangerous) {
                            Log.w(TAG, "🚨 Safe Browsing detected threat: " + url);
                            try {
                                JSONArray matches = response.getJSONArray("matches");
                                for (int i = 0; i < matches.length(); i++) {
                                    JSONObject match = matches.getJSONObject(i);
                                    String threatType = match.optString("threatType", "UNKNOWN");
                                    Log.w(TAG, "  - Threat type: " + threatType);
                                }
                            } catch (JSONException e) {
                                Log.w(TAG, "Could not parse threat details");
                            }
                            triggerAllAlerts(context, url);
                        } else {
                            Log.i(TAG, "✅ Safe Browsing: URL appears clean: " + url);
                        }
                        
                        callback.onResult(isDangerous);
                    },
                    error -> {
                        Log.e(TAG, "❌ Safe Browsing API Error: " + error.toString());
                        // Don't fallback here - let UrlScanner handle fallback logic
                        callback.onError("Safe Browsing API failed: " + error.toString());
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    // Force cache bypass
                    headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                    headers.put("Pragma", "no-cache");
                    headers.put("Expires", "0");
                    // Add timestamp to force unique requests
                    headers.put("X-Request-Time", String.valueOf(System.currentTimeMillis()));
                    headers.put("User-Agent", "MaliciousURLDetector/1.0 (Android)");
                    return headers;
                }
            };

            request.setShouldCache(false); // Disable Volley caching
            Volley.newRequestQueue(context).add(request);

        } catch (JSONException e) {
            Log.e(TAG, "❌ JSON Error: " + e.getMessage());
            callback.onError("Request formation error: " + e.getMessage());
        }
    }

    /**
     * ✅ Emergency Safe Browsing check - faster with minimal processing
     */
    public static void emergencyCheck(Context context, String url, ResultCallback callback) {
        Log.w(TAG, "🚨 Emergency Safe Browsing check for: " + url);
        
        String apiKey = context.getString(R.string.google_api_key);
        String endpoint = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("client", new JSONObject()
                    .put("clientId", "malicious-url-detector-emergency")
                    .put("clientVersion", "1.0"));

            JSONObject threatInfo = new JSONObject();
            // Use primary threat types for faster response
            threatInfo.put("threatTypes", new JSONArray()
                    .put("MALWARE")
                    .put("SOCIAL_ENGINEERING"));
            
            threatInfo.put("platformTypes", new JSONArray().put("ANY_PLATFORM"));
            threatInfo.put("threatEntryTypes", new JSONArray().put("URL"));
            threatInfo.put("threatEntries", new JSONArray()
                    .put(new JSONObject().put("url", url)));

            requestBody.put("threatInfo", threatInfo);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    endpoint,
                    requestBody,
                    response -> {
                        boolean isDangerous = response.has("matches");
                        if (isDangerous) {
                            Log.w(TAG, "🚨 Emergency detection: threat found!");
                            triggerAllAlerts(context, url);
                        }
                        callback.onResult(isDangerous);
                    },
                    error -> {
                        Log.e(TAG, "❌ Emergency check failed: " + error.toString());
                        callback.onError("Emergency check failed");
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cache-Control", "no-cache");
                    headers.put("X-Request-Time", String.valueOf(System.currentTimeMillis()));
                    return headers;
                }
            };

            request.setShouldCache(false);
            Volley.newRequestQueue(context).add(request);

        } catch (JSONException e) {
            Log.e(TAG, "❌ Emergency JSON Error: " + e.getMessage());
            callback.onError("Emergency request failed");
        }
    }

    // 🔔 Alert - Vibrate, Alarm, and Notification
    public static void triggerAllAlerts(Context context, String url) {
        Log.w(TAG, "🚨 Triggering all security alerts for: " + url);
        
        SharedPreferences prefs = context.getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean soundEnabled = prefs.getBoolean("notification_sound", true);

        if (notificationsEnabled) {
            NotificationHelper.showSecurityAlert(context, url, "Real-time Detection");
        }

        if (soundEnabled) {
            AlarmUtils.playAlarm(context);
        }

        // Vibration alert
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(1000);
            }
        }
    }
}
