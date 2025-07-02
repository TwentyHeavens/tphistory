// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.trickypr.tpHistory.store;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class Database {
    protected final TeleportTracerPlugin plugin; // ✅ Corregido tipo
    protected final String dbName;

    @Nullable
    protected Connection connection;

    public Database(TeleportTracerPlugin plugin, String dbName) { // ✅ Cambiado tipo
        this.plugin = plugin;
        this.dbName = dbName;
    }

    @NotNull
    public abstract Connection getConnection();

    public Statement getStatement() throws SQLException {
        return getConnection().createStatement();
    }

    public void close() throws SQLException {
        if (connection == null) return;

        connection.close();
        connection = null;
    }

    public void handleException(String context, SQLException e) {
        plugin.getLogger().severe(context + ". Exiting");
        plugin.getLogger().severe(e.getMessage());
        plugin.getServer().getPluginManager().disablePlugin(plugin);
    }
}