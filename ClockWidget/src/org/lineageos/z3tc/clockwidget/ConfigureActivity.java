/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.clockwidget;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;

/**
 * Settings of one widget: which lines to show, 12/24-hour, clock size. Opened from the widget's edit icon
 * (long-press the widget). Every change is saved and shown on the widget at once; Done or Back closes it.
 */
public class ConfigureActivity extends Activity {

    private static final int REQUEST_CALENDAR = 1;

    private int mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetPrefs mPrefs;
    private Switch mShowEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // Until the user finishes here, a newly added widget counts as cancelled
        setResult(RESULT_CANCELED);
        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        setTitle(R.string.settings_title);
        setContentView(R.layout.configure);
        mPrefs = WidgetPrefs.load(this, mWidgetId);

        bindSwitch(R.id.show_date, mPrefs.showDate, (b, on) -> { mPrefs.showDate = on; changed(); });
        bindSwitch(R.id.show_alarm, mPrefs.showAlarm, (b, on) -> { mPrefs.showAlarm = on; changed(); });
        mShowEvent = bindSwitch(R.id.show_event, mPrefs.showEvent && hasCalendar(), (b, on) -> {
            mPrefs.showEvent = on;
            changed();
            if (on && !hasCalendar()) {
                requestPermissions(new String[] { Manifest.permission.READ_CALENDAR }, REQUEST_CALENDAR);
            }
        });

        RadioGroup format = findViewById(R.id.time_format);
        format.check(mPrefs.timeFormat == WidgetPrefs.FORMAT_12 ? R.id.format_12
                : mPrefs.timeFormat == WidgetPrefs.FORMAT_24 ? R.id.format_24 : R.id.format_system);
        format.setOnCheckedChangeListener((g, id) -> {
            mPrefs.timeFormat = id == R.id.format_12 ? WidgetPrefs.FORMAT_12
                    : id == R.id.format_24 ? WidgetPrefs.FORMAT_24 : WidgetPrefs.FORMAT_SYSTEM;
            changed();
        });

        RadioGroup size = findViewById(R.id.size);
        size.check(mPrefs.size == WidgetPrefs.SIZE_SMALL ? R.id.size_small
                : mPrefs.size == WidgetPrefs.SIZE_LARGE ? R.id.size_large : R.id.size_medium);
        size.setOnCheckedChangeListener((g, id) -> {
            mPrefs.size = id == R.id.size_small ? WidgetPrefs.SIZE_SMALL
                    : id == R.id.size_large ? WidgetPrefs.SIZE_LARGE : WidgetPrefs.SIZE_MEDIUM;
            changed();
        });

        findViewById(R.id.done).setOnClickListener(v -> finish());
        markDone();
    }

    private Switch bindSwitch(int id, boolean checked, CompoundButton.OnCheckedChangeListener l) {
        Switch s = findViewById(id);
        s.setChecked(checked);
        s.setOnCheckedChangeListener(l);
        return s;
    }

    private boolean hasCalendar() {
        return checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    private void changed() {
        mPrefs.save(this, mWidgetId);
        ClockWidgetProvider.updateAll(this);
    }

    private void markDone() {
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == REQUEST_CALENDAR && !hasCalendar()) {
            // Without the permission the event line cannot be shown
            mShowEvent.setChecked(false);
        }
        changed();
    }
}
