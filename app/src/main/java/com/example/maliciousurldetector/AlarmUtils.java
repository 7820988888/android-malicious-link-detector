package com.example.maliciousurldetector;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

public class AlarmUtils {

    private static MediaPlayer mediaPlayer;

    /**
     * ✅ This method triggers alarm: plays siren and shows warning dialog.
     */
    public static void triggerAlarm(Context context, String url) {
        showAlarmDialog(context, url);
    }

    /**
     * ✅ This method plays the alarm sound and opens a dialog screen to alert user about a malicious URL.
     */
    public static void showAlarmDialog(Context context, String url) {
        playAlarm(context);

        try {
            Intent intent = new Intent(context, AlarmDialogActivity.class);
            intent.putExtra("malicious_url", url);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("AlarmUtils", "Error showing alarm dialog: " + e.getMessage());
        }
    }

    /**
     * ✅ This method plays a siren sound once. It automatically stops after 5 seconds.
     */
    public static void playAlarm(Context context) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                return;
            }

            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), R.raw.siren);

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false);
                mediaPlayer.start();

                // Automatically stop after 5 seconds
                new Handler().postDelayed(AlarmUtils::stopAlarm, 5000);
            } else {
                Log.e("AlarmUtils", "MediaPlayer is null. Siren resource might be missing.");
            }
        } catch (Exception e) {
            Log.e("AlarmUtils", "Error playing alarm: " + e.getMessage());
        }
    }

    /**
     * ✅ Stop alarm safely and release resources.
     */
    public static void stopAlarm() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e("AlarmUtils", "Error stopping alarm: " + e.getMessage());
        }
    }
}
