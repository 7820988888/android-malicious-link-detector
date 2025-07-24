package com.example.maliciousurldetector;

import android.content.Context;

import org.json.JSONObject;

public class UrlScanner {

    public interface ScanCallback {
        void onResult(boolean isMalicious, String source);
        void onError(String error);
    }

    public static void scan(Context context, String url, ScanCallback callback) {
        // First check with Google Safe Browsing
        SafeBrowsingHelper.checkUrl(context, url, new SafeBrowsingHelper.ResultCallback() {
            @Override
            public void onResult(boolean isDangerous) {
                if (isDangerous) {
                    callback.onResult(true, "Google Safe Browsing");
                } else {
                    // If not found, fallback to VirusTotal
                    VirusTotalApi vt = new VirusTotalApi(context);
                    vt.scanUrl(url, new VirusTotalApi.VirusTotalCallback() {
                        @Override
                        public void onResult(boolean isMalicious, JSONObject fullResult) {
                            if (isMalicious) {
                                callback.onResult(true, "VirusTotal");
                            } else {
                                callback.onResult(false, "None");
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            callback.onError("VirusTotal Error: " + errorMessage);
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                callback.onError("SafeBrowsing Error: " + error);
            }
        });
    }
}
