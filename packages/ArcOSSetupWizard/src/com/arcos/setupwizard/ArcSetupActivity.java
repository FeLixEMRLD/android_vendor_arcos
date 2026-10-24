package com.arcos.setupwizard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArcSetupActivity extends Activity implements View.OnClickListener, Runnable {

    private static final int REQ_SET_LOCK = 101;
    private static final int STATUS_BAR_DISABLE_MASK = 0x00010000 | 0x00200000 | 0x01000000 | 0x00040000;

    // Steps:
    // 1 = Welcome
    // 2 = Wi-Fi / Network
    // 3 = Google Setup & Permissions
    // 4 = Password / Screen Lock
    // 5 = Navigation Mode
    // 6 = Final Page
    private int currentStep = 1;

    private FrameLayout rootContainer;
    private LinearLayout screenDark;
    private LinearLayout screenLight;

    // Dark screen views (Step 1 & Step 6)
    private TextView darkTitleLine1;
    private TextView darkTitleLine2;
    private TextView darkFinalSummary;
    private FrameLayout btnDarkNext;
    private LinearLayout welcomeBottomLinks;
    private TextView btnLanguage;
    private TextView btnAccessibility;
    private TextView btnEmergency;
    private TextView btnFinalBack;

    // Light screen views (Steps 2, 3, 4, 5)
    private FrameLayout btnLightBack;
    private FrameLayout btnLightNext;
    private TextView lightTitleLine1;
    private TextView lightTitleLine2;
    private TextView lightSubtitle;

    private ScrollView panelStepWifi;
    private LinearLayout panelStepGoogle;
    private LinearLayout panelStepPassword;
    private LinearLayout panelStepNav;

    private TextView btnLightBottomPrimary;
    private TextView btnLightBottomSecondary;
    private TextView btnLightBottomTertiary;

    // Wi-Fi rows
    private LinearLayout[] wifiRows = new LinearLayout[6];
    private TextView[] wifiSsids = new TextView[6];
    private TextView[] wifiStatuses = new TextView[6];
    private View[] wifiDivs = new View[5];

    private List<String> scannedSsids = new ArrayList<String>();
    private List<Boolean> scannedSecured = new ArrayList<Boolean>();
    private boolean showAllWifi = false;
    private String selectedWifiSsid = "";

    // Google Setup views & states
    private LinearLayout rowGoogleAccount;
    private TextView txtGoogleAccountTitle;
    private TextView txtGoogleAccountSub;
    private TextView statusGoogleAccount;
    private View divGoogleAccount;
    private LinearLayout rowGoogleLocation;
    private TextView toggleLocation;
    private LinearLayout rowGoogleScanning;
    private TextView toggleScanning;
    private LinearLayout rowGoogleBackup;
    private TextView toggleBackup;

    private boolean offlineSkipGoogleAccount = false;
    private boolean optLocationEnabled = true;
    private boolean optScanningEnabled = true;
    private boolean optBackupEnabled = true;

    // Password views
    private LinearLayout rowLockPin;
    private LinearLayout rowLockPassword;
    private LinearLayout rowLockPattern;

    // Navigation views
    private LinearLayout rowNavGesture;
    private LinearLayout rowNavThreeButton;
    private TextView radioNavGesture;
    private TextView radioNavThreeButton;
    private boolean useGestureNav = true;

    // Inline Modals
    private FrameLayout modalOverlay;
    private LinearLayout modalWifiPassword;
    private TextView modalWifiTitle;
    private EditText inputWifiPassword;
    private TextView btnWifiCancel;
    private TextView btnWifiConnect;

    private LinearLayout modalLanguagePicker;
    private TextView langOptEn;
    private TextView langOptFil;
    private TextView langOptEs;
    private TextView langOptJa;
    private TextView langOptZh;
    private TextView btnLangClose;

    private WifiManager wifiManager;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isAlreadyProvisioned()) {
            disableSelfAndLaunchHome();
            return;
        }

        configureEdgeToEdgeWindow();
        setContentView(R.layout.activity_arc_setup);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        // Enable location services so Wi-Fi scanning works immediately without popups
        try {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 3);
        } catch (Throwable ignored) {
        }

        bindViews();
        setStatusBarLocked(true);
        renderStep();
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureEdgeToEdgeWindow();
        if (currentStep == 2) {
            triggerWifiScan();
        } else if (currentStep == 3 || currentStep == 4) {
            renderStep();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            configureEdgeToEdgeWindow();
        }
    }

    private void configureEdgeToEdgeWindow() {
        try {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);

            WindowManager.LayoutParams lp = window.getAttributes();
            // LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS = 3 (eliminates top black cutout bar)
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(lp);

            View decor = window.getDecorView();
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } catch (Throwable ignored) {
        }
    }

    private void bindViews() {
        rootContainer = (FrameLayout) findViewById(R.id.root_container);
        screenDark = (LinearLayout) findViewById(R.id.screen_dark);
        screenLight = (LinearLayout) findViewById(R.id.screen_light);

        // Dark Screen
        darkTitleLine1 = (TextView) findViewById(R.id.dark_title_line1);
        darkTitleLine2 = (TextView) findViewById(R.id.dark_title_line2);
        darkFinalSummary = (TextView) findViewById(R.id.dark_final_summary);
        btnDarkNext = (FrameLayout) findViewById(R.id.btn_dark_next);
        welcomeBottomLinks = (LinearLayout) findViewById(R.id.welcome_bottom_links);
        btnLanguage = (TextView) findViewById(R.id.btn_language);
        btnAccessibility = (TextView) findViewById(R.id.btn_accessibility);
        btnEmergency = (TextView) findViewById(R.id.btn_emergency);
        btnFinalBack = (TextView) findViewById(R.id.btn_final_back);

        // Underline welcome bottom links to match Image 1
        btnLanguage.setPaintFlags(btnLanguage.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnAccessibility.setPaintFlags(btnAccessibility.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnEmergency.setPaintFlags(btnEmergency.getPaintFlags() | Paint.UNDER
