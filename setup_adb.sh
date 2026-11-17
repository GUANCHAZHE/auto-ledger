#!/usr/bin/env bash
set -euo pipefail

sudo tee /etc/udev/rules.d/51-android.rules >/dev/null <<'EOF'
SUBSYSTEM=="usb", ATTR{idVendor}=="18d1", MODE="0666", GROUP="plugdev"
SUBSYSTEM=="usb", ATTR{idVendor}=="2717", MODE="0666", GROUP="plugdev"
EOF

sudo udevadm control --reload-rules
sudo udevadm trigger

/home/cooper/Android/platform-tools/adb kill-server
/home/cooper/Android/platform-tools/adb devices -l
