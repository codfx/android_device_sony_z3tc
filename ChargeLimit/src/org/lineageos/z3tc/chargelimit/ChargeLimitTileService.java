/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.content.ComponentName;
import android.content.Context;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: tap toggles the charge limit; long press opens the settings screen. */
public class ChargeLimitTileService extends TileService {
    // SystemUI keeps this service bound and does not call onStartListening again for every
    // shade opening, so changes made in the settings screen are pushed to the tile from there.
    private static ChargeLimitTileService sInstance;

    /** Shows the current settings on the tile; called after changes in the settings screen. */
    static void refresh(Context context) {
        if (sInstance != null) {
            sInstance.update(ChargeLimit.isEnabled());
        }
        requestListeningState(context, new ComponentName(context, ChargeLimitTileService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        update(ChargeLimit.isEnabled());
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean enable = !ChargeLimit.isEnabled();
        ChargeLimit.setEnabled(enable);
        PausedService.sync(this);
        update(enable);
    }

    private void update(boolean enabled) {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setSubtitle(enabled
                ? getString(R.string.tile_on, ChargeLimit.getLimit())
                : getString(R.string.tile_off));
        tile.updateTile();
    }
}
