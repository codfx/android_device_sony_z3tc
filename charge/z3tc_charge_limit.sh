#!/vendor/bin/sh
#
# Copyright (C) 2026 The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#
# Battery charge limiter for the Xperia Z3 Tablet Compact (off by default).
#   persist.vendor.z3tc.charge_limit_enable  1 = on, anything else = off
#   persist.vendor.z3tc.charge_limit         stop charging at this % (51..99, default 80)
#   persist.vendor.z3tc.charge_resume        resume at or below this % (30..limit-1, default limit-5)
#
# It drives the qpnp charger driver's own limiter (Sony "LLK": battery/enable_llk, llk_socmax,
# llk_socmin). While a charger is connected the driver checks it every 3 s: at >= max it stops
# charging, at <= min it starts again; unplugging clears a pause. So this script only writes the
# settings and exits: it never polls and never wakes the device. init runs it once per property
# change, and at boot when one of the properties is set (the driver itself starts with LLK off).
#
# battery/charging_enabled is not used: it also forces the tablet to run from the battery, which
# trips the driver's reverse-boost check, and the charger then stays "unplugged" until it is
# physically replugged. The hardware has no working idle mode (charger powering the tablet while
# the battery neither charges nor discharges): while paused, the battery supplies the tablet.

B=/sys/class/power_supply/battery
TAG=z3tc_charge_limit

log_i() { log -p i -t $TAG "$*"; }

num() { case "$1" in ''|*[!0-9]*) echo "$2" ;; *) echo "$1" ;; esac; }

if [ "$(getprop persist.vendor.z3tc.charge_limit_enable)" != 1 ]; then
    if [ "$(cat $B/enable_llk)" = 0 ]; then
        log_i "off"
        exit 0
    fi
    # enable_llk=0 alone leaves an active pause in place (the driver only clears it in its own
    # check), so first let the driver resume charging, then switch LLK off. The driver tests
    # "capacity >= max -> pause" before "capacity <= min -> charge", so both limits go above any
    # possible capacity (the driver accepts any value). Then poll until charging has resumed
    # (the driver checks every 3 s), for at most OFF_WAIT seconds.
    OFF_WAIT=30
    echo 101 > $B/llk_socmax
    echo 101 > $B/llk_socmin
    i=0
    while [ $i -lt $OFF_WAIT ]; do
        case "$(cat $B/status)" in Charging|Full) break ;; esac
        [ "$(cat /sys/class/power_supply/usb/online)$(cat /sys/class/power_supply/qpnp-dc/online)" = 00 ] && break
        sleep 1
        i=$((i + 1))
    done
    if [ $i -ge $OFF_WAIT ]; then
        # Not resumed yet (e.g. charging held off for temperature). Switching LLK off now could
        # leave the pause stuck until an unplug, so leave it on at 101/101, which never pauses:
        # the driver clears the pause at its next check. A reboot resets LLK to off.
        log_i "off pending: charging not resumed after ${i}s (status $(cat $B/status)), LLK left at 101/101"
        exit 0
    fi
    echo 0 > $B/enable_llk
    echo 0 > $B/llk_socmax
    echo 0 > $B/llk_socmin
    log_i "off (was on; status $(cat $B/status) after ${i}s)"
    exit 0
fi

LIMIT=$(num "$(getprop persist.vendor.z3tc.charge_limit)" 80)
if [ "$LIMIT" -le 50 ] || [ "$LIMIT" -ge 100 ]; then
    LIMIT=80
fi
RESUME=$(num "$(getprop persist.vendor.z3tc.charge_resume)" $((LIMIT - 5)))
if [ "$RESUME" -lt 30 ] || [ "$RESUME" -ge "$LIMIT" ]; then
    RESUME=$((LIMIT - 5))
fi

echo $RESUME > $B/llk_socmin
echo $LIMIT > $B/llk_socmax
echo 1 > $B/enable_llk
log_i "on: stop at $LIMIT%, resume at $RESUME% (now $(cat $B/capacity)%, $(cat $B/status))"
