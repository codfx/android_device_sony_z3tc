/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Process;

/** Charger connected / disconnected and boot: start or stop PausedService to match. */
public class ChargeLimitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            context.stopServiceAsUser(new Intent(context, PausedService.class),
                    Process.myUserHandle());
        } else {
            PausedService.sync(context);
        }
    }
}
