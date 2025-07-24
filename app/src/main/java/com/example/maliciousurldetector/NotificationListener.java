package com.example.maliciousurldetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";
    private static final long COOLDOWN_PERIOD = 10000; // 10 seconds
    private static long lastDetectionTime = 0;
    private final HashSet<String> processedUrls = new HashSet<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Bundle extras = sbn.getNotification().extras;
        CharSequence charSequence = extras.getCharSequence("android.text");

        String notificationText = null;
        if (charSequence instanceof Spannable || charSequence instanceof SpannableString) {
            notificationText = charSequence.toString(); // Safe conversion
        } else if (charSequence != null) {
            notificationText = String.valueOf(charSequence); // Fallback
        }

        if (notificationText != null) {
            Log.d(TAG, "Notification: " + notificationText);
            String extractedUrl = extractUrl(notificationText);

            if (extractedUrl != null && !processedUrls.contains(extractedUrl)) {
                Log.d(TAG, "Extracted URL: " + extractedUrl);
                processedUrls.add(extractedUrl);
                checkMaliciousUrl(extractedUrl);
            }
        }
    }

    private String extractUrl(String text) {
        Pattern urlPattern = Pattern.compile(
                "(https?:\\/\\/|www\\.)[a-zA-Z0-9\\-\\.]+\\.[a-z]{2,6}(:\\d{1,5})?(\\/[^\\s]*)?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = urlPattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private void checkMaliciousUrl(String url) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastDetectionTime < COOLDOWN_PERIOD) {
            Log.d(TAG, "Cooldown active. Skipping detection.");
            return;
        }

        lastDetectionTime = currentTime;

        // ✅ Use anonymous class instead of lambda for ResultCallback
        SafeBrowsingHelper.checkUrl(getApplicationContext(), url, new SafeBrowsingHelper.ResultCallback() {
            @Override
            public void onResult(boolean isDangerous) {
                if (isDangerous) {
                    sendNotification(url);
                    showToast("❌ Malicious URL Detected:\n" + url);
                } else {
                    showToast("✅ Safe URL:\n" + url);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "SafeBrowsing error: " + error);
                showToast("Error checking URL: " + error);
            }
        });
    }

    private void sendNotification(String url) {
        String CHANNEL_ID = "malicious_url_alert";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Malicious URL Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when a malicious URL is detected");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        Notification.Builder builder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        Notification notification = builder
                .setContentTitle("⚠️ Malicious URL Detected!")
                .setContentText(url)
                .setStyle(new Notification.BigTextStyle().bigText(url))
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setAutoCancel(true)
                .build();

        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    private void showToast(final String message) {
        new Handler(getMainLooper()).post(() ->
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show()
        );
    }
}
