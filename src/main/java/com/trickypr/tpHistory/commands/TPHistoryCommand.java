// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.trickypr.tpHistory.commands;

import com.trickypr.tpHistory.TPHistory;
import com.trickypr.tpHistory.Teleport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.stream.Collectors; // Para la lista de jugadores

public class TPHistoryCommand implements CommandExecutor {

    private final TPHistory plugin;

    public TPHistoryCommand(TPHistory plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Solo jugadores pueden usar este comando para ver historiales, la consola es para listar jugadores.
        if (!(sender instanceof Player) && (args.length == 0 || (args.length == 1 && Bukkit.getPlayer(args[0]) == null))) {
            sender.sendMessage(Component.text("Este comando solo puede ser ejecutado por un jugador para ver historiales específicos o necesitas especificar un jugador.", NamedTextColor.RED));
            sender.sendMessage(Component.text("Uso: /tphist <nombre_de_jugador> o /tphist (para lista de jugadores).", NamedTextColor.YELLOW));
            return true;
        }

        Player playerSender = (Player) sender; // El jugador que ejecuta el comando

        if (args.length == 0) {
            // Si no hay argumentos, mostrar la lista de jugadores conectados (con permiso)
            if (!sender.hasPermission("tphistory.command.listplayers")) {
                sender.sendMessage(Component.text("No tienes permiso para ver la lista de jugadores.", NamedTextColor.RED));
                return true;
            }
            displayOnlinePlayers(playerSender);
            return true;
        }

        // Si hay argumentos, tratarlos como el nombre de un jugador
        String targetPlayerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName); // El jugador cuyo historial queremos ver

        if (targetPlayer == null) {
            sender.sendMessage(Component.text("No se pudo encontrar el jugador '", NamedTextColor.RED)
                    .append(Component.text(targetPlayerName, NamedTextColor.AQUA))
                    .append(Component.text("'. Asegúrate de que esté conectado o verifica la ortografía.", NamedTextColor.RED)));
            return true;
        }

        // Asegúrate de que el sender tenga permiso para ver el historial de otros jugadores
        // O si es su propio historial, siempre permitirlo.
        if (!sender.equals(targetPlayer) && !sender.hasPermission("tphistory.command.playerhistory")) {
            sender.sendMessage(Component.text("No tienes permiso para ver el historial de ", NamedTextColor.RED)
                    .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.RED)));
            return true;
        }

        displayTeleportHistory(playerSender, targetPlayer);
        return true;
    }

    // --- Métodos Auxiliares ---

    private void displayOnlinePlayers(Player sender) {
        ArrayList<String> onlinePlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(p.getName());
        }

        if (onlinePlayers.isEmpty()) {
            sender.sendMessage(Component.text("No hay jugadores conectados en este momento.", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("--- Jugadores Conectados ---", NamedTextColor.GOLD));
        Component playerList = Component.text("");
        for (int i = 0; i < onlinePlayers.size(); i++) {
            String playerName = onlinePlayers.get(i);
            playerList = playerList.append(Component.text(playerName, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.runCommand("/tphist " + playerName)) // Click para ver su historial
                    .hoverEvent(HoverEvent.showText(Component.text("Click para ver el historial de " + playerName, NamedTextColor.YELLOW))));
            if (i < onlinePlayers.size() - 1) {
                playerList = playerList.append(Component.text(", ", NamedTextColor.GRAY));
            }
        }
        sender.sendMessage(playerList);
        sender.sendMessage(Component.text("Click en un nombre para ver su historial.", NamedTextColor.GRAY));
    }

    private void displayTeleportHistory(Player sender, Player targetPlayer) {
        ArrayList<Teleport> teleports = plugin.getTeleports(targetPlayer);

        if (teleports.isEmpty()) {
            sender.sendMessage(Component.text("No hay historial de teletransportes para ", NamedTextColor.YELLOW)
                    .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                    .append(Component.text(".", NamedTextColor.YELLOW)));
            return;
        }

        sender.sendMessage(Component.text("--- Historial de Teletransportes para ", NamedTextColor.GOLD)
                .append(Component.text(targetPlayer.getName(), NamedTextColor.AQUA))
                .append(Component.text(" ---", NamedTextColor.GOLD)));

        // Mensaje de ejemplo de comando
        sender.sendMessage(Component.text("Si no puedes clickear, usa: ", NamedTextColor.GRAY)
                .append(Component.text("/tpopos [X] [Y] [Z] [mundo]", NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)));


        for (Teleport tp : teleports) {
            Location fromLoc = tp.getFrom();
            Location toLoc = tp.getTo();

            // GENERAR COMANDO /tpopos con el formato exacto
            String fromTpCommand = "/tpopos " + fromLoc.getBlockX() + " " + fromLoc.getBlockY() + " " + fromLoc.getBlockZ() + " " + fromLoc.getWorld().getName();
            String toTpCommand = "/tpopos " + toLoc.getBlockX() + " " + toLoc.getBlockY() + " " + toLoc.getBlockZ() + " " + toLoc.getWorld().getName();

            TextComponent message = Component.text("Desde: ", NamedTextColor.GRAY)
                    .append(Component.text(fromLoc.getBlockX() + " " + fromLoc.getBlockY() + " " + fromLoc.getBlockZ() + " ", NamedTextColor.AQUA))
                    .append(Component.text("[" + fromLoc.getWorld().getName() + "]", NamedTextColor.DARK_AQUA)
                            .clickEvent(ClickEvent.runCommand(fromTpCommand))
                            .hoverEvent(HoverEvent.showText(Component.text("Click para teletransportarse al origen", NamedTextColor.YELLOW))))
                    .append(Component.text(" -> A: ", NamedTextColor.GRAY))
                    .append(Component.text(toLoc.getBlockX() + " " + toLoc.getBlockY() + " " + toLoc.getBlockZ() + " ", NamedTextColor.GREEN))
                    .append(Component.text("[" + toLoc.getWorld().getName() + "]", NamedTextColor.DARK_GREEN)
                            .clickEvent(ClickEvent.runCommand(toTpCommand))
                            .hoverEvent(HoverEvent.showText(Component.text("Click para teletransportarse al destino", NamedTextColor.YELLOW))));

            sender.sendMessage(message);
        }
    }
}