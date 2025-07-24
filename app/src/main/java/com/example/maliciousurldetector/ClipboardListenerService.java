package com.example.maliciousurldetector;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ClipboardListenerService extends Service {

    private ClipboardManager clipboardManager;

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(onPrimaryClipChangedListener);
        }
    }

    private final ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener = () -> {
        if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
            CharSequence clipText = clipboardManager.getPrimaryClip().getItemAt(0).getText();
            if (!TextUtils.isEmpty(clipText)) {
                String copiedText = clipText.toString();

                if (copiedText.startsWith("http")) {
                    Log.d("ClipboardListener", "URL detected: " + copiedText);

                    // Send broadcast every time even for same link
                    Intent intent = new Intent(getApplicationContext(), UrlReceiver.class);
                    intent.setAction("com.example.maliciousurldetector.CHECK_URL");
                    intent.putExtra("url", copiedText);
                    sendBroadcast(intent);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
        }
        super.onDestroy();
    }
}
