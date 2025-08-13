package com.example.maliciousurldetector;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VirusTotalHelper {

    private static final String API_KEY = "2fd34ef2f7b2a76b4cce6b5cd0903378c8ec18ea490c3712d34dbde9e06e121f";
    private static final String TAG = "VirusTotal";

    // ✅ Final Callback Interface - use this everywhere
    public interface ResultCallback {
        void onResult(boolean isMalicious);
        void onError(String error);
    }

    public static void checkUrl(final Context context, final String url, final ResultCallback callback) {
        submitUrl(context, url, new VTSubmitCallback() {
            @Override
            public void onSubmitted(String analysisId) {
                if (analysisId == null) {
                    callback.onError("Submission failed: No analysis ID received");
                    return;
                }

                pollForResult(context, analysisId, callback);
            }
        });
    }

    // Internal callback to handle submission
    private interface VTSubmitCallback {
        void onSubmitted(String analysisId);
    }

    private static void submitUrl(Context context, String url, VTSubmitCallback callback) {
        String submitUrl = "https://www.virustotal.com/api/v3/urls";
        Map<String, String> body = new HashMap<>();
        body.put("url", url);

        JSONObject jsonBody = new JSONObject(body);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, submitUrl, jsonBody,
                response -> {
                    try {
                        String analysisId = response.getJSONObject("data").getString("id");
                        callback.onSubmitted(analysisId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing submission response", e);
                        callback.onSubmitted(null);
                    }
                },
                error -> {
                    Log.e(TAG, "Error submitting URL", error);
                    callback.onSubmitted(null);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-apikey", API_KEY);
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    private static void pollForResult(Context context, String analysisId, ResultCallback callback) {
        String pollUrl = "https://www.virustotal.com/api/v3/analyses/" + analysisId;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, pollUrl, null,
                        response -> {
                            try {
                                JSONObject data = response.getJSONObject("data");
                                String status = data.getJSONObject("attributes").getString("status");
                                if (status.equals("completed")) {
                                    JSONObject stats = data.getJSONObject("attributes").getJSONObject("stats");
                                    int malicious = stats.getInt("malicious");
                                    callback.onResult(malicious > 0);
                                } else {
                                    handler.postDelayed(this, 3000);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing poll response", e);
                                callback.onError("Error parsing poll response");
                            }
                        },
                        error -> {
                            Log.e(TAG, "Error polling result", error);
                            callback.onError("Error polling result");
                        }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("x-apikey", API_KEY);
                        return headers;
                    }
                };

                Volley.newRequestQueue(context).add(request);
            }
        }, 3000);
    }
}
