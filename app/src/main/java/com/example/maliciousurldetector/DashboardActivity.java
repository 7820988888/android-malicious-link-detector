package com.example.maliciousurldetector;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        // 🔐 Firebase + Google Sign-In setup
        mAuth = FirebaseAuth.getInstance();
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);

        // 🧭 Toolbar setup
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 🧭 Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navView.setNavigationItemSelectedListener(this::handleNavigationItemSelected);

        // 👤 Show user info in navigation drawer
        updateUserInfoInDrawer();

        // 🧩 Dashboard Card Clicks
        findViewById(R.id.scanUrlCard).setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.appSafetyCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("action", "scan_apps");
            startActivity(intent);
        });

        findViewById(R.id.subscriptionCard).setOnClickListener(v ->
                startActivity(new Intent(this, SubscriptionActivity.class)));

        // ✅ Safe Website Card click handler added
        findViewById(R.id.safeWebsiteCard).setOnClickListener(v ->
                startActivity(new Intent(this, SafeWebsiteActivity.class)));
    }

    // 👤 Update Drawer Header with User Info
    private void updateUserInfoInDrawer() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            View headerView = navView.getHeaderView(0);
            TextView userName = headerView.findViewById(R.id.userNameTextView);
            TextView userEmail = headerView.findViewById(R.id.userEmailTextView);
            ImageView profileImage = headerView.findViewById(R.id.profileImageView);

            userName.setText(user.getDisplayName() != null ? user.getDisplayName() : "User");
            userEmail.setText(user.getEmail());

            // Optional: Load profile image if available
            // Glide.with(this).load(user.getPhotoUrl()).into(profileImage);
        }
    }

    // ☰ Navigation Drawer Clicks
    private boolean handleNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_subscription) {
            startActivity(new Intent(this, SubscriptionActivity.class));
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_privacy) {
            startActivity(new Intent(this, PrivacyPolicyActivity.class));
        } else if (id == R.id.nav_logout) {
            Toast.makeText(this, "🚪 Logging out...", Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut();
            }
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    // 🔙 Handle back button for drawer
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
