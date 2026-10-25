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
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArcSetupActivity extends Activity implements View.OnClickListener, Runnable {

    private static final int STATUS_BAR_DISABLE_MASK = 0x00010000 | 0x00200000 | 0x01000000 | 0x00040000;
    private static final int ID_WIFI_BASE = 1000;
    private static final int ID_GOOGLE_ACCOUNT = 2001;
    private static final int ID_GOOGLE_LOC = 2002;
    private static final int ID_GOOGLE_SCAN = 2003;
    private static final int ID_GOOGLE_BACKUP = 2004;
    private static final int ID_LOCK_PIN = 3001;
    private static final int ID_LOCK_PWD = 3002;
    private static final int ID_LOCK_PAT = 3003;
    private static final int ID_NAV_GESTURE = 4001;
    private static final int ID_NAV_THREE = 4002;
    private static final int ID_LANG_BASE = 5000;

    private int currentStep = 1;
    private float density = 3.0f;

    private FrameLayout rootContainer;
    private LinearLayout screenDark;
    private LinearLayout screenLight;

    private TextView darkTitleLine1;
    private TextView darkTitleLine2;
    private TextView darkFinalSummary;
    private FrameLayout btnDarkNext;
    private LinearLayout welcomeBottomLinks;
    private TextView btnLanguage;
    private TextView btnAccessibility;
    private TextView btnEmergency;
    private TextView btnFinalBack;

    private FrameLayout btnLightBack;
    private FrameLayout btnLightNext;
    private TextView lightTitleLine1;
    private TextView lightTitleLine2;
    private TextView lightSubtitle;
    private LinearLayout cardListContainer;
    private TextView btnLightBottomPrimary;
    private TextView btnLightBottomSecondary;
    private TextView btnLightBottomTertiary;

    private FrameLayout modalOverlay;
    private TextView modalTitle;
    private EditText inputWifiPassword;
    private LinearLayout modalListContainer;
    private TextView btnModalCancel;
    private TextView btnModalConfirm;

    private List<String> scannedSsids = new ArrayList<String>();
    private List<Boolean> scannedSecured = new ArrayList<Boolean>();
    private boolean showAllWifi = false;
    private String selectedWifiSsid = "";

    private boolean offlineSkipGoogleAccount = false;
    private boolean optLocationEnabled = true;
    private boolean optScanningEnabled = true;
    private boolean optBackupEnabled = true;
    private boolean useGestureNav = true;

    private WifiManager wifiManager;
    private Handler handler;

    private final String[] langNames = new String[]{
            "English (United States)",
            "Filipino (Pilipinas)",
            "Español (Estados Unidos)",
            "日本語 (日本)",
            "中文 (简体)"
    };
    private final Locale[] langLocales = new Locale[]{
            Locale.US,
            new Locale("fil", "PH"),
            new Locale("es", "US"),
            Locale.JAPAN,
            Locale.SIMPLIFIED_CHINESE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isAlreadyProvisioned()) {
            disableSelfAndLaunchHome();
            return;
        }
        density = getResources().getDisplayMetrics().density;
        configureEdgeToEdgeWindow();
        setContentView(R.layout.activity_arc_setup);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        handler = new Handler(Looper.getMainLooper());

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
        renderStep();
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

    private GradientDrawable makeCircleOutline(int strokeColor, int strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.WHITE);
        gd.setStroke((int) (strokeDp * density + 0.5f), strokeColor);
        return gd;
    }

    private void bindViews() {
        rootContainer = (FrameLayout) findViewById(R.id.root_container);
        screenDark = (LinearLayout) findViewById(R.id.screen_dark);
        screenLight = (LinearLayout) findViewById(R.id.screen_light);

        darkTitleLine1 = (TextView) findViewById(R.id.dark_title_line1);
        darkTitleLine2 = (TextView) findViewById(R.id.dark_title_line2);
        darkFinalSummary = (TextView) findViewById(R.id.dark_final_summary);
        btnDarkNext = (FrameLayout) findViewById(R.id.btn_dark_next);
        welcomeBottomLinks = (LinearLayout) findViewById(R.id.welcome_bottom_links);
        btnLanguage = (TextView) findViewById(R.id.btn_language);
        btnAccessibility = (TextView) findViewById(R.id.btn_accessibility);
        btnEmergency = (TextView) findViewById(R.id.btn_emergency);
        btnFinalBack = (TextView) findViewById(R.id.btn_final_back);

        btnLanguage.setPaintFlags(btnLanguage.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnAccessibility.setPaintFlags(btnAccessibility.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnEmergency.setPaintFlags(btnEmergency.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnFinalBack.setPaintFlags(btnFinalBack.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        btnDarkNext.setOnClickListener(this);
        btnLanguage.setOnClickListener(this);
        btnAccessibility.setOnClickListener(this);
        btnEmergency.setOnClickListener(this);
        btnFinalBack.setOnClickListener(this);

        btnLightBack = (FrameLayout) findViewById(R.id.btn_light_back);
        btnLightNext = (FrameLayout) findViewById(R.id.btn_light_next);
        btnLightBack.setBackground(makeCircleOutline(0xFFCCCCCC, 1));
        btnLightNext.setBackground(makeCircleOutline(0xFF222222, 2));

        lightTitleLine1 = (TextView) findViewById(R.id.light_title_line1);
        lightTitleLine2 = (TextView) findViewById(R.id.light_title_line2);
        lightSubtitle = (TextView) findViewById(R.id.light_subtitle);
        cardListContainer = (LinearLayout) findViewById(R.id.card_list_container);

        btnLightBottomPrimary = (TextView) findViewById(R.id.btn_light_bottom_primary);
        btnLightBottomSecondary = (TextView) findViewById(R.id.btn_light_bottom_secondary);
        btnLightBottomTertiary = (TextView) findViewById(R.id.btn_light_bottom_tertiary);

        btnLightBack.setOnClickListener(this);
        btnLightNext.setOnClickListener(this);
        btnLightBottomPrimary.setOnClickListener(this);
        btnLightBottomSecondary.setOnClickListener(this);
        btnLightBottomTertiary.setOnClickListener(this);

        modalOverlay = (FrameLayout) findViewById(R.id.modal_overlay);
        modalTitle = (TextView) findViewById(R.id.modal_title);
        inputWifiPassword = (EditText) findViewById(R.id.input_wifi_password);
        modalListContainer = (LinearLayout) findViewById(R.id.modal_list_container);
        btnModalCancel = (TextView) findViewById(R.id.btn_modal_cancel);
        btnModalConfirm = (TextView) findViewById(R.id.btn_modal_confirm);

        btnModalCancel.setOnClickListener(this);
        btnModalConfirm.setOnClickListener(this);
    }

    private void addCardRow(String leftIcon, String title, String subtitle, String rightStatus, int rowId, boolean showDivider) {
        LinearLayout row = new LinearLayout(this);
        row.setId(rowId);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight((int) (54 * density));
        row.setPadding(0, (int) (10 * density), 0, (int) (10 * density));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(this);

        if (leftIcon != null && !leftIcon.isEmpty()) {
            TextView iconTv = new TextView(this);
            iconTv.setText(leftIcon);
            iconTv.setTextColor(Color.BLACK);
            iconTv.setTextSize(16);
            iconTv.setPadding(0, 0, (int) (14 * density), 0);
            row.addView(iconTv);
        }

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textCol.setLayoutParams(colLp);

        TextView titleTv = new TextView(this);
        titleTv.setText(title);
        titleTv.setTextColor(Color.BLACK);
        titleTv.setTextSize(15);
        textCol.addView(titleTv);

        if (subtitle != null && !subtitle.isEmpty()) {
            TextView subTv = new TextView(this);
            subTv.setText(subtitle);
            subTv.setTextColor(0xFF555555);
            subTv.setTextSize(12);
            textCol.addView(subTv);
        }
        row.addView(textCol);

        if (rightStatus != null && !rightStatus.isEmpty()) {
            TextView statusTv = new TextView(this);
            statusTv.setText(rightStatus);
            statusTv.setTextColor(Color.BLACK);
            statusTv.setTextSize(12);
            statusTv.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(statusTv);
        }

        cardListContainer.addView(row);

        if (showDivider) {
            View div = new View(this);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1 * density)));
            divLp.leftMargin = leftIcon != null ? (int) (28 * density) : 0;
            div.setLayoutParams(divLp);
            div.setBackgroundColor(0xFF444444);
            cardListContainer.addView(div);
        }
    }

    private void renderStep() {
        handler.removeCallbacks(this);
        modalOverlay.setVisibility(View.GONE);

        if (currentStep == 1 || currentStep == 6) {
            rootContainer.setBackgroundResource(R.drawable.bg_arc_ambient);
            screenDark.setVisibility(View.VISIBLE);
            screenLight.setVisibility(View.GONE);

            if (currentStep == 1) {
                darkTitleLine1.setText("Hi, lets set up your");
                darkTitleLine2.setText("New Device");
                darkFinalSummary.setVisibility(View.GONE);
                welcomeBottomLinks.setVisibility(View.VISIBLE);
                btnFinalBack.setVisibility(View.GONE);
            } else {
                darkTitleLine1.setText("You're all set,");
                darkTitleLine2.setText("Welcome to ArcOS");
                welcomeBottomLinks.setVisibility(View.GONE);
                btnFinalBack.setVisibility(View.VISIBLE);
                darkFinalSummary.setVisibility(View.VISIBLE);

                String netStatus = getConnectedWifiSsid();
                if (netStatus == null || netStatus.isEmpty()) {
                    netStatus = offlineSkipGoogleAccount ? "Offline Setup" : "Mobile Data / Ready";
                } else {
                    netStatus = "Wi-Fi (" + netStatus + ")";
                }
                String lockStatus = isDeviceSecureLockSet() ? "Screen Lock Active" : "Skipped";
                String navStatus = useGestureNav ? "Gesture Navigation" : "3-Button Navigation";
                darkFinalSummary.setText(
                        "Network: " + netStatus + "\n"
                                + "Security: " + lockStatus + "\n"
                                + "Navigation: " + navStatus + "\n\n"
                                + "Tap the arrow below to launch ArcOS."
                );
            }
            return;
        }

        rootContainer.setBackgroundColor(Color.WHITE);
        screenDark.setVisibility(View.GONE);
        screenLight.setVisibility(View.VISIBLE);
        cardListContainer.removeAllViews();
        btnLightBottomTertiary.setVisibility(View.GONE);

        if (currentStep == 2) {
            lightTitleLine1.setText("Let's connect to a");
            lightTitleLine2.setText("network");
            String connected = getConnectedWifiSsid();
            lightSubtitle.setText(connected != null ? "Connected to " + connected : "Select a Wi-Fi network");

            btnLightBottomPrimary.setVisibility(View.VISIBLE);
            btnLightBottomPrimary.setText(showAllWifi ? "Refresh networks" : "View all networks");
            btnLightBottomSecondary.setVisibility(View.VISIBLE);
            btnLightBottomSecondary.setText("Continue with Mobile Data");
            btnLightBottomTertiary.setVisibility(View.VISIBLE);
            btnLightBottomTertiary.setText("Continue without Wi-Fi");

            triggerWifiScan();
        } else if (currentStep == 3) {
            lightTitleLine1.setText("Google Services &");
            lightTitleLine2.setText("Permissions");
            lightSubtitle.setText("Configure system & GMS services");

            if (!offlineSkipGoogleAccount) {
                String acc = getSignedInGoogleAccount();
                addCardRow(
                        null,
                        acc != null ? "Google Account Added" : "Sign in to Google",
                        acc != null ? acc : "Add your Google Account for Play Store & sync",
                        acc != null ? "Added" : "Sign in",
                        ID_GOOGLE_ACCOUNT,
                        true
                );
            }
            addCardRow(null, "Use Location Services", "Allow apps & system to estimate location",
                    optLocationEnabled ? "ON" : "OFF", ID_GOOGLE_LOC, true);
            addCardRow(null, "Wi-Fi & Bluetooth Scanning", "Improve location & network accuracy",
                    optScanningEnabled ? "ON" : "OFF", ID_GOOGLE_SCAN, true);
            addCardRow(null, "Device Backup & Diagnostics", "Back up app data and system settings",
                    optBackupEnabled ? "ON" : "OFF", ID_GOOGLE_BACKUP, false);

            btnLightBottomPrimary.setVisibility(View.VISIBLE);
            btnLightBottomPrimary.setText("Accept & Continue");
            btnLightBottomSecondary.setVisibility(View.GONE);
        } else if (currentStep == 4) {
            lightTitleLine1.setText("Protect your");
            lightTitleLine2.setText("device");
            lightSubtitle.setText(isDeviceSecureLockSet()
                    ? "Screen lock is active"
                    : "Set a screen lock or skip for now");

            addCardRow(null, "PIN", "4 to 16 numeric digits", "Set", ID_LOCK_PIN, true);
            addCardRow(null, "Password", "High security alphanumeric password", "Set", ID_LOCK_PWD, true);
            addCardRow(null, "Pattern", "Draw a simple pattern to unlock", "Set", ID_LOCK_PAT, false);

            btnLightBottomPrimary.setVisibility(View.VISIBLE);
            btnLightBottomPrimary.setText("Set Screen Lock");
            btnLightBottomSecondary.setVisibility(View.VISIBLE);
            btnLightBottomSecondary.setText("Skip for now");
        } else if (currentStep == 5) {
            lightTitleLine1.setText("System");
            lightTitleLine2.setText("navigation");
            lightSubtitle.setText("Choose how you move around ArcOS");

            addCardRow(null, "Gesture Navigation", "Swipe up to go Home, swipe edges for Back",
                    useGestureNav ? "Selected" : "", ID_NAV_GESTURE, true);
            addCardRow(null, "3-Button Navigation", "Back, Home, and Overview buttons at bottom",
                    !useGestureNav ? "Selected" : "", ID_NAV_THREE, false);

            btnLightBottomPrimary.setVisibility(View.VISIBLE);
            btnLightBottomPrimary.setText("Continue");
            btnLightBottomSecondary.setVisibility(View.GONE);
        }
    }

    @Override
    public void run() {
        if (currentStep == 2) {
            populateWifiRows();
            handler.postDelayed(this, 3000);
        }
    }

    private void triggerWifiScan() {
        try {
            if (wifiManager != null) {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
                wifiManager.startScan();
            }
        } catch (Throwable ignored) {
        }
        populateWifiRows();
        handler.removeCallbacks(this);
        handler.postDelayed(this, 2500);
    }

    private void populateWifiRows() {
        if (currentStep != 2) {
            return;
        }
        cardListContainer.removeAllViews();
        scannedSsids.clear();
        scannedSecured.clear();

        String connectedSsid = getConnectedWifiSsid();
        if (connectedSsid != null) {
            lightSubtitle.setText("Connected to " + connectedSsid);
        }

        try {
            if (wifiManager != null) {
                List<ScanResult> results = wifiManager.getScanResults();
                if (results != null) {
                    for (int i = 0; i < results.size(); i++) {
                        ScanResult sr = results.get(i);
                        if (sr == null || sr.SSID == null) {
                            continue;
                        }
                        String ssid = sr.SSID.trim();
                        if (ssid.isEmpty() || scannedSsids.contains(ssid)) {
                            continue;
                        }
                        scannedSsids.add(ssid);
                        String caps = sr.capabilities != null ? sr.capabilities : "";
                        boolean sec = caps.contains("WPA") || caps.contains("WEP")
                                || caps.contains("SAE") || caps.contains("EAP")
                                || caps.contains("PSK");
                        scannedSecured.add(Boolean.valueOf(sec));
                        if (scannedSsids.size() >= (showAllWifi ? 8 : 2)) {
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        if (scannedSsids.isEmpty()) {
            addCardRow("\u25CE", "Scanning for networks...", null, "", ID_WIFI_BASE, false);
            return;
        }

        for (int i = 0; i < scannedSsids.size(); i++) {
            String ssid = scannedSsids.get(i);
            boolean sec = scannedSecured.get(i).booleanValue();
            String right = (connectedSsid != null && connectedSsid.equals(ssid))
                    ? "Connected"
                    : (sec ? "Secured" : "");
            boolean showDiv = (i < scannedSsids.size() - 1);
            addCardRow("\u25CE", ssid, null, right, ID_WIFI_BASE + i, showDiv);
        }
    }

    private String getConnectedWifiSsid() {
        try {
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                WifiInfo info = wifiManager.getConnectionInfo();
                if (info != null && info.getNetworkId() != -1) {
                    String ssid = info.getSSID();
                    if (ssid != null && !ssid.equals("<unknown ssid>")) {
                        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() > 2) {
                            ssid = ssid.substring(1, ssid.length() - 1);
                        }
                        return ssid;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void showWifiPasswordModal(String ssid) {
        selectedWifiSsid = ssid;
        modalTitle.setText("Connect to " + ssid);
        inputWifiPassword.setVisibility(View.VISIBLE);
        inputWifiPassword.setText("");
        modalListContainer.setVisibility(View.GONE);
        btnModalConfirm.setVisibility(View.VISIBLE);
        modalOverlay.setVisibility(View.VISIBLE);
        inputWifiPassword.requestFocus();
    }

    private void showLanguageModal() {
        modalTitle.setText("Select Language");
        inputWifiPassword.setVisibility(View.GONE);
        btnModalConfirm.setVisibility(View.GONE);
        modalListContainer.setVisibility(View.VISIBLE);
        modalListContainer.removeAllViews();

        for (int i = 0; i < langNames.length; i++) {
            TextView tv = new TextView(this);
            tv.setId(ID_LANG_BASE + i);
            tv.setText(langNames[i]);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(15);
            tv.setPadding(0, (int) (12 * density), 0, (int) (12 * density));
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(this);
            modalListContainer.addView(tv);
        }
        modalOverlay.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("deprecation")
    private void connectToWifiNetwork(String ssid, String password, boolean isOpen) {
        try {
            if (wifiManager == null) {
                return;
            }
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = "\"" + ssid + "\"";
            if (isOpen) {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            } else {
                config.preSharedKey = "\"" + password + "\"";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            }

            boolean connectedBySystemApi = false;
            try {
                Method[] methods = WifiManager.class.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    Method m = methods[i];
                    if ("connect".equals(m.getName())) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 2 && params[0] == WifiConfiguration.class) {
                            m.setAccessible(true);
                            m.invoke(wifiManager, config, null);
                            connectedBySystemApi = true;
                            break;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            if (!connectedBySystemApi) {
                int netId = wifiManager.addNetwork(config);
                if (netId != -1) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                }
            }
            lightSubtitle.setText("Connecting to " + ssid + "...");
            offlineSkipGoogleAccount = false;
            handler.removeCallbacks(this);
            handler.postDelayed(this, 2000);
        } catch (Throwable ignored) {
        }
    }

    private void enableMobileDataAndContinue() {
        try {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                Method m = TelephonyManager.class.getDeclaredMethod("setDataEnabled", boolean.class);
                m.setAccessible(true);
                m.invoke(tm, Boolean.TRUE);
            }
        } catch (Throwable ignored) {
        }
        offlineSkipGoogleAccount = false;
        currentStep = 3;
        renderStep();
    }

    private String getSignedInGoogleAccount() {
        try {
            Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
            if (accounts != null && accounts.length > 0) {
                return accounts[0].name;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void launchGoogleAccountSignIn() {
        try {
            AccountManager.get(this).addAccount("com.google", null, null, null, this, null, null);
        } catch (Throwable e) {
            try {
                Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, new String[]{"com.google"});
                startActivity(intent);
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyGooglePermissions() {
        try {
            ContentResolver cr = getContentResolver();
            Settings.Secure.putInt(cr, Settings.Secure.LOCATION_MODE, optLocationEnabled ? 3 : 0);
            Settings.Global.putInt(cr, "wifi_scan_always_enabled", optScanningEnabled ? 1 : 0);
            Settings.Global.putInt(cr, "ble_scan_always_enabled", optScanningEnabled ? 1 : 0);
            Settings.Secure.putInt(cr, "backup_enabled", optBackupEnabled ? 1 : 0);
        } catch (Throwable ignored) {
        }
    }

    private boolean isDeviceSecureLockSet() {
        try {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            return km != null && km.isDeviceSecure();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void launchScreenLockSetup() {
        try {
            startActivity(new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD));
        } catch (Throwable e) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyNavigationMode(boolean gesture) {
        useGestureNav = gesture;
        String targetPkg = gesture
                ? "com.android.internal.systemui.navbar.gestural"
                : "com.android.internal.systemui.navbar.threebutton";
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            Method getService = smClass.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, "overlay");
            if (binder != null) {
                Class<?> stubClass = Class.forName("android.content.om.IOverlayManager$Stub");
                Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
                Object om = asInterface.invoke(null, binder);
                Method setExclusive = om.getClass().getMethod("setEnabledExclusiveInCategory", String.class, int.class);
                setExclusive.invoke(om, targetPkg, Integer.valueOf(0));
            }
        } catch (Throwable ignored) {
        }
        try {
            Runtime.getRuntime().exec(new String[]{"cmd", "overlay", "enable-exclusive", targetPkg});
        } catch (Throwable ignored) {
        }
    }

    private void applySystemLocale(Locale locale) {
        try {
            Class<?> lpClass = Class.forName("com.android.internal.app.LocalePicker");
            Method updateLocale = lpClass.getMethod("updateLocale", Locale.class);
            updateLocale.invoke(null, locale);
        } catch (Throwable ignored) {
        }
        btnLanguage.setText("Language (" + locale.getDisplayLanguage() + ")");
        modalOverlay.setVisibility(View.GONE);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id >= ID_WIFI_BASE && id < ID_WIFI_BASE + 50) {
            int idx = id - ID_WIFI_BASE;
            if (idx >= 0 && idx < scannedSsids.size()) {
                String ssid = scannedSsids.get(idx);
                boolean sec = scannedSecured.get(idx).booleanValue();
                String conn = getConnectedWifiSsid();
                if (conn != null && conn.equals(ssid)) {
                    offlineSkipGoogleAccount = false;
                    currentStep = 3;
                    renderStep();
                } else if (!sec) {
                    connectToWifiNetwork(ssid, "", true);
                } else {
                    showWifiPasswordModal(ssid);
                }
            } else {
                triggerWifiScan();
            }
            return;
        }

        if (id >= ID_LANG_BASE && id < ID_LANG_BASE + langLocales.length) {
            applySystemLocale(langLocales[id - ID_LANG_BASE]);
            return;
        }

        if (id == R.id.btn_dark_next) {
            if (currentStep == 1) {
                currentStep = 2;
                renderStep();
            } else if (currentStep == 6) {
                completeProvisioningAndFinish();
            }
        } else if (id == R.id.btn_final_back) {
            currentStep = 5;
            renderStep();
        } else if (id == R.id.btn_language) {
            showLanguageModal();
        } else if (id == R.id.btn_accessibility) {
            try {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Throwable ignored) {
            }
        } else if (id == R.id.btn_emergency) {
            try {
                Intent em = new Intent("com.android.phone.EmergencyDialer.DIAL");
                em.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(em);
            } catch (Throwable e) {
                try {
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")));
                } catch (Throwable ignored) {
                }
            }
        } else if (id == R.id.btn_light_back) {
            if (currentStep > 1) {
                currentStep--;
                renderStep();
            }
        } else if (id == R.id.btn_light_next) {
            advanceFromLightStep();
        } else if (id == R.id.btn_light_bottom_primary) {
            if (currentStep == 2) {
                showAllWifi = !showAllWifi;
                renderStep();
            } else if (currentStep == 4) {
                launchScreenLockSetup();
            } else {
                advanceFromLightStep();
            }
        } else if (id == R.id.btn_light_bottom_secondary) {
            if (currentStep == 2) {
                enableMobileDataAndContinue();
            } else if (currentStep == 4) {
                currentStep = 5;
                renderStep();
            }
        } else if (id == R.id.btn_light_bottom_tertiary) {
            if (currentStep == 2) {
                offlineSkipGoogleAccount = true;
                currentStep = 3;
                renderStep();
            }
        } else if (id == R.id.btn_modal_cancel) {
            hideKeyboard();
            modalOverlay.setVisibility(View.GONE);
        } else if (id == R.id.btn_modal_confirm) {
            String pwd = inputWifiPassword.getText().toString();
            hideKeyboard();
            modalOverlay.setVisibility(View.GONE);
            connectToWifiNetwork(selectedWifiSsid, pwd, false);
        } else if (id == ID_GOOGLE_ACCOUNT) {
            launchGoogleAccountSignIn();
        } else if (id == ID_GOOGLE_LOC) {
            optLocationEnabled = !optLocationEnabled;
            renderStep();
        } else if (id == ID_GOOGLE_SCAN) {
            optScanningEnabled = !optScanningEnabled;
            renderStep();
        } else if (id == ID_GOOGLE_BACKUP) {
            optBackupEnabled = !optBackupEnabled;
            renderStep();
        } else if (id == ID_LOCK_PIN || id == ID_LOCK_PWD || id == ID_LOCK_PAT) {
            launchScreenLockSetup();
        } else if (id == ID_NAV_GESTURE) {
            applyNavigationMode(true);
            renderStep();
        } else if (id == ID_NAV_THREE) {
            applyNavigationMode(false);
            renderStep();
        }
    }

    private void advanceFromLightStep() {
        if (currentStep == 2) {
            currentStep = 3;
        } else if (currentStep == 3) {
            applyGooglePermissions();
            currentStep = 4;
        } else if (currentStep == 4) {
            currentStep = 5;
        } else if (currentStep == 5) {
            applyNavigationMode(useGestureNav);
            currentStep = 6;
        }
        renderStep();
    }

    private boolean isAlreadyProvisioned() {
        try {
            ContentResolver cr = getContentResolver();
            int dev = Settings.Global.getInt(cr, Settings.Global.DEVICE_PROVISIONED, 0);
            int usr = Settings.Secure.getInt(cr, "user_setup_complete", 0);
            return (dev == 1 && usr == 1);
        } catch (Throwable e) {
            return false;
        }
    }

    private void completeProvisioningAndFinish() {
        try {
            ContentResolver cr = getContentResolver();
            Settings.Global.putInt(cr, Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(cr, "user_setup_complete", 1);
            Settings.Secure.putInt(cr, "tv_user_setup_complete", 1);
        } catch (Throwable ignored) {
        }
        setStatusBarLocked(false);
        disableSelfAndLaunchHome();
    }

    private void disableSelfAndLaunchHome() {
        try {
            PackageManager pm = getPackageManager();
            ComponentName cn = new ComponentName(this, ArcSetupActivity.class);
            pm.setComponentEnabledSetting(
                    cn,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {
        }
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(home);
        } catch (Throwable ignored) {
        }
        finish();
    }

    @SuppressWarnings("WrongConstant")
    private void setStatusBarLocked(boolean locked) {
        try {
            Object sbService = getSystemService("statusbar");
            if (sbService != null) {
                Method disable = sbService.getClass().getMethod("disable", int.class);
                disable.invoke(sbService, Integer.valueOf(locked ? STATUS_BAR_DISABLE_MASK : 0));
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (modalOverlay != null && modalOverlay.getVisibility() == View.VISIBLE) {
                modalOverlay.setVisibility(View.GONE);
                return true;
            }
            if (currentStep > 1) {
                currentStep--;
                renderStep();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
