package com.example.maliciousurldetector;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

public class VirusTotalHelper {

    public static void checkUrlWithVirusTotal(Context context, String url, SafeBrowsingHelper.ResultCallback callback) {
        VirusTotalApi api = new VirusTotalApi(context);
        api.scanUrl(url, new VirusTotalApi.VirusTotalCallback() {
            @Override
            public void onResult(boolean isMalicious, JSONObject fullResult) {
                if (isMalicious) {
                    SafeBrowsingHelper.triggerAllAlerts(context, url);
                }
                callback.onResult(isMalicious);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("VirusTotalHelper", "Error: " + errorMessage);
                callback.onResult(false);
            }
        });
    }
}
