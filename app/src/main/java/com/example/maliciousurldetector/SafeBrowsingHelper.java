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

    public interface ResultCallback {
        void onResult(boolean isDangerous);
        void onError(String error);
    }

    public static void checkUrl(Context context, String url, ResultCallback callback) {
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
                    .put("POTENTIALLY_HARMFUL_APPLICATION"));
            threatInfo.put("platformTypes", new JSONArray().put("ANY_PLATFORM"));
            threatInfo.put("threatEntryTypes", new JSONArray().put("URL"));
            threatInfo.put("threatEntries", new JSONArray().put(new JSONObject().put("url", url)));

            requestBody.put("threatInfo", threatInfo);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    endpoint,
                    requestBody,
                    response -> {
                        boolean isDangerous = response.has("matches");
                        if (isDangerous) {
                            triggerAllAlerts(context, url);
                        }
                        callback.onResult(isDangerous);
                    },
                    error -> {
                        Log.e("SafeBrowsing", "API Error: " + error.toString());
                        fallbackToVirusTotal(context, url, callback);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cache-Control", "no-cache"); // 🔁 Disable cache
                    return headers;
                }
            };

            request.setShouldCache(false); // ❗Disable Volley caching
            Volley.newRequestQueue(context).add(request);

        } catch (JSONException e) {
            Log.e("SafeBrowsing", "JSON Error: " + e.getMessage());
            fallbackToVirusTotal(context, url, callback);
        }
    }

    private static void fallbackToVirusTotal(Context context, String url, ResultCallback callback) {
        Log.d("SafeBrowsing", "Fallback to VirusTotal...");
        VirusTotalApi vt = new VirusTotalApi(context);
        vt.scanUrl(url, new VirusTotalApi.VirusTotalCallback() {
            @Override
            public void onResult(boolean isMalicious, JSONObject result) {
                if (isMalicious) {
                    triggerAllAlerts(context, url);
                }
                callback.onResult(isMalicious);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("VirusTotal", "Error: " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    // 🔔 Alert - Vibrate, Alarm, and Notification
    public static void triggerAllAlerts(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean soundEnabled = prefs.getBoolean("notification_sound", true);

        if (notificationsEnabled) {
            NotificationUtils.sendNotification(context, "⚠️ Malicious URL Detected", url);
        }

        if (soundEnabled) {
            AlarmUtils.playAlarm(context);
        }

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
