/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.clockwidget;

import android.content.Context;
import android.content.SharedPreferences;

/** Settings of one widget instance (each widget on the home screen has its own). */
final class WidgetPrefs {

    static final int FORMAT_SYSTEM = 0;
    static final int FORMAT_12 = 1;
    static final int FORMAT_24 = 2;

    static final int SIZE_SMALL = 0;
    static final int SIZE_MEDIUM = 1;
    static final int SIZE_LARGE = 2;

    /** Clock text size in sp for SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE. */
    private static final float[] CLOCK_SP = { 72f, 90f, 108f };

    boolean showDate = true;
    boolean showAlarm = true;
    boolean showEvent = true;
    int timeFormat = FORMAT_SYSTEM;
    int size = SIZE_MEDIUM;

    private static SharedPreferences prefs(Context context, int widgetId) {
        return context.getSharedPreferences("widget_" + widgetId, Context.MODE_PRIVATE);
    }

    static WidgetPrefs load(Context context, int widgetId) {
        SharedPreferences p = prefs(context, widgetId);
        WidgetPrefs w = new WidgetPrefs();
        w.showDate = p.getBoolean("show_date", true);
        w.showAlarm = p.getBoolean("show_alarm", true);
        w.showEvent = p.getBoolean("show_event", true);
        w.timeFormat = p.getInt("time_format", FORMAT_SYSTEM);
        w.size = p.getInt("size", SIZE_MEDIUM);
        return w;
    }

    void save(Context context, int widgetId) {
        prefs(context, widgetId).edit()
                .putBoolean("show_date", showDate)
                .putBoolean("show_alarm", showAlarm)
                .putBoolean("show_event", showEvent)
                .putInt("time_format", timeFormat)
                .putInt("size", size)
                .apply();
    }

    static void delete(Context context, int widgetId) {
        context.deleteSharedPreferences("widget_" + widgetId);
    }

    float clockSp() {
        return CLOCK_SP[Math.max(0, Math.min(size, CLOCK_SP.length - 1))];
    }
}
