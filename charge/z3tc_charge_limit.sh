#!/vendor/bin/sh
#
# Copyright (C) 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
# Battery charge limiter for the Xperia Z3 Tablet Compact (off by default).
#   persist.vendor.z3tc.charge_limit   stop charging at this % (50..99; unset/0/100 = off)
#   persist.vendor.z3tc.charge_resume  resume charging at or below this % (default: limit - 5)
#   persist.vendor.z3tc.charge_idle    1 = idle mode: cut the charger input current
#                                      (qpnp-dc current_max = 0) instead of disabling charging
#                                      (battery/charging_enabled = 0)
# init restarts this service whenever one of the properties changes. Every start first restores
# normal charging (init stops services with SIGKILL, so the exit trap is only best effort);
# setting the limit to 0/100 therefore turns charging back on. It waits with a plain sleep,
# so it never wakes the device itself (no alarms, no wakelocks).

B=/sys/class/power_supply
EN=$B/battery/charging_enabled
DC=$B/qpnp-dc/current_max
TAG=z3tc_charge_limit

log() { echo "$TAG: $*" > /dev/kmsg; }

DC_ORIG=$(cat $DC 2>/dev/null)
[ -n "$DC_ORIG" ] && [ "$DC_ORIG" -gt 0 ] 2>/dev/null || DC_ORIG=1800000

start_charge() {
    echo 1 > $EN
    echo $DC_ORIG > $DC 2>/dev/null
}
stop_charge() {
    if [ "$IDLE" = 1 ]; then
        echo 0 > $DC
    else
        echo 0 > $EN
    fi
}

trap 'start_charge; log "stopped, charging restored"; exit 0' TERM INT HUP

LIMIT=$(getprop persist.vendor.z3tc.charge_limit)
RESUME=$(getprop persist.vendor.z3tc.charge_resume)
IDLE=$(getprop persist.vendor.z3tc.charge_idle)

case "$LIMIT" in ''|*[!0-9]*) LIMIT=0 ;; esac
if [ "$LIMIT" -lt 50 ] || [ "$LIMIT" -ge 100 ]; then
    start_charge
    log "off (limit '$(getprop persist.vendor.z3tc.charge_limit)')"
    exit 0
fi
case "$RESUME" in ''|*[!0-9]*) RESUME=$((LIMIT - 5)) ;; esac
if [ "$RESUME" -ge "$LIMIT" ] || [ "$RESUME" -lt 30 ]; then
    RESUME=$((LIMIT - 5))
fi
[ "$IDLE" = 1 ] || IDLE=0

log "on: limit $LIMIT%, resume $RESUME%, idle mode $IDLE"
start_charge
charging=1
while :; do
    cap=$(cat $B/battery/capacity)
    if [ $charging = 1 ] && [ "$cap" -ge "$LIMIT" ]; then
        stop_charge; charging=0; log "$cap% >= $LIMIT%: charging paused"
    elif [ $charging = 0 ] && [ "$cap" -le "$RESUME" ]; then
        start_charge; charging=1; log "$cap% <= $RESUME%: charging resumed"
    fi
    sleep 30 &
    wait $!
done
