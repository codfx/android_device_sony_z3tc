/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.clockwidget;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Large clock with an "at a glance" block: date, next alarm, next calendar event (local data only).
 *
 * No background work: the clock and the date are TextClocks (they update themselves while visible). The
 * alarm line is refreshed by NEXT_ALARM_CLOCK_CHANGED. The event line is refreshed by a NON-wakeup alarm at
 * the next moment it can change (event start/end, at most 30 min ahead), which only runs while the device
 * is awake anyway, plus the time/locale broadcasts.
 */
public class ClockWidgetProvider extends AppWidgetProvider {

    static final String ACTION_REFRESH = "org.lineageos.z3tc.clockwidget.REFRESH";

    private static final long LOOKAHEAD_MS = 24L * 60 * 60 * 1000;
    private static final long MAX_REFRESH_MS = 30L * 60 * 1000;
    private static final String DESKCLOCK = "com.android.deskclock";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            updateAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        updateAll(context);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WidgetPrefs.delete(context, id);
        }
    }

    @Override
    public void onDisabled(Context context) {
        context.getSystemService(AlarmManager.class).cancel(refreshIntent(context));
        ThemeWatchJob.cancel(context);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        if (ids.length == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long nextRefresh = now + MAX_REFRESH_MS;
        boolean darkText = wallpaperSupportsDarkText(context);
        AlarmManager am = context.getSystemService(AlarmManager.class);
        AlarmManager.AlarmClockInfo alarm = am.getNextAlarmClock();
        boolean hasCalendar = context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
        Event event = null;
        boolean eventQueried = false;

        for (int id : ids) {
            WidgetPrefs prefs = WidgetPrefs.load(context, id);
            RemoteViews views = new RemoteViews(context.getPackageName(),
                    darkText ? R.layout.widget_dark : R.layout.widget);
            views.setOnClickPendingIntent(R.id.root, clockIntent(context));

            // Clock: size and 12/24-hour
            views.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_SP, prefs.clockSp());
            String f12 = prefs.timeFormat == WidgetPrefs.FORMAT_24 ? "HH:mm" : "h:mm";
            String f24 = prefs.timeFormat == WidgetPrefs.FORMAT_12 ? "h:mm" : "HH:mm";
            views.setCharSequence(R.id.clock, "setFormat12Hour", f12);
            views.setCharSequence(R.id.clock, "setFormat24Hour", f24);

            // Date
            views.setViewVisibility(R.id.date, prefs.showDate ? View.VISIBLE : View.GONE);

            // Next alarm (no gap before it when the date is hidden, so the row stays centred)
            if (prefs.showAlarm && alarm != null) {
                views.setTextViewText(R.id.alarm, formatAlarm(context, prefs, alarm.getTriggerTime(), now));
                views.setViewVisibility(R.id.alarm_group, View.VISIBLE);
                views.setViewLayoutMargin(R.id.alarm_group, RemoteViews.MARGIN_START,
                        prefs.showDate ? 14f : 0f, TypedValue.COMPLEX_UNIT_DIP);
                views.setOnClickPendingIntent(R.id.alarm_group, PendingIntent.getActivity(context, 1,
                        new Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            } else {
                views.setViewVisibility(R.id.alarm_group, View.GONE);
            }
            views.setViewVisibility(R.id.glance_row,
                    prefs.showDate || (prefs.showAlarm && alarm != null) ? View.VISIBLE : View.GONE);

            // Next calendar event (only with the optional calendar permission)
            views.setViewVisibility(R.id.event_group, View.GONE);
            if (prefs.showEvent && hasCalendar) {
                if (!eventQueried) {
                    event = nextEvent(context, now);
                    eventQueried = true;
                }
                if (event != null) {
                    String text = event.allDay
                            ? context.getString(R.string.event_all_day, event.title)
                            : context.getString(R.string.event_format,
                                    timeFormat(context, prefs).format(event.begin), event.title);
                    views.setTextViewText(R.id.event, text);
                    views.setViewVisibility(R.id.event_group, View.VISIBLE);
                    views.setOnClickPendingIntent(R.id.event_group, PendingIntent.getActivity(context, 2,
                            new Intent(Intent.ACTION_VIEW,
                                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
                    long change = event.begin > now ? event.begin : event.end;
                    if (change > now && change < nextRefresh) {
                        nextRefresh = change;
                    }
                }
            }
            manager.updateAppWidget(id, views);
        }
        // RTC (not RTC_WAKEUP): never wakes the device; runs the next time it is awake after this time.
        am.set(AlarmManager.RTC, nextRefresh, refreshIntent(context));
        // Light/dark text follows wallpaper changes
        ThemeWatchJob.schedule(context);
    }

    /** True when the home screen wallpaper is light enough for dark text (Android's own wallpaper hint). */
    private static boolean wallpaperSupportsDarkText(Context context) {
        try {
            WallpaperColors colors = WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
            return colors != null
                    && (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static java.text.DateFormat timeFormat(Context context, WidgetPrefs prefs) {
        if (prefs.timeFormat == WidgetPrefs.FORMAT_12) {
            return new SimpleDateFormat("h:mm a", Locale.getDefault());
        }
        if (prefs.timeFormat == WidgetPrefs.FORMAT_24) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault());
        }
        return DateFormat.getTimeFormat(context);
    }

    private static String formatAlarm(Context context, WidgetPrefs prefs, long when, long now) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(when);
        Calendar n = Calendar.getInstance();
        n.setTimeInMillis(now);
        boolean within24h = when - now < LOOKAHEAD_MS;
        String time = timeFormat(context, prefs).format(when);
        if (within24h) {
            return time;
        }
        String day = DateFormat.format("EEE", a).toString();
        return day + " " + time;
    }

    private static final class Event {
        long id;
        long begin;
        long end;
        boolean allDay;
        String title;
    }

    /** Next event that has not ended yet and starts within 24 h; all-day events only for today. */
    private static Event nextEvent(Context context, long now) {
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, now - LOOKAHEAD_MS);
        ContentUris.appendId(b, now + LOOKAHEAD_MS);
        String[] projection = {
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.TITLE,
        };
        String selection = CalendarContract.Instances.END + " > ? AND "
                + CalendarContract.Instances.VISIBLE + " = 1";
        String[] args = { Long.toString(now) };
        Event best = null;
        try (Cursor c = context.getContentResolver().query(b.build(), projection, selection, args,
                CalendarContract.Instances.BEGIN + " ASC")) {
            if (c == null) {
                return null;
            }
            while (c.moveToNext()) {
                Event e = new Event();
                e.id = c.getLong(0);
                e.begin = c.getLong(1);
                e.end = c.getLong(2);
                e.allDay = c.getInt(3) != 0;
                e.title = c.getString(4);
                if (e.title == null || e.title.isEmpty()) {
                    continue;
                }
                if (e.allDay) {
                    // All-day instances are in UTC; show only the one covering today
                    if (!isTodayUtc(e.begin, e.end, now)) {
                        continue;
                    }
                } else if (e.begin > now + LOOKAHEAD_MS) {
                    continue;
                }
                // Prefer a timed event over an all-day one
                if (best == null || (best.allDay && !e.allDay)) {
                    best = e;
                    if (!e.allDay) {
                        break;
                    }
                }
            }
        } catch (SecurityException | IllegalArgumentException ignored) {
            return null;
        }
        return best;
    }

    private static boolean isTodayUtc(long beginUtc, long endUtc, long now) {
        Calendar local = Calendar.getInstance();
        local.setTimeInMillis(now);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        long todayUtc = utc.getTimeInMillis();
        return beginUtc <= todayUtc && endUtc > todayUtc;
    }

    private static PendingIntent clockIntent(Context context) {
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(DESKCLOCK);
        if (launch == null) {
            launch = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent refreshIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_REFRESH).setClass(context, ClockWidgetProvider.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
