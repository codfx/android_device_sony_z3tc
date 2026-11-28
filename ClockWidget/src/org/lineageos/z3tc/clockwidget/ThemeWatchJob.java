/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.clockwidget;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.provider.Settings;

/**
 * Re-draws the widget shortly after the wallpaper changes, so light/dark text follows it. Apps may not receive
 * WALLPAPER_CHANGED in the background, but Android 12 rewrites the theme setting below whenever the wallpaper
 * colours change; a content-trigger job on it runs about a second later. It never wakes the device.
 * Content-trigger jobs fire once, so the job schedules itself again each time.
 */
public class ThemeWatchJob extends JobService {

    private static final int JOB_ID = 1;
    private static final String THEME_SETTING = "theme_customization_overlay_packages";

    static void schedule(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js == null || js.getPendingJob(JOB_ID) != null) {
            return;
        }
        Uri uri = Settings.Secure.getUriFor(THEME_SETTING);
        js.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(context, ThemeWatchJob.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(uri, 0))
                .setTriggerContentUpdateDelay(500)
                .setTriggerContentMaxDelay(2000)
                .build());
    }

    static void cancel(Context context) {
        JobScheduler js = context.getSystemService(JobScheduler.class);
        if (js != null) {
            js.cancel(JOB_ID);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        ClockWidgetProvider.updateAll(this); // schedules the next watch as well
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
