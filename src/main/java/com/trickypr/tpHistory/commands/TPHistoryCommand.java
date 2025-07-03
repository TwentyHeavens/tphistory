package com.trickypr.tpHistory.commands;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import com.trickypr.tpHistory.objects.Teleport;
import com.trickypr.tpHistory.store.SQLite;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class TPHistoryCommand implements CommandExecutor, TabCompleter {

    private final TeleportTracerPlugin plugin;
    private final SQLite database;
    private static final int ENTRIES_PER_PAGE = 5; // Cantidad de entradas por página
    private SimpleDateFormat dateFormat;

    public TPHistoryCommand(TeleportTracerPlugin plugin, SQLite database) {
        this.plugin = plugin;
        this.database = database;
        String dateFormatString = plugin.getPluginConfig().getString("date-format", "dd-MM-yyyy hh:mm a");
        this.dateFormat = new SimpleDateFormat(dateFormatString);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // PERMISO GENERAL: Si no tienen este permiso, NO APARECE NADA y el comando simplemente termina.
        if (!sender.hasPermission("tphistory.command.use")) {
            return true;
        }

        // --- MANEJO DEL SUBCOMANDO INTERNO PARA TELETRANSPORTAR ---
        // Este es un comando interno que se ejecuta cuando el jugador hace clic en un mensaje de historial.
        // No está destinado a ser escrito directamente por los jugadores.
        // El formato será: /<command_alias> teleportinternal <worldName> <x> <y> <z>
        if (args.length == 5 && args[0].equalsIgnoreCase("teleportinternal")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLangMessage("player-only-command"));
                return true;
            }
            // Permiso específico para teletransportarse a través del historial
            if (!sender.hasPermission("tphistory.command.teleport")) {
                sender.sendMessage(plugin.getLangMessage("no-permission"));
                return true;
            }

            Player player = (Player) sender;
            try {
                String worldName = args[1];
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);

                World targetWorld = Bukkit.getWorld(worldName);
                if (targetWorld == null) {
                    sender.sendMessage(plugin.getLangMessage("world-not-found").replace("%world%", worldName));
                    return true;
                }

                Location targetLocation = new Location(targetWorld, x, y, z);

                // IMPORTANTE: La teletransportación DEBE ocurrir en el hilo principal del servidor.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.teleport(targetLocation);
                    player.sendMessage(plugin.getLangMessage("teleported-success").replace("%location%", String.format("%.1f, %.1f, %.1f en %s", x, y, z, worldName)));
                });

            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getLangMessage("invalid-teleport-coords"));
                plugin.getLogger().log(Level.WARNING, "Error al parsear coordenadas para teletransporte interno: " + e.getMessage());
            }
            return true;
        }
        // --- FIN DEL MANEJO DEL SUBCOMANDO INTERNO ---


        // Manejo del subcomando "reload"
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tphistory.command.reload")) {
                sender.sendMessage(plugin.getLangMessage("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            String dateFormatString = plugin.getPluginConfig().getString("date-format", "dd-MM-yyyy hh:mm a");
            this.dateFormat = new SimpleDateFormat(dateFormatString);
            sender.sendMessage(plugin.getLangMessage("reload-success"));
            return true;
        }

        String targetPlayerName = null;
        int page = 1; // Página por defecto es 1

        // Parseo de argumentos: "player <nombre>" y "[page]"
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("player") && i + 1 < args.length) {
                targetPlayerName = args[i + 1];
                i++; // Saltar el siguiente argumento ya que es el nombre del jugador

                if (!sender.hasPermission("tphistory.command.listplayers")) {
                    sender.sendMessage(plugin.getLangMessage("no-permission"));
                    return true;
                }
            } else {
                try {
                    int parsedPage = Integer.parseInt(args[i]);

                    if (targetPlayerName != null && !sender.getName().equalsIgnoreCase(targetPlayerName) && !sender.hasPermission("tphistory.command.playerpages")) {
                        sender.sendMessage(plugin.getLangMessage("no-permission"));
                        return true;
                    }
                    page = parsedPage;

                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLangMessage("invalid-arguments")
                                    .replace("%usage%", plugin.getLangMessageRaw("tphist-usage")
                                            .replace("%command_alias%", plugin.getCommandAlias())));
                    return true;
                }
            }
        }

        if (targetPlayerName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLangMessage("player-only-command"));
                return true;
            }
            targetPlayerName = sender.getName(); // Por defecto, es el propio jugador
        }

        final String finalTargetPlayerName = targetPlayerName;
        final int finalPage = page;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int totalEntries = database.getTeleportHistoryCount(finalTargetPlayerName);
                if (totalEntries == 0) {
                    sender.sendMessage(plugin.getLangMessage("no-history-found").replace("%player%", finalTargetPlayerName));
                    return;
                }

                int totalPages = (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE);

                if (finalPage < 1 || finalPage > totalPages) {
                    sender.sendMessage(plugin.getLangMessage("page-out-of-bounds")
                                    .replace("%requested_page%", String.valueOf(finalPage))
                                    .replace("%total_pages%", String.valueOf(totalPages))
                                    .replace("%player%", finalTargetPlayerName));
                    return;
                }

                int offset = (finalPage - 1) * ENTRIES_PER_PAGE;
                ResultSet rs = database.getTeleportHistory(finalTargetPlayerName, ENTRIES_PER_PAGE, offset);

                List<Teleport> teleportsOnPage = new ArrayList<>();
                while (rs.next()) {
                    String pName = rs.getString("player_name");
                    long timestamp = rs.getLong("timestamp");
                    String fromWorldName = rs.getString("world_from");
                    double fromX = rs.getDouble("x_from");
                    double fromY = rs.getDouble("y_from");
                    double fromZ = rs.getDouble("z_from");
                    String toWorldName = rs.getString("world_to");
                    double toX = rs.getDouble("x_to");
                    double toY = rs.getDouble("y_to");
                    double toZ = rs.getDouble("z_to");

                    World fromWorld = Bukkit.getWorld(fromWorldName);
                    Location fromLoc = (fromWorld != null) ? new Location(fromWorld, fromX, fromY, fromZ) : null;

                    World toWorld = Bukkit.getWorld(toWorldName);
                    Location toLoc = (toWorld != null) ? new Location(toWorld, toX, toY, toZ) : null;

                    if (fromLoc != null && toLoc != null) {
                        teleportsOnPage.add(new Teleport(pName, timestamp, fromLoc, toLoc));
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Saltando entrada de teletransporte debido a mundo desconocido: FromWorld=" + fromWorldName + ", ToWorld=" + toWorldName + " para el jugador " + pName + ". Esta entrada no se mostrará.");
                    }
                }
                rs.close();

                if (teleportsOnPage.isEmpty() && totalEntries > 0) {
                    sender.sendMessage(plugin.getLangMessage("no-entries-on-page")
                                    .replace("%player%", finalTargetPlayerName)
                                    .replace("%page%", String.valueOf(finalPage)));
                    return;
                }

                String header = plugin.getLangMessageRaw("player-history-header")
                        .replace("%player%", finalTargetPlayerName);
                sender.sendMessage(header);

                // Info sobre cómo cliquear para teletransportarse
                if (sender instanceof Player) {
                    TextComponent clickInfoBase = new TextComponent(plugin.getLangMessageRaw("teleport-click-info-base"));
                    TextComponent clickInfoCommand = new TextComponent(plugin.getLangMessageRaw("teleport-click-info-command"));
                    clickInfoBase.addExtra(clickInfoCommand);
                    ((Player) sender).spigot().sendMessage(clickInfoBase);
                } else {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-manual-info"));
                }

                if (!teleportsOnPage.isEmpty()) {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                }

                for (int i = 0; i < teleportsOnPage.size(); i++) {
                    Teleport teleport = teleportsOnPage.get(i);
                    String formattedTimestamp = dateFormat.format(new Date(teleport.getTimestamp()));

                    // Componente principal de la línea
                    TextComponent lineComponent = new TextComponent();

                    // Parte del Timestamp
                    TextComponent timestampPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-timestamp").replace("%timestamp%", formattedTimestamp));
                    lineComponent.addExtra(timestampPart);

                    // Parte de las coordenadas FROM
                    String fromCoords = String.format(plugin.getLangMessageRaw("teleport-entry-coords"),
                            teleport.getFromX(), teleport.getFromY(), teleport.getFromZ());
                    TextComponent fromCoordsPart = new TextComponent(fromCoords);
                    if (sender instanceof Player && sender.hasPermission("tphistory.command.teleport")) {
                        String hoverTextFrom = plugin.getLangMessageRaw("hover-from-coords")
                                .replace("%x%", String.format("%.0f", teleport.getFromX()))
                                .replace("%y%", String.format("%.0f", teleport.getFromY()))
                                .replace("%z%", String.format("%.0f", teleport.getFromZ()))
                                .replace("%world%", teleport.getFromWorld());
                        fromCoordsPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverTextFrom)));

                        // ClickEvent para teletransportarse al FROM
                        String teleportFromCommand = "/" + plugin.getCommandAlias() + " teleportinternal " +
                                teleport.getFromWorld() + " " +
                                String.format("%.0f", teleport.getFromX()) + " " +
                                String.format("%.0f", teleport.getFromY()) + " " +
                                String.format("%.0f", teleport.getFromZ());
                        fromCoordsPart.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportFromCommand));
                    }
                    lineComponent.addExtra(fromCoordsPart);

                    // Parte del mundo FROM
                    TextComponent fromWorldPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-world").replace("%world%", teleport.getFromWorld()));
                    lineComponent.addExtra(fromWorldPart);

                    // Flecha de separación
                    TextComponent arrowPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-arrow"));
                    lineComponent.addExtra(arrowPart);

                    // Parte de las coordenadas TO
                    String toCoords = String.format(plugin.getLangMessageRaw("teleport-entry-coords-to"),
                            teleport.getToX(), teleport.getToY(), teleport.getToZ());
                    TextComponent toCoordsPart = new TextComponent(toCoords);
                    if (sender instanceof Player && sender.hasPermission("tphistory.command.teleport")) {
                        String hoverTextTo = plugin.getLangMessageRaw("hover-to-coords")
                                .replace("%x%", String.format("%.0f", teleport.getToX()))
                                .replace("%y%", String.format("%.0f", teleport.getToY()))
                                .replace("%z%", String.format("%.0f", teleport.getToZ()))
                                .replace("%world%", teleport.getToWorld());
                        toCoordsPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverTextTo)));

                        // ClickEvent para teletransportarse al TO
                        String teleportToCommand = "/" + plugin.getCommandAlias() + " teleportinternal " +
                                teleport.getToWorld() + " " +
                                String.format("%.0f", teleport.getToX()) + " " +
                                String.format("%.0f", teleport.getToY()) + " " +
                                String.format("%.0f", teleport.getToZ());
                        toCoordsPart.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportToCommand));
                    }
                    lineComponent.addExtra(toCoordsPart);

                    // Parte del mundo TO
                    TextComponent toWorldPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-world-to").replace("%world%", teleport.getToWorld()));
                    lineComponent.addExtra(toWorldPart);

                    // Envía la línea completa al jugador
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(lineComponent);
                    } else {
                        sender.sendMessage(lineComponent.toPlainText());
                    }

                    if (i < teleportsOnPage.size() - 1) {
                        sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                    }
                }

                if (!teleportsOnPage.isEmpty()) {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                }

                // Lógica de paginación
                TextComponent paginationComponent = new TextComponent();

                String pageInfo = plugin.getLangMessageRaw("pagination-info")
                        .replace("%current_page%", String.valueOf(finalPage))
                        .replace("%total_pages%", String.valueOf(totalPages));
                paginationComponent.addExtra(pageInfo);

                if (finalPage > 1) {
                    if (!paginationComponent.toPlainText().trim().isEmpty()) {
                        paginationComponent.addExtra(plugin.getLangMessageRaw("pagination-separator"));
                    }
                    TextComponent previousPage = new TextComponent(plugin.getLangMessageRaw("pagination-previous"));
                    String prevCommand = "/" + plugin.getCommandAlias() + " ";
                    if (!finalTargetPlayerName.equals(sender.getName())) {
                        prevCommand += "player " + finalTargetPlayerName + " ";
                    }
                    prevCommand += (finalPage - 1);
                    previousPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prevCommand));
                    if (sender instanceof Player) {
                        previousPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getLangMessageRaw("hover-pagination-previous"))));
                    }
                    paginationComponent.addExtra(previousPage);
                }

                if (finalPage < totalPages) {
                    if (!paginationComponent.toPlainText().trim().isEmpty()) {
                        paginationComponent.addExtra(plugin.getLangMessageRaw("pagination-separator"));
                    }
                    TextComponent nextPage = new TextComponent(plugin.getLangMessageRaw("pagination-next"));
                    String nextCommand = "/" + plugin.getCommandAlias() + " ";
                    if (!finalTargetPlayerName.equals(sender.getName())) {
                        nextCommand += "player " + finalTargetPlayerName + " ";
                    }
                    nextCommand += (finalPage + 1);
                    nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, nextCommand));
                    if (sender instanceof Player) {
                        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getLangMessageRaw("hover-pagination-next"))));
                    }
                    paginationComponent.addExtra(nextPage);
                }

                if (!paginationComponent.toPlainText().trim().isEmpty()) {
                    if (sender instanceof Player) {
                        ((Player) sender).spigot().sendMessage(paginationComponent);
                    } else {
                        sender.sendMessage(paginationComponent.toPlainText());
                    }
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error recuperando el historial de teletransporte para " + finalTargetPlayerName, e);
                sender.sendMessage(plugin.getLangMessage("error-retrieving-history"));
            }
        });

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("tphistory.command.use")) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("tphistory.command.listplayers")) {
                completions.add("player");
            }
            if (sender.hasPermission("tphistory.command.reload")) {
                completions.add("reload");
            }
            List<String> filteredCompletions = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filteredCompletions.add(s);
                }
            }
            return filteredCompletions;

        } else if (args.length == 2 && args[0].equalsIgnoreCase("player") && sender.hasPermission("tphistory.command.listplayers")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}