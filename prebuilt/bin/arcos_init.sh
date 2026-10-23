#!/system/bin/sh
# Wait for boot to complete
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 1
done

# Enforce 374dp Smallest Width on 1080p panel (1080 / (462/160) = 374dp)
wm density 462

# Set custom settings for ArcOS Out-of-the-box feel
settings put global window_animation_scale 1.0
settings put global transition_animation_scale 1.0
settings put global animator_duration_scale 1.0
