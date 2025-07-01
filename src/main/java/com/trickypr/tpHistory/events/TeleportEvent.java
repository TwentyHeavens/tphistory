// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.trickypr.tpHistory.events;

import com.trickypr.tpHistory.TPHistory;
import com.trickypr.tpHistory.Teleport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TeleportEvent implements Listener {
    TPHistory plugin;

    public TeleportEvent(TPHistory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Teleport teleport = new Teleport(e.getPlayer(), e.getFrom(), e.getTo());
        plugin.logTeleport(teleport);
    }

}
