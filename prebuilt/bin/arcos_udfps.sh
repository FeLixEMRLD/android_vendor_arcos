#!/system/bin/sh
# ArcOS Real-Time AIDL UDFPS Hardware Bridge for Emerald (Goodix FOD + Mi Display DSI-0)

DISP_PARAM="/sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param"
FOD_PRESS="/sys/class/touch/touch_dev/fod_press_status"
XIAOMI_FP_SVC="vendor.xiaomi.hardware.fingerprintextension.IXiaomiFingerprint/default"

# Clear old logs and watch FingerprintODMHalSession in real time
logcat -c
logcat -b main -v brief FingerprintODMHalSession:D UdfpsController:D *:S | while read -r line; do
    case "$line" in
        *"onPointerDown"*)
            # 1. Notify Touch Driver that finger is pressing FOD area
            echo "1" > "$FOD_PRESS" 2>/dev/null
            # 2. Turn ON Xiaomi Local HBM (High Brightness Mode) on DSI-0
            echo "9 1" > "$DISP_PARAM" 2>/dev/null
            # 3. Notify Xiaomi Fingerprint Extension AIDL HAL (cmd 1, param1=10/4, param2=1)
            service call "$XIAOMI_FP_SVC" 1 i32 10 i32 1 >/dev/null 2>&1
            service call "$XIAOMI_FP_SVC" 1 i32 4 i32 1 >/dev/null 2>&1
            ;;
        *"onPointerUp"*|*"hideUdfpsOverlay"*)
            # 1. Turn OFF Xiaomi Local HBM on DSI-0
            echo "9 0" > "$DISP_PARAM" 2>/dev/null
            # 2. Reset Touch Driver FOD status
            echo "0" > "$FOD_PRESS" 2>/dev/null
            # 3. Notify Xiaomi Fingerprint Extension AIDL HAL finger lifted
            service call "$XIAOMI_FP_SVC" 1 i32 10 i32 0 >/dev/null 2>&1
            service call "$XIAOMI_FP_SVC" 1 i32 4 i32 0 >/dev/null 2>&1
            ;;
    esac
done
