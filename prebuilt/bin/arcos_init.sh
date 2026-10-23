#!/system/bin/sh
# Wait for boot to complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done

# Ensure FOD HBM and Touch sysfs nodes are writable by SystemUI & AIDL HAL
chmod 0666 /sys/devices/virtual/mi_display/disp_feature/disp-DSI-0/disp_param
chmod 0666 /sys/class/mi_display/disp-DSI-0/disp_param
chmod 0666 /sys/class/touch/touch_dev/fod_press_status

# Clear runtime wm density overrides so Stable Density == Current Density (fixes UDFPS clipping)
wm density reset
