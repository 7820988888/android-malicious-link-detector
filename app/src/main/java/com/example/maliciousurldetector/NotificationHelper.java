package com.example.maliciousurldetector;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID_SECURITY = "MALICIOUS_URL_ALERT";
    private static final String CHANNEL_ID_SCAN = "URL_SCAN_STATUS";
    private static final int NOTIFICATION_ID_SECURITY = 1001;
    private static final int NOTIFICATION_ID_SCAN_ERROR = 1002;
    private static final int NOTIFICATION_ID_SAFE = 1003;

    /**
     * ✅ Main security alert method - enhanced for real-time detection
     */
    public static void showSecurityAlert(Context context, String url, String sender) {
        Log.w(TAG, "🚨 Showing security alert for: " + url + " (detected by: " + sender + ")");
        
        createNotificationChannels(context);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (notificationManager.areNotificationsEnabled()) {
            sendSecurityNotification(context, url, sender);
        } else {
            Log.w(TAG, "⚠️ Notifications disabled, using fallback alerts");
            playAlarm(context);
            showToastOnMainThread(context, "⚠️ SECURITY ALERT: Malicious URL detected!\nEnable notifications for better protection!");
        }
    }

    /**
     * ✅ Enhanced security notification with better formatting
     */
    private static void sendSecurityNotification(Context context, String url, String sender) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("malicious_url", url);
        intent.putExtra("detection_source", sender);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );

        String shortUrl = url.length() > 50 ? url.substring(0, 47) + "..." : url;
        String title = "🚨 SECURITY THREAT DETECTED";
        String message = "Source: " + sender + "\nURL: " + shortUrl;
        String expandedMessage = "⚠️ A malicious URL has been detected!\n\n" +
                "Detection Source: " + sender + "\n" +
                "Malicious URL: " + url + "\n\n" +
                "This URL has been blocked for your protection. " +
                "Do not visit this URL.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SECURITY)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedMessage))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColorized(true)
                .setColor(0xFFFF0000) // Red color for danger
                .setVibrate(new long[]{0, 500, 250, 500}); // Vibration pattern

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SECURITY, builder.build());
        
        // Play alarm sound
        playAlarm(context);
    }

    /**
     * ✅ Show scan error notification
     */
    public static void showScanError(Context context, String url, String error) {
        Log.e(TAG, "❌ Showing scan error for: " + url + " - Error: " + error);
        
        createNotificationChannels(context);

        String shortUrl = url.length() > 40 ? url.substring(0, 37) + "..." : url;
        String title = "⚠️ URL Scan Failed";
        String message = "Could not verify: " + shortUrl;
        String expandedMessage = "Failed to scan URL for threats:\n\n" +
                "URL: " + url + "\n" +
                "Error: " + error + "\n\n" +
                "Please manually verify this URL before visiting.";

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("scan_error_url", url);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SCAN)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(expandedMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(0xFFFF9800); // Orange color for warning

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SCAN_ERROR, builder.build());
    }

    /**
     * ✅ Show safe URL notification (optional, can be disabled in settings)
     */
    public static void showSafeNotification(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
        boolean showSafeNotifications = prefs.getBoolean("show_safe_notifications", false);
        
        if (!showSafeNotifications) {
            Log.d(TAG, "✅ Safe notifications disabled, skipping for: " + url);
            return;
        }

        Log.d(TAG, "✅ Showing safe notification for: " + url);
        
        createNotificationChannels(context);

        String shortUrl = url.length() > 50 ? url.substring(0, 47) + "..." : url;
        String title = "✅ URL Verified Safe";
        String message = "Safe: " + shortUrl;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_SCAN)
                .setSmallIcon(R.drawable.ic_safe)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setColor(0xFF4CAF50); // Green color for safe

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SAFE, builder.build());
    }

    /**
     * ✅ Enhanced toast display
     */
    private static void showToastOnMainThread(Context context, String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        );
    }

    /**
     * ✅ Enhanced alarm with settings check
     */
    private static void playAlarm(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE);
            boolean soundEnabled = prefs.getBoolean("notification_sound", true);
            
            if (!soundEnabled) {
                Log.d(TAG, "🔕 Alarm sound disabled in settings");
                return;
            }
            
            MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.siren);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
                Log.d(TAG, "🔊 Alarm sound played");
            } else {
                Log.e(TAG, "❌ Could not create MediaPlayer for alarm");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error playing alarm: " + e.getMessage());
        }
    }

    /**
     * ✅ Create all notification channels
     */
    private static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                
                // Security alert channel (high priority)
                NotificationChannel securityChannel = new NotificationChannel(
                        CHANNEL_ID_SECURITY,
                        "Security Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                securityChannel.setDescription("Critical security alerts for malicious URLs");
                securityChannel.enableVibration(true);
                securityChannel.enableLights(true);
                securityChannel.setLightColor(0xFFFF0000); // Red light
                notificationManager.createNotificationChannel(securityChannel);
                
                // Scan status channel (normal priority)
                NotificationChannel scanChannel = new NotificationChannel(
                        CHANNEL_ID_SCAN,
                        "Scan Status",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                scanChannel.setDescription("URL scan results and status updates");
                notificationManager.createNotificationChannel(scanChannel);
                
                Log.d(TAG, "✅ Notification channels created");
            }
        }
    }

    /**
     * ✅ Legacy method - maintained for compatibility
     */
    @Deprecated
    private static void createNotificationChannel(Context context) {
        createNotificationChannels(context);
    }
}
