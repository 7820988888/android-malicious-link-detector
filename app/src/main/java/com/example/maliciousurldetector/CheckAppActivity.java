package com.example.maliciousurldetector;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class CheckAppActivity extends AppCompatActivity {

    private TextView resultTextView; // Declare TextView variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_app); // Link XML layout

        // Find TextView after setting content view
        resultTextView = findViewById(R.id.resultTextView);

        // Run the app scanning function
        checkInstalledApps();
    }

    private void checkInstalledApps() {
        StringBuilder result = new StringBuilder();
        PackageManager pm = getPackageManager();
        List<PackageInfo> installedApps = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : installedApps) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;

            // Ignore system apps
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            String appName = appInfo.loadLabel(pm).toString();
            String packageName = packageInfo.packageName;

            // Flag apps with suspicious names
            if (packageName.contains("spy") || packageName.contains("hack") || packageName.contains("malware")) {
                result.append("⚠️ Suspicious App: ").append(appName).append("\n");
            } else {
                result.append("✅ Safe App: ").append(appName).append("\n");
            }
        }

        // If no suspicious apps found
        if (result.length() == 0) {
            result.append("No suspicious apps detected.");
        }

        // Display the result in TextView
        resultTextView.setText(result.toString());
    }
}
