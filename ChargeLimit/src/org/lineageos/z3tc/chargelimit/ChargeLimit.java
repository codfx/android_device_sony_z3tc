/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.os.SystemProperties;

/**
 * Charge limiter settings. They are persistent system properties; init re-runs
 * /vendor/bin/z3tc_charge_limit.sh whenever one changes, and that script drives the charger
 * driver's own limiter. Ranges and defaults match the script.
 */
final class ChargeLimit {
    static final String PROP_ENABLE = "persist.sys.z3tc.charge_enable";
    static final String PROP_LIMIT = "persist.sys.z3tc.charge_limit";
    static final String PROP_RESUME = "persist.sys.z3tc.charge_resume";

    static final int LIMIT_MIN = 51;
    static final int LIMIT_MAX = 99;
    static final int LIMIT_DEFAULT = 80;
    static final int RESUME_MIN = 30;
    static final int RESUME_GAP_DEFAULT = 5;

    private ChargeLimit() {}

    static boolean isEnabled() {
        return "1".equals(SystemProperties.get(PROP_ENABLE));
    }

    static int getLimit() {
        int limit = SystemProperties.getInt(PROP_LIMIT, LIMIT_DEFAULT);
        return (limit < LIMIT_MIN || limit > LIMIT_MAX) ? LIMIT_DEFAULT : limit;
    }

    static int getResume() {
        int limit = getLimit();
        int resume = SystemProperties.getInt(PROP_RESUME, limit - RESUME_GAP_DEFAULT);
        return (resume < RESUME_MIN || resume >= limit) ? limit - RESUME_GAP_DEFAULT : resume;
    }

    static void setEnabled(boolean enabled) {
        if (enabled) {
            // Store the current (or default) levels explicitly so the script sees them.
            setLevels(getLimit(), getResume());
        }
        SystemProperties.set(PROP_ENABLE, enabled ? "1" : "0");
    }

    /** Stores both levels; resume is clamped below limit. */
    static void setLevels(int limit, int resume) {
        limit = Math.max(LIMIT_MIN, Math.min(LIMIT_MAX, limit));
        resume = Math.max(RESUME_MIN, Math.min(limit - 1, resume));
        // Each change re-runs the script with all current values. Write in the order that keeps
        // resume < limit at every step, so no intermediate run falls back to limit - 5.
        if (limit < getLimit()) {
            SystemProperties.set(PROP_RESUME, Integer.toString(resume));
            SystemProperties.set(PROP_LIMIT, Integer.toString(limit));
        } else {
            SystemProperties.set(PROP_LIMIT, Integer.toString(limit));
            SystemProperties.set(PROP_RESUME, Integer.toString(resume));
        }
    }
}
