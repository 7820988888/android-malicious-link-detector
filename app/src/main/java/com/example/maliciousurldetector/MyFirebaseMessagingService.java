package com.example.maliciousurldetector;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        SharedPreferences preferences = getSharedPreferences("AppSettingsPrefs", MODE_PRIVATE);
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
        boolean soundEnabled = preferences.getBoolean("notification_sound", true);

        String title = "⚠️ Malicious URL Alert";
        String message = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getBody() : "A dangerous link was detected!";

        if (notificationsEnabled) {
            sendNotification(title, message);
        } else if (soundEnabled) {
            AlarmUtils.playAlarm(this);  // Local siren alert
        }
    }

    private void sendNotification(String title, String messageBody) {
        String channelId = "MaliciousURL_Channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Required for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Malicious URL Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
