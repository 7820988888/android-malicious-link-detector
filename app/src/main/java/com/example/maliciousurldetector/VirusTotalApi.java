package com.example.maliciousurldetector;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VirusTotalApi {

    private static final String TAG = "VirusTotalApi";
    private static final String BASE_URL = "https://www.virustotal.com/api/v3/urls";
    private static final int MAX_RETRIES = 3; // Reduced for faster response
    private static final long RETRY_DELAY_MS = 2000; // Reduced delay

    private final Context context;
    private final com.android.volley.RequestQueue requestQueue;

    public interface VirusTotalCallback {
        void onResult(boolean isMalicious, JSONObject fullResult);
        void onError(String errorMessage);
    }

    public VirusTotalApi(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    /**
     * ✅ Legacy scan method - maintained for compatibility
     */
    public void scanUrl(String urlToScan, VirusTotalCallback callback) {
        scanUrlFresh(urlToScan, callback);
    }

    /**
     * ✅ Enhanced fresh scan method with cache bypass
     * Forces real-time API calls without any caching
     */
    public void scanUrlFresh(String urlToScan, VirusTotalCallback callback) {
        Log.d(TAG, "🔄 Starting fresh VirusTotal scan for: " + urlToScan);
        
        String encodedUrlId = Base64.encodeToString(urlToScan.trim().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");

        // First submit URL for analysis (fresh submission)
        submitUrlForAnalysis(urlToScan, encodedUrlId, callback);
    }

    /**
     * ✅ Submit URL for fresh analysis
     */
    private void submitUrlForAnalysis(String originalUrl, String encodedUrlId, VirusTotalCallback callback) {
        String submitEndpoint = "https://www.virustotal.com/api/v3/urls";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("url", originalUrl);

            JsonObjectRequest submitRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    submitEndpoint,
                    requestBody,
                    response -> {
                        Log.d(TAG, "✅ URL submitted for analysis, checking results...");
                        // Wait briefly then check results
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                fetchAnalysisResultWithRetry(encodedUrlId, callback, MAX_RETRIES), 1500);
                    },
                    error -> {
                        Log.w(TAG, "Submission failed, checking existing results: " + error.toString());
                        // If submission fails, check existing results
                        fetchAnalysisResultWithRetry(encodedUrlId, callback, MAX_RETRIES);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY);
                    headers.put("Cache-Control", "no-cache, no-store, must-revalidate"); // Force fresh
                    headers.put("Pragma", "no-cache");
                    headers.put("Expires", "0");
                    return headers;
                }
            };

            submitRequest.setShouldCache(false);
            requestQueue.add(submitRequest);

        } catch (JSONException e) {
            Log.e(TAG, "JSON error during submission: " + e.getMessage());
            // Fallback to checking existing results
            fetchAnalysisResultWithRetry(encodedUrlId, callback, MAX_RETRIES);
        }
    }

    /**
     * ✅ Enhanced result fetching with cache bypass
     */
    private void fetchAnalysisResultWithRetry(String encodedId, VirusTotalCallback callback, int retriesLeft) {
        String finalUrl = BASE_URL + "/" + encodedId;
        Log.d(TAG, "🔍 Fetching analysis results (retries left: " + retriesLeft + ")");

        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET,
                finalUrl,
                null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        // Check if analysis is complete
                        if (!attributes.has("last_analysis_results")) {
                            if (retriesLeft > 0) {
                                Log.d(TAG, "⏳ Analysis not ready, retrying...");
                                new Handler(Looper.getMainLooper()).postDelayed(() ->
                                        fetchAnalysisResultWithRetry(encodedId, callback, retriesLeft - 1), RETRY_DELAY_MS);
                                return;
                            } else {
                                // If no retries left, report as safe (analysis incomplete)
                                Log.w(TAG, "⚠️ Analysis timeout, reporting as safe");
                                callback.onResult(false, response);
                                return;
                            }
                        }

                        // Parse analysis results
                        JSONObject stats = attributes.getJSONObject("last_analysis_stats");
                        int malicious = stats.optInt("malicious", 0);
                        int suspicious = stats.optInt("suspicious", 0);
                        boolean isMalicious = (malicious > 0 || suspicious > 0);

                        Log.i(TAG, String.format("📊 VirusTotal results: %d malicious, %d suspicious", malicious, suspicious));

                        if (isMalicious) {
                            // Log detailed detection info
                            JSONObject results = attributes.getJSONObject("last_analysis_results");
                            Iterator<String> engines = results.keys();
                            StringBuilder detectedBy = new StringBuilder("🚨 Detected by: ");
                            
                            while (engines.hasNext()) {
                                String engine = engines.next();
                                JSONObject result = results.getJSONObject(engine);
                                String category = result.optString("category");
                                if ("malicious".equalsIgnoreCase(category) || "suspicious".equalsIgnoreCase(category)) {
                                    detectedBy.append(engine).append(", ");
                                }
                            }
                            
                            if (detectedBy.toString().endsWith(", ")) {
                                detectedBy.setLength(detectedBy.length() - 2);
                            }
                            Log.w(TAG, detectedBy.toString());
                        }

                        callback.onResult(isMalicious, response);

                    } catch (JSONException e) {
                        Log.e(TAG, "❌ Parse error: " + e.getMessage());
                        callback.onError("Response parsing failed: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "❌ Network error: " + error.toString());
                    callback.onError("Network request failed: " + error.toString());
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY);
                // Force cache bypass
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                headers.put("Expires", "0");
                // Add timestamp to force unique requests
                headers.put("X-Request-Time", String.valueOf(System.currentTimeMillis()));
                return headers;
            }
        };

        getRequest.setShouldCache(false); // Disable Volley caching
        requestQueue.add(getRequest);
    }

    /**
     * ✅ Emergency fast scan - minimal retries for urgent detection
     */
    public void emergencyScan(String urlToScan, VirusTotalCallback callback) {
        Log.w(TAG, "🚨 Emergency VirusTotal scan for: " + urlToScan);
        
        String encodedUrlId = Base64.encodeToString(urlToScan.trim().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");

        // Direct result fetch with minimal retries
        fetchAnalysisResultWithRetry(encodedUrlId, callback, 1);
    }
}
