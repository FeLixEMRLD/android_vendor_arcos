package com.arcos.setupwizard;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ArcSetupActivity extends Activity implements View.OnClickListener, Runnable {

    private int mCurrentStep = 0;

    private View mCardContainer;
    private View mDot1;
    private View mDot2;
    private View mDot3;
    private TextView mTxtBadge;
    private TextView mTxtTitle;
    private TextView mTxtSubtitle;
    private TextView mTxtFeatureTitle;
    private TextView mTxtFeatureDesc;
    private Button mBtnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } catch (Throwable ignored) {
        }

        // Configure window to show over lockscreen if keyguard ever initializes early
        try {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        } catch (Throwable ignored) {
        }

        setContentView(R.layout.activity_arc_setup);

        mCardContainer = findViewById(R.id.card_container);
        mDot1 = findViewById(R.id.dot_1);
        mDot2 = findViewById(R.id.dot_2);
        mDot3 = findViewById(R.id.dot_3);
        mTxtBadge = findViewById(R.id.txt_badge);
        mTxtTitle = findViewById(R.id.txt_title);
        mTxtSubtitle = findViewById(R.id.txt_subtitle);
        mTxtFeatureTitle = findViewById(R.id.txt_feature_title);
        mTxtFeatureDesc = findViewById(R.id.txt_feature_desc);
        mBtnNext = findViewById(R.id.btn_next);

        if (mBtnNext != null) {
            mBtnNext.setOnClickListener(this);
        }

        applyImmersiveSetupMode();
        updateStepUi(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyImmersiveSetupMode();
    }

    private void applyImmersiveSetupMode() {
        try {
            Window window = getWindow();
            if (window == null) return;

            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }

            View decor = window.getDecorView();
            if (decor != null) {
                // 0x01600000 = STATUS_BAR_DISABLE_EXPAND | STATUS_BAR_DISABLE_HOME | STATUS_BAR_DISABLE_RECENT
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | 0x01600000);
            }
        } catch (Throwable ignored) {
            // Fallback to standard immersive if STATUS_BAR flag is restricted
            try {
                View decor = getWindow().getDecorView();
                if (decor != null) {
                    decor.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            } catch (Throwable ignored2) {
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (mCurrentStep < 2) {
            mCurrentStep++;
            updateStepUi(true);
        } else {
            completeSystemProvisioning();
        }
    }

    private void updateStepUi(boolean animate) {
        if (animate && mCardContainer != null) {
            mCardContainer.animate()
                    .alpha(0f)
                    .translationY(-20f)
                    .setDuration(140)
                    .withEndAction(this)
                    .start();
        } else {
            applyStepContent();
        }
    }

    @Override
    public void run() {
        applyStepContent();
        if (mCardContainer != null) {
            mCardContainer.setTranslationY(20f);
            mCardContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .start();
        }
    }

    private void applyStepContent() {
        updateStepDots(mCurrentStep);

        if (mCurrentStep == 0) {
            if (mTxtBadge != null) mTxtBadge.setText("ARCOS • V0-ALPHA");
            if (mTxtTitle != null) mTxtTitle.setText("Welcome to ArcOS");
            if (mTxtSubtitle != null) {
                mTxtSubtitle.setText("Engineered from the ground up for fluidity, precision scaling, and native hardware harmony.");
            }
            if (mTxtFeatureTitle != null) mTxtFeatureTitle.setText("Native System Architecture");
            if (mTxtFeatureDesc != null) {
                mTxtFeatureDesc.setText("• 374 DP (462 DPI) Precision Display Scaling\n• 120Hz AMOLED Latch Optimization\n• Deep Hardware & Haptic Integration");
            }
            if (mBtnNext != null) mBtnNext.setText("Begin Setup");

        } else if (mCurrentStep == 1) {
            if (mTxtBadge != null) mTxtBadge.setText("INTERFACE & NAVIGATION");
            if (mTxtTitle != null) mTxtTitle.setText("Fluid By Design");
            if (mTxtSubtitle != null) {
                mTxtSubtitle.setText("ArcOS replaces legacy clutter with unified gesture navigation and calibrated visual density.");
            }
            if (mTxtFeatureTitle != null) mTxtFeatureTitle.setText("Active Display Profile");
            if (mTxtFeatureDesc != null) {
                mTxtFeatureDesc.setText("• Target Density: 462 DPI (374 DP)\n• Gesture Navigation: Enabled\n• SurfaceFlinger Refresh Latch: Active");
            }
            if (mBtnNext != null) mBtnNext.setText("Continue");

        } else {
            if (mTxtBadge != null) mTxtBadge.setText("PROVISIONING READY");
            if (mTxtTitle != null) mTxtTitle.setText("You're All Set");
            if (mTxtSubtitle != null) {
                mTxtSubtitle.setText("Your device is configured and ready. Tap below to finalize system provisioning and enter ArcOS.");
            }
            if (mTxtFeatureTitle != null) mTxtFeatureTitle.setText("System Status");
            if (mTxtFeatureDesc != null) {
                mTxtFeatureDesc.setText("• Android 16 Core: Provisioned\n• Hardware HALs: Connected\n• Security & Keyguard: Ready");
            }
            if (mBtnNext != null) mBtnNext.setText("Enter ArcOS");
        }
    }

    private void updateStepDots(int step) {
        setDotWidthAndAlpha(mDot1, step == 0 ? 24 : 8, step == 0 ? 1.0f : 0.35f);
        setDotWidthAndAlpha(mDot2, step == 1 ? 24 : 8, step == 1 ? 1.0f : 0.35f);
        setDotWidthAndAlpha(mDot3, step == 2 ? 24 : 8, step == 2 ? 1.0f : 0.35f);
    }

    private void setDotWidthAndAlpha(View dot, int widthDp, float alpha) {
        if (dot == null) return;
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int widthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, dm);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            if (lp != null) {
                lp.width = widthPx;
                dot.setLayoutParams(lp);
            }
            dot.setAlpha(alpha);
        } catch (Throwable ignored) {
        }
    }

    private void completeSystemProvisioning() {
        ContentResolver cr = getContentResolver();

        // 1. Apply system defaults safely
        try {
            Settings.Secure.putInt(cr, "navigation_mode", 2);
        } catch (Throwable ignored) {
        }

        try {
            Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(cr, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
        } catch (Throwable ignored) {
        }

        // 2. Mark Android 16 Device Provisioned & User Setup Complete (Native OS Organ Bit)
        try {
            Settings.Global.putInt(cr, Settings.Global.DEVICE_PROVISIONED, 1);
        } catch (Throwable ignored) {
        }

        try {
            Settings.Secure.putInt(cr, Settings.Secure.USER_SETUP_COMPLETE, 1);
            Settings.Secure.putInt(cr, "tv_user_setup_complete", 1);
        } catch (Throwable ignored) {
        }

        // 3. Disable ArcSetupActivity component so it never intercepts HOME again
        try {
            PackageManager pm = getPackageManager();
            ComponentName name = new ComponentName(this, ArcSetupActivity.class);
            pm.setComponentEnabledSetting(
                    name,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } catch (Throwable ignored) {
        }

        // 4. Transition cleanly to the default Home Launcher
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(homeIntent);
        } catch (Throwable ignored) {
        }

        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        if (mCurrentStep > 0) {
            mCurrentStep--;
            updateStepUi(true);
        }
        // Block back press on step 0 during provisioning
    }
}
