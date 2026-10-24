package com.arcos.setupwizard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArcSetupActivity extends Activity implements View.OnClickListener, Runnable {

    private int currentStep = 0;
    private LinearLayout contentContainer;
    private LinearLayout cardFeatures;
    private TextView tvBadge;
    private TextView tvTitle;
    private TextView tvSubtitle;
    private Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_arc_setup);

        contentContainer = findViewById(R.id.content_container);
        cardFeatures = findViewById(R.id.card_features);
        tvBadge = findViewById(R.id.tv_badge);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnNext = findViewById(R.id.btn_next);

        contentContainer.setAlpha(0f);
        contentContainer.setTranslationY(60f);
        contentContainer.animate().alpha(1f).translationY(0f).setDuration(700).start();

        btnNext.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        currentStep++;
        if (currentStep <= 2) {
            contentContainer.animate()
                    .alpha(0f)
                    .translationY(-40f)
                    .setDuration(220)
                    .withEndAction(this)
                    .start();
        } else {
            finishSetup();
        }
    }

    @Override
    public void run() {
        if (currentStep == 1) {
            tvBadge.setText("SYSTEM TUNING");
            tvTitle.setText("Tailored for\nEmerald");
            tvTitle.setTextSize(40f);
            tvSubtitle.setText("ArcOS automatically optimizes your display density, refresh rate, and haptics.");
            cardFeatures.setVisibility(View.VISIBLE);
            btnNext.setText("Continue");
        } else if (currentStep == 2) {
            tvBadge.setText("READY");
            tvTitle.setText("Welcome to\nArcOS");
            tvTitle.setTextSize(44f);
            tvSubtitle.setText("Swipe up from the bottom edge at any time to return home.");
            cardFeatures.setVisibility(View.GONE);
            btnNext.setText("Enter ArcOS");
        }

        contentContainer.setTranslationY(40f);
        contentContainer.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .start();
    }

    private void finishSetup() {
        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(getContentResolver(), "user_setup_complete", 1);

            Settings.Global.putFloat(getContentResolver(), Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
            Settings.Global.putFloat(getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f);
            Settings.Global.putFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f);
            Settings.System.putFloat(getContentResolver(), "peak_refresh_rate", 120.0f);
            Settings.System.putFloat(getContentResolver(), "min_refresh_rate", 60.0f);
        } catch (Exception ignored) {
        }

        PackageManager pm = getPackageManager();
        ComponentName name = new ComponentName(this, ArcSetupActivity.class);
        pm.setComponentEnabledSetting(
                name,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Block back button during initial setup
    }
}
