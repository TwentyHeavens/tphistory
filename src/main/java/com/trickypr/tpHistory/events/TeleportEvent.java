package com.trickypr.tpHistory.events;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import com.trickypr.tpHistory.objects.Teleport; // ¡Necesitas importar tu objeto Teleport aquí!

import org.bukkit.Location; // Necesario para Location
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player; // Necesario para Player

public class TeleportEvent implements Listener {

    private final TeleportTracerPlugin plugin;

    public TeleportEvent(TeleportTracerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Ignora teletransportaciones que son el resultado de un cambio de mundo
        // o si el destino es nulo (aunque esto es raro en PlayerTeleportEvent)
        // NOTA: event.getFrom().equals(event.getTo()) puede ser true si solo hay cambios de pitch/yaw.
        // Para teletransportes reales (cambio de posición o mundo), esto debería ser suficiente.
        if (event.getFrom().equals(event.getTo())) {
            return; // No se movió de posición real
        }

        // Obtener el jugador, ubicaciones y timestamp
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        long timestamp = System.currentTimeMillis(); // Timestamp actual

        // Crear un objeto Teleport con los datos
        // Asegúrate de que tu constructor de Teleport acepte PlayerName, Timestamp, FromLocation, ToLocation
        Teleport teleport = new Teleport(player.getName(), timestamp, from, to);

        // Llamar al método logTeleport del plugin principal para guardar el registro
        plugin.logTeleport(teleport);
    }
}