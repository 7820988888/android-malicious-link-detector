
package com.example.maliciousurldetector;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class NotificationService extends Service {

    private MediaPlayer mediaPlayer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String detectedUrl = intent.getStringExtra("malicious_url");

        // ✅ Show toast alert even if notifications are off
        Toast.makeText(this, "⚠️ Malicious Link Detected: " + detectedUrl, Toast.LENGTH_LONG).show();

        // ✅ Play alarm sound (sarin.mp3 should be in res/raw/)
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.siren); // make sure 'sarin.mp3' is in res/raw
            mediaPlayer.setLooping(false);
        }

        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

        // ✅ Stop the service after playback
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> stopSelf());
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // not a bound service
    }
}
