// unchanged package and imports
package com.example.maliciousurldetector;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;
    private static final int RC_SIGN_IN = 100;

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private EditText urlInput;
    private Button btnCheckUrl, btnCheckApp, btnNotificationAccess;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        urlInput = findViewById(R.id.urlInput);
        btnCheckUrl = findViewById(R.id.btnCheckUrl);
        btnCheckApp = findViewById(R.id.btnCheckApp);
        btnNotificationAccess = findViewById(R.id.btnNotificationAccess);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);
        updateUserInfoInDrawer();

        btnCheckUrl.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                scanUrl(url);
            } else {
                Toast.makeText(this, "\u26a0\ufe0f Please enter a URL!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCheckApp.setOnClickListener(v -> checkInstalledApps());
        btnNotificationAccess.setOnClickListener(v -> requestNotificationAccess());

        checkNotificationPermission();
        handleClipboardOnLaunch();
        handleIncomingIntent();
    }

    private void scanUrl(String url) {
        Toast.makeText(this, "\ud83d\udd0d Scanning...", Toast.LENGTH_SHORT).show();
        boolean isApk = url.toLowerCase().endsWith(".apk");

        UrlScanner.scan(this, url, new UrlScanner.ScanCallback() {
            @Override
            public void onResult(boolean isMalicious, String source) {
                String reason;
                if (isMalicious && isApk) {
                    reason = "Malicious APK Detected";
                } else if (isMalicious) {
                    reason = "Malicious URL Detected by " + source;
                } else if (isApk) {
                    reason = "APK File - Use caution";
                } else {
                    reason = "URL is Safe";
                }

                if (isMalicious || isApk) {
                    Toast.makeText(MainActivity.this, "\u274c " + reason, Toast.LENGTH_LONG).show();
                    NotificationHelper.showSecurityAlert(MainActivity.this, url, reason);
                    showAlarmDialog(url, reason);
                } else {
                    Toast.makeText(MainActivity.this, "\u2705 URL is safe", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "\u274c Scan Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlarmDialog(String url, String reason) {
        new AlertDialog.Builder(this)
                .setTitle("\u26a0\ufe0f Alert")
                .setMessage(reason + "\n" + url)
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("Ignore", null)
                .setNegativeButton("Open Anyway", (d, w) -> showBrowserChoiceDialog(url))
                .show();

        try {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.siren);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception ignored) {}

        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vib != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(1000);
            }
        }
    }

    private void showBrowserChoiceDialog(String url) {
        String[] browserNames = {
                "\uD83C\uDF10 Chrome",
                "\uD83E\uDD81 Brave",
                "\uD83E\uDD8A Firefox",
                "\uD83C\uDF0A Edge",
                "\uD83C\uDFAD Opera",
                "\uD83E\uDD86 DuckDuckGo"
        };
        String[] browserPackages = {
                "com.android.chrome",
                "com.brave.browser",
                "org.mozilla.firefox",
                "com.microsoft.emmx",
                "com.opera.browser",
                "com.duckduckgo.mobile.android"
        };

        new AlertDialog.Builder(this)
                .setTitle("Open in Secure Browser?")
                .setItems(browserNames, (dialog, which) -> {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.setPackage(browserPackages[which]);
                        startActivity(i);
                    } catch (Exception e) {
                        Toast.makeText(this, "Browser not installed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleClipboardOnLaunch() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData cd = cm.getPrimaryClip();
            if (cd != null && cd.getItemCount() > 0) {
                CharSequence clipText = cd.getItemAt(0).getText();
                if (clipText != null && clipText.toString().startsWith("http")) {
                    scanUrl(clipText.toString());
                }
            }
        }
    }

    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String url = null;

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if ("text/plain".equals(intent.getType())) {
                url = intent.getStringExtra(Intent.EXTRA_TEXT);
            }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) url = data.toString();
        }

        if (url != null && url.startsWith("http")) {
            scanUrl(url.trim());
        }
    }

    private boolean handleNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_subscription) startActivity(new Intent(this, SubscriptionActivity.class));
        else if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
        else if (id == R.id.nav_settings) startActivity(new Intent(this, SettingsActivity.class));
        else if (id == R.id.nav_privacy) startActivity(new Intent(this, PrivacyPolicyActivity.class));
        else if (id == R.id.nav_logout) {
            Toast.makeText(this, "\uD83D\uDC4B Logging out...", Toast.LENGTH_SHORT).show();
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        drawerLayout.closeDrawers();
        return true;
    }

    private void updateUserInfoInDrawer() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            View headerView = navView.getHeaderView(0);
            ((TextView) headerView.findViewById(R.id.userNameTextView)).setText(user.getDisplayName());
            ((TextView) headerView.findViewById(R.id.userEmailTextView)).setText(user.getEmail());
        }
    }

    private void checkInstalledApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        StringBuilder detected = new StringBuilder();

        for (ApplicationInfo app : apps) {
            String pkg = app.packageName.toLowerCase();
            if (pkg.contains("malware") || pkg.contains("spyware") || pkg.contains("trojan")) {
                detected.append("\u26a0\ufe0f ").append(pkg).append("\n");
            }
        }

        if (detected.length() > 0) {
            Toast.makeText(this, "Malicious Apps Found:\n" + detected, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "\u2705 No Malicious Apps Detected", Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationAccess() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "\u2705 Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "\u274c Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Toast.makeText(this, "\u2705 Signed in as: " + account.getEmail(), Toast.LENGTH_LONG).show();
            } catch (ApiException e) {
                Toast.makeText(this, "\u274c Sign-in failed", Toast.LENGTH_LONG).show();
            }
        }
    }
}
