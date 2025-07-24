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
    private static final int MAX_RETRIES = 7;
    private static final long RETRY_DELAY_MS = 4000;

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

    public void scanUrl(String urlToScan, VirusTotalCallback callback) {
        String encodedUrlId = Base64.encodeToString(urlToScan.trim().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");

        fetchAnalysisResultWithRetry(encodedUrlId, callback, MAX_RETRIES);
    }

    private void fetchAnalysisResultWithRetry(String encodedId, VirusTotalCallback callback, int retriesLeft) {
        String finalUrl = BASE_URL + "/" + encodedId;

        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET,
                finalUrl,
                null,
                response -> {
                    try {
                        JSONObject data = response.getJSONObject("data");
                        JSONObject attributes = data.getJSONObject("attributes");

                        if (!attributes.has("last_analysis_results") && retriesLeft > 0) {
                            new Handler(Looper.getMainLooper()).postDelayed(() ->
                                    fetchAnalysisResultWithRetry(encodedId, callback, retriesLeft - 1), RETRY_DELAY_MS);
                            return;
                        }

                        JSONObject stats = attributes.getJSONObject("last_analysis_stats");
                        int malicious = stats.optInt("malicious", 0);
                        int suspicious = stats.optInt("suspicious", 0);
                        boolean isMalicious = (malicious > 0 || suspicious > 0);

                        if (isMalicious) {
                            JSONObject results = attributes.getJSONObject("last_analysis_results");
                            Iterator<String> engines = results.keys();
                            StringBuilder detectedBy = new StringBuilder("Detected by: ");
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
                        callback.onError("❌ Parse error: " + e.getMessage());
                    }
                },
                error -> callback.onError("❌ GET error: " + error.toString())
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY);
                return headers;
            }
        };

        requestQueue.add(getRequest);
    }
}
