#!/system/bin/sh
# BRING-UP DEBUG: dump boot/perf state to /cache/z3tc every 15s (synced, survives forced power-off)
D=/cache/z3tc
rm -rf $D.prev; [ -d $D ] && mv $D $D.prev; mkdir -p $D
n=0
while true; do
  n=$((n+1))
  dmesg > $D/dmesg.txt 2>&1
  logcat -d -b all -t 20000 > $D/logcat.txt 2>&1
  ps -A -o PID,PPID,STAT,PCPU,RSS,NAME > $D/ps.txt 2>&1
  getprop > $D/props.txt 2>&1
  {
    echo "=== round $n $(date)"
    echo "online=$(cat /sys/devices/system/cpu/online) offline=$(cat /sys/devices/system/cpu/offline)"
    for c in 0 1 2 3; do
      d=/sys/devices/system/cpu/cpu$c/cpufreq
      echo "cpu$c gov=$(cat $d/scaling_governor 2>/dev/null) cur=$(cat $d/scaling_cur_freq 2>/dev/null) min=$(cat $d/scaling_min_freq 2>/dev/null) max=$(cat $d/scaling_max_freq 2>/dev/null)"
    done
    echo "gpu gov=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/governor 2>/dev/null) cur=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq 2>/dev/null) max=$(cat /sys/class/kgsl/kgsl-3d0/devfreq/max_freq 2>/dev/null)"
    echo "core_control=$(cat /sys/module/msm_thermal/core_control/enabled 2>/dev/null) cpus_offlined=$(cat /sys/module/msm_thermal/core_control/cpus_offlined 2>/dev/null)"
    echo "loadavg=$(cat /proc/loadavg)"
    top -b -n 1 -m 8 2>/dev/null | tail -n 9
  } >> $D/perf.txt
  tail -n 3000 $D/perf.txt > $D/perf.tmp && mv $D/perf.tmp $D/perf.txt
  sync
  sleep 15
done
