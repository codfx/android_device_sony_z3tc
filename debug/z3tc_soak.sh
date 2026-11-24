#!/system/bin/sh
#
# Copyright (C) 2026 codfx
# SPDX-License-Identifier: Apache-2.0
#
# BRING-UP DEBUG: standby soak logger, started by init at boot while persist.vendor.z3tc.soak is set, so it
# does not depend on a PC or an adb session. persist.vendor.z3tc.soak: 1 = start a new soak at the next boot,
# 2 = soak running (a reboot during the soak continues in a new segment), 0/unset = off.
# Output: /data/local/tmp/soak (segment 1), /data/local/tmp/soak/seg<N> after reboots. Stop: touch <dir>/stop
# (the host's soak.sh collect does this, takes the end snapshots and sets the property to 0).
B=/sys/class/power_supply
R=/data/local/tmp/soak
state=$(getprop persist.vendor.z3tc.soak)
sleep 60
if [ "$state" = 1 ]; then
  [ -d $R ] && { rm -rf $R.prev; mv $R $R.prev; }
  mkdir -p $R; O=$R
  setprop persist.vendor.z3tc.soak 2
  dumpsys batterystats --reset >/dev/null 2>&1
  first=1
else
  n=2; while [ -d $R/seg$n ]; do n=$((n+1)); done
  O=$R/seg$n; mkdir -p $O; first=0
  echo "$(date +%m-%dT%T) REBOOT during soak, continuing in seg$n (boot $(cat /proc/sys/kernel/random/boot_id))" >> $R/log.txt
fi
dmesg -w > $O/kmsg.txt 2>/dev/null &
cp /sys/kernel/debug/wakeup_sources $O/ws-start.txt; cat /sys/kernel/debug/suspend_stats > $O/suspend_stats-start.txt
for f in rpm_stats rpm_master_stats; do echo "== $f $(date +%m-%dT%T) uptime $(cut -d. -f1 /proc/uptime)"; cat /sys/kernel/debug/$f; done > $O/rpm-start.txt 2>&1
ts0=$(ls /data/tombstones 2>/dev/null | wc -l)
wifi() { dumpsys wifi 2>/dev/null | grep -m1 -oE 'mWifiInfo SSID: "[^"]*", BSSID: [^,]+|mWifiInfo SSID: <unknown ssid>' ; }
echo "# soak $(date) kernel $(uname -r) boot $(cat /proc/sys/kernel/random/boot_id) segment $O" >> $O/log.txt
echo "time uptime_s batt% voltage_uV status usb current_uA temp bms_cc_uAh bms_cc_shadow_uAh ssid freq ip net suspends_ok suspends_fail screen tombstones crashbuf kernel_errs deep light" >> $O/log.txt
input keyevent KEYCODE_SLEEP
while [ ! -f $O/stop ] && [ ! -f $R/stop ]; do
  w=$(wifi | sed 's/mWifiInfo //; s/ /_/g')
  f=$(dumpsys wifi 2>/dev/null | grep -m1 -oE 'Frequency: [0-9]+MHz' | grep -oE '[0-9]+')
  n=$(dumpsys connectivity 2>/dev/null | grep -oE "NetworkAgentInfo\{network\{[0-9]+\}[^}]*\}[^}]*ni\{WIFI CONNECTED" | grep -oE "network\{[0-9]+" | grep -oE "[0-9]+" | head -1)
  ip=$(ip -4 addr show wlan0 2>/dev/null | grep -oE 'inet [0-9.]+' | cut -d' ' -f2)
  echo "$(date +%m-%dT%T) $(cut -d. -f1 /proc/uptime) $(cat $B/battery/capacity) $(cat $B/battery/voltage_now) $(cat $B/battery/status) $(cat $B/usb/online) $(cat $B/battery/current_now) $(cat $B/battery/temp) $(cat $B/bms/charge_counter) $(cat $B/bms/charge_counter_shadow) ${w:-none} ${f:--} ${ip:--} ${n:--} $(grep '^success' /sys/kernel/debug/suspend_stats | cut -f2) $(grep '^fail:' /sys/kernel/debug/suspend_stats | cut -f2) $(dumpsys power | grep -m1 -oE 'mWakefulness=[A-Za-z]+' | cut -d= -f2) $(( $(ls /data/tombstones 2>/dev/null | wc -l) - ts0 )) $(logcat -b crash -d 2>/dev/null | grep -cE 'Fatal signal|FATAL EXCEPTION|\*\*\* \*\*\* \*\*\*') $(grep -cE 'Oops|BUG: |WARNING: at|Unable to handle|Kernel panic|soft lockup|hung_task' $O/kmsg.txt) $(dumpsys deviceidle | grep -m1 -oE 'mState=[A-Z_]+' | cut -d= -f2) $(dumpsys deviceidle | grep -m1 -oE 'mLightState=[A-Z_]+' | cut -d= -f2)" >> $O/log.txt
  sync
  sleep 300
done
pidof dmesg | xargs -r kill
