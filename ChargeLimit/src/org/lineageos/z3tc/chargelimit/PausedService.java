/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.Process;

/**
 * Shows "Charging paused at X%" while the charge limit holds charging off.
 *
 * Runs only while the charger is connected and the limit is on, and is driven by the battery
 * broadcast alone: no polling, no alarms, no wake locks. ChargeLimitReceiver starts it on
 * power connected / boot and stops it on power disconnected; it also stops itself as soon as
 * a battery broadcast shows the charger gone or the limit off.
 */
public class PausedService extends Service {
    private static final String CHANNEL_ID = "paused";
    private static final int NOTIFICATION_ID = 1;
    static final String ACTION_REFRESH = "org.lineageos.z3tc.chargelimit.REFRESH";
    private static final String ACTION_DISMISSED = "org.lineageos.z3tc.chargelimit.DISMISSED";

    private NotificationManager mNm;
    private boolean mRegistered;
    private int mShownLevel = -1;    // level in the visible notification, -1 = none
    private boolean mDismissed;      // swiped away during the current pause

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update(intent);
        }
    };

    /** Starts or stops the service to match the charger and the limit setting. */
    static void sync(Context context) {
        Context app = context.getApplicationContext();
        Intent battery = app.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        boolean plugged = battery != null
                && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        Intent service = new Intent(app, PausedService.class);
        // The app runs as the system uid: name the user, or every call logs a warning.
        if (plugged && ChargeLimit.isEnabled()) {
            app.startServiceAsUser(service.setAction(ACTION_REFRESH), Process.myUserHandle());
        } else {
            app.stopServiceAsUser(service, Process.myUserHandle());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.paused_channel), NotificationManager.IMPORTANCE_MIN);
        channel.setDescription(getString(R.string.paused_channel_description));
        channel.setShowBadge(false);
        mNm.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISMISSED.equals(intent.getAction())) {
            mDismissed = true;
            mShownLevel = -1;
            if (!mRegistered) {
                stopSelf();  // started only by the dismissal, e.g. just after unplugging
                return START_NOT_STICKY;
            }
            return START_STICKY;
        }
        if (!mRegistered) {
            // The battery broadcast is sticky: registering delivers the current state at once.
            registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            mRegistered = true;
        } else {
            update(registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
        }
        return START_STICKY;
    }

    private void update(Intent battery) {
        if (battery == null) {
            return;
        }
        if (battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) == 0 || !ChargeLimit.isEnabled()) {
            stopSelf();
            return;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        // While the limit holds charging off the driver reports "Discharging" with the charger
        // still connected (Settings turns that into "Not charging"); accept both.
        boolean paused = (status == BatteryManager.BATTERY_STATUS_DISCHARGING
                        || status == BatteryManager.BATTERY_STATUS_NOT_CHARGING)
                && battery.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
                        == BatteryManager.BATTERY_HEALTH_GOOD
                && level >= ChargeLimit.getResume();
        if (!paused) {
            mDismissed = false;
            cancel();
        } else if (!mDismissed && level != mShownLevel) {
            show(level);
        }
    }

    private void show(int level) {
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, ChargeLimitActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE);
        PendingIntent dismissed = PendingIntent.getService(this, 0,
                new Intent(this, PausedService.class).setAction(ACTION_DISMISSED),
                PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_charge_limit)
                .setContentTitle(getString(R.string.paused_title, level))
                .setContentText(getString(R.string.paused_text, ChargeLimit.getResume()))
                .setContentIntent(open)
                .setDeleteIntent(dismissed)
                .setCategory(Notification.CATEGORY_STATUS)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setLocalOnly(true)
                .build();
        mNm.notify(NOTIFICATION_ID, n);
        mShownLevel = level;
    }

    private void cancel() {
        if (mShownLevel != -1) {
            mNm.cancel(NOTIFICATION_ID);
            mShownLevel = -1;
        }
    }

    @Override
    public void onDestroy() {
        if (mRegistered) {
            unregisterReceiver(mBatteryReceiver);
            mRegistered = false;
        }
        mNm.cancel(NOTIFICATION_ID);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
