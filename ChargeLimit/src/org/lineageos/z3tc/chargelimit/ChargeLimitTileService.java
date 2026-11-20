/*
 * Copyright (C) 2026 codfx
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.z3tc.chargelimit;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: tap toggles the charge limit; long press opens the settings screen. */
public class ChargeLimitTileService extends TileService {

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
