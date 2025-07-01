// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.trickypr.tpHistory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date; // Importar Date
import java.text.SimpleDateFormat; // Importar SimpleDateFormat
import java.sql.Timestamp; // Importar Timestamp

public class Teleport {
    Player player;
    Location from;
    Location to;
    Date timestamp; // Añadir campo para el timestamp

    public Teleport(Player player, Location from, Location to) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.timestamp = new Date(); // Establecer la fecha y hora actual al crear el objeto
    }

    // Nuevo constructor para cuando se carga desde la base de datos
    public Teleport(Player player, Location from, Location to, Date timestamp) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.timestamp = timestamp;
    }

    public static ArrayList<Teleport> getFromDB(Player player, Statement statement) throws SQLException {
        ArrayList<Teleport> teleports = new ArrayList<>();
        // Ordena por timestamp en orden descendente para mostrar los más recientes primero
        ResultSet resultSet = statement.executeQuery("SELECT * FROM teleports WHERE (uuid = '"+ player.getUniqueId() +"') ORDER BY timestamp DESC");

        while (resultSet.next()) {
            String fromWorldName = resultSet.getString("from_world");
            String toWorldName = resultSet.getString("to_world");
            Timestamp dbTimestamp = resultSet.getTimestamp("timestamp"); // Obtener el timestamp

            World fromWorld = Bukkit.getWorld(fromWorldName);
            World toWorld = Bukkit.getWorld(toWorldName);

            if (fromWorld != null && toWorld != null) {
                Location from = new Location(fromWorld, resultSet.getDouble("from_x"), resultSet.getDouble("from_y"), resultSet.getDouble("from_z"));
                Location to = new Location(toWorld, resultSet.getDouble("to_x"), resultSet.getDouble("to_y"), resultSet.getDouble("to_z"));
                // Usar el nuevo constructor con timestamp
                teleports.add(new Teleport(player, from, to, dbTimestamp));
            } else {
                System.out.println("Advertencia: No se encontró el mundo para un teletransporte en la base de datos. From World: " + fromWorldName + ", To World: " + toWorldName + " para el UUID: " + player.getUniqueId());
            }
        }
        return teleports;
    }

    public void add(Statement statement) throws SQLException {
        // El timestamp se genera automáticamente en la DB con DEFAULT CURRENT_TIMESTAMP,
        // así que no necesitamos insertarlo aquí manualmente.
        statement.executeUpdate("INSERT INTO teleports (uuid, from_x, from_y, from_z, from_world, to_x, to_y, to_z, to_world)"
                + "VALUES('" + player.getUniqueId() + "', "
                + from.getBlockX() + "," + from.getBlockY() + "," + from.getBlockZ() + ",'" + from.getWorld().getName() + "',"
                + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ() + ",'" + to.getWorld().getName() + "')");
    }

    // Getters para acceder a las Locations
    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    // Nuevo método para obtener el timestamp formateado
    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "Fecha desconocida";
        }
        // Formato: DD/MM/YY HH:MM AM/PM
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy hh:mm a");
        return sdf.format(timestamp);
    }
}