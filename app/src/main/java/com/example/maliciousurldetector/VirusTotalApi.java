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

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VirusTotalApi {

    private static final String TAG = "VirusTotalApi";
    private static final String URL_SCAN_BASE = "https://www.virustotal.com/api/v3/urls";
    private static final String FILE_UPLOAD_URL = "https://www.virustotal.com/api/v3/files";
    private static final String FILE_ANALYSIS_URL = "https://www.virustotal.com/api/v3/analyses/";

    private static final int MAX_RETRIES = 7;
    private static final long RETRY_DELAY_MS = 5000;

    private final Context context;
    private final com.android.volley.RequestQueue requestQueue;

    public interface VirusTotalCallback {
        void onResult(boolean isMalicious, JSONObject result);
        void onError(String errorMessage);
    }

    public VirusTotalApi(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(this.context);
    }

    // ------------------------------------
    // ✅ 1. Scan URL logic (unchanged)
    // ------------------------------------
    public void scanUrl(String urlToScan, VirusTotalCallback callback) {
        String encodedUrlId = Base64.encodeToString(urlToScan.trim().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP)
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");

        fetchUrlAnalysis(encodedUrlId, callback, MAX_RETRIES);
    }

    private void fetchUrlAnalysis(String encodedId, VirusTotalCallback callback, int retriesLeft) {
        String finalUrl = URL_SCAN_BASE + "/" + encodedId;

        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.GET,
                finalUrl,
                null,
                response -> {
                    try {
                        JSONObject attributes = response.getJSONObject("data").getJSONObject("attributes");
                        JSONObject stats = attributes.getJSONObject("last_analysis_stats");
                        int malicious = stats.optInt("malicious", 0);
                        int suspicious = stats.optInt("suspicious", 0);
                        boolean isMalicious = (malicious > 0 || suspicious > 0);
                        callback.onResult(isMalicious, response);
                    } catch (JSONException e) {
                        callback.onError("❌ JSON error: " + e.getMessage());
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

    // ------------------------------------
    // ✅ 2. Scan APK File logic
    // ------------------------------------
    public void scanApk(File apkFile, VirusTotalCallback callback) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(FILE_UPLOAD_URL).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY);
                connection.setDoOutput(true);

                String boundary = "----VirusTotalBoundary" + System.currentTimeMillis();
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream os = connection.getOutputStream();
                os.write(("--" + boundary + "\r\n").getBytes());
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + apkFile.getName() + "\"\r\n").getBytes());
                os.write(("Content-Type: application/vnd.android.package-archive\r\n\r\n").getBytes());

                FileInputStream fis = new FileInputStream(apkFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();

                os.write(("\r\n--" + boundary + "--\r\n").getBytes());
                os.flush();
                os.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    String response = new java.util.Scanner(connection.getInputStream()).useDelimiter("\\A").next();
                    JSONObject json = new JSONObject(response);
                    String analysisId = json.getJSONObject("data").getString("id");

                    // Poll result
                    pollFileAnalysis(analysisId, callback, MAX_RETRIES);
                } else {
                    callback.onError("❌ Upload failed. Code: " + responseCode);
                }

            } catch (Exception e) {
                callback.onError("❌ Upload error: " + e.getMessage());
            }
        }).start();
    }

    private void pollFileAnalysis(String analysisId, VirusTotalCallback callback, int retriesLeft) {
        if (retriesLeft <= 0) {
            callback.onError("❌ Analysis timeout");
            return;
        }

        String analysisUrl = FILE_ANALYSIS_URL + analysisId;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                analysisUrl,
                null,
                response -> {
                    try {
                        JSONObject stats = response.getJSONObject("data").getJSONObject("attributes").getJSONObject("stats");
                        int malicious = stats.optInt("malicious", 0);
                        int suspicious = stats.optInt("suspicious", 0);
                        boolean isMalicious = (malicious > 0 || suspicious > 0);
                        callback.onResult(isMalicious, response);
                    } catch (JSONException e) {
                        callback.onError("❌ Parsing error: " + e.getMessage());
                    }
                },
                error -> {
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            pollFileAnalysis(analysisId, callback, retriesLeft - 1), RETRY_DELAY_MS);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-apikey", BuildConfig.VIRUSTOTAL_API_KEY);
                return headers;
            }
        };

        requestQueue.add(request);
    }
}
