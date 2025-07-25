package com.example.maliciousurldetector;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * ✅ StartupService ensures all real-time protection services are running
 * This service starts automatically when the app launches and ensures
 * clipboard monitoring and other protection services are active
 */
public class StartupService extends Service {

    private static final String TAG = "StartupService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "🚀 StartupService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "🔧 Initializing real-time protection services...");
        
        initializeRealTimeProtection();
        
        // Stop self after initialization
        stopSelf();
        
        return START_NOT_STICKY;
    }

    /**
     * ✅ Initialize all real-time protection components
     */
    private void initializeRealTimeProtection() {
        SharedPreferences prefs = getSharedPreferences("AppSettingsPrefs", MODE_PRIVATE);
        boolean realTimeEnabled = prefs.getBoolean("realtime_detection", true);
        
        if (realTimeEnabled) {
            Log.i(TAG, "✅ Real-time detection enabled, starting services...");
            
            // Start clipboard monitoring service
            startClipboardListener();
            
            // Clear any old cached data to ensure fresh scans
            clearOldCaches();
            
            Log.i(TAG, "✅ Real-time protection services initialized");
        } else {
            Log.w(TAG, "⚠️ Real-time detection disabled in settings");
        }
    }

    /**
     * ✅ Start clipboard listener service
     */
    private void startClipboardListener() {
        try {
            Intent clipboardIntent = new Intent(this, ClipboardListenerService.class);
            startService(clipboardIntent);
            Log.d(TAG, "📋 Clipboard listener service started");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start clipboard listener: " + e.getMessage());
        }
    }

    /**
     * ✅ Clear old caches to ensure fresh real-time detection
     */
    private void clearOldCaches() {
        try {
            // Clear scanned URL store for fresh detection
            ScannedUrlStore.clear();
            Log.d(TAG, "🧹 Cleared old scan caches");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error clearing caches: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "🛑 StartupService destroyed");
        super.onDestroy();
    }
}