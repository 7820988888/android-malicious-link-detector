package com.example.maliciousurldetector;

import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class ClipboardListenerService extends Service {

    private static final String TAG = "ClipboardListener";
    private ClipboardManager clipboardManager;
    private String lastProcessedUrl = "";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🎯 Clipboard listener service started");
        
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(onPrimaryClipChangedListener);
            Log.d(TAG, "✅ Clipboard listener registered");
        } else {
            Log.e(TAG, "❌ Failed to get ClipboardManager");
        }
    }

    private final ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener = () -> {
        try {
            if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                CharSequence clipText = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                
                if (!TextUtils.isEmpty(clipText)) {
                    String copiedText = clipText.toString().trim();
                    
                    // Check if it's a URL
                    if (isValidUrl(copiedText)) {
                        Log.i(TAG, "🔗 URL detected in clipboard: " + copiedText);
                        
                        // ✅ ALWAYS process - removed duplicate check for real-time protection
                        // Real-time protection should scan every URL copy, even if it's the same URL
                        processUrlFromClipboard(copiedText);
                        
                    } else {
                        Log.d(TAG, "📄 Non-URL text copied: " + copiedText.substring(0, Math.min(50, copiedText.length())));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing clipboard change: " + e.getMessage());
        }
    };

    /**
     * ✅ Enhanced URL validation
     */
    private boolean isValidUrl(String text) {
        if (TextUtils.isEmpty(text)) return false;
        
        text = text.trim().toLowerCase();
        
        // Check for common URL patterns
        return text.startsWith("http://") || 
               text.startsWith("https://") || 
               text.startsWith("www.") ||
               (text.contains(".") && (text.contains(".com") || text.contains(".org") || 
                text.contains(".net") || text.contains(".edu") || text.contains(".gov")));
    }

    /**
     * ✅ Process URL from clipboard with real-time detection
     */
    private void processUrlFromClipboard(String url) {
        // Normalize URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.startsWith("www.")) {
                url = "https://" + url;
            } else if (url.contains(".")) {
                url = "https://" + url;
            }
        }

        Log.w(TAG, "🚨 Processing URL for real-time scan: " + url);

        // Send broadcast for immediate processing
        Intent intent = new Intent(getApplicationContext(), UrlReceiver.class);
        intent.setAction("com.example.maliciousurldetector.CHECK_URL");
        intent.putExtra("url", url);
        intent.putExtra("source", "clipboard");
        intent.putExtra("timestamp", System.currentTimeMillis());
        
        try {
            sendBroadcast(intent);
            Log.d(TAG, "📡 Broadcast sent for URL: " + url);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to send broadcast: " + e.getMessage());
            
            // Fallback: directly start service
            fallbackDirectScan(url);
        }
    }

    /**
     * ✅ Fallback method if broadcast fails
     */
    private void fallbackDirectScan(String url) {
        try {
            Intent serviceIntent = new Intent(getApplicationContext(), URLCheckService.class);
            serviceIntent.putExtra("url", url);
            serviceIntent.putExtra("source", "clipboard_fallback");
            serviceIntent.putExtra("priority", "real_time");
            serviceIntent.putExtra("timestamp", System.currentTimeMillis());
            
            startService(serviceIntent);
            Log.d(TAG, "🔄 Fallback direct scan initiated for: " + url);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Fallback scan also failed: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "🛑 Clipboard listener service stopping");
        
        if (clipboardManager != null) {
            try {
                clipboardManager.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
                Log.d(TAG, "✅ Clipboard listener unregistered");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error unregistering clipboard listener: " + e.getMessage());
            }
        }
        
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "🔄 Clipboard service restarted");
        return START_STICKY; // Keep service running
    }
}
