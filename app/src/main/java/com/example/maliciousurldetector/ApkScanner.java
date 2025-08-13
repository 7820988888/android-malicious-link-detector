package com.example.maliciousurldetector;

import android.content.Context;

import org.json.JSONObject;

import java.io.File;

public class ApkScanner {

    public interface ScanCallback {
        void onResult(boolean isMalicious, String source);
        void onError(String error);
    }

    public static void scan(Context context, File apkFile, ScanCallback callback) {
        VirusTotalApi vt = new VirusTotalApi(context);
        vt.scanApk(apkFile, new VirusTotalApi.VirusTotalCallback() {
            @Override
            public void onResult(boolean isMalicious, JSONObject result) {
                if (isMalicious) {
                    callback.onResult(true, "VirusTotal");
                } else {
                    callback.onResult(false, "VirusTotal");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError("APK Scan Error: " + errorMessage);
            }
        });
    }
}
