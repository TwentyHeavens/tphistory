package com.trickypr.tpHistory.commands;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import com.trickypr.tpHistory.objects.Teleport;
import com.trickypr.tpHistory.store.SQLite;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.logging.Level;

public class TPHistoryCommand implements CommandExecutor, TabCompleter {

    private final TeleportTracerPlugin plugin;
    private final SQLite database;
    private static final int ENTRIES_PER_PAGE = 5; // Cantidad de entradas por página
    private SimpleDateFormat dateFormat; // Ahora no es final y se inicializa en el constructor

    public TPHistoryCommand(TeleportTracerPlugin plugin, SQLite database) {
        this.plugin = plugin;
        this.database = database;
        // Cargar el formato de fecha desde la configuración del plugin
        // Si no se encuentra, usar un valor por defecto
        String dateFormatString = plugin.getPluginConfig().getString("date-format", "dd-MM-yyyy hh:mm a");
        this.dateFormat = new SimpleDateFormat(dateFormatString);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // PERMISO GENERAL: Si no tienen este permiso, NO APARECE NADA y el comando simplemente termina.
        if (!sender.hasPermission("tphistory.command.use")) {
            // Se ha eliminado la línea sender.sendMessage(plugin.getLangMessage("no-permission"));
            return true; // Termina la ejecución del comando silenciosamente
        }

        // Manejo del subcomando "reload"
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Solo se llega aquí si ya tienen tphistory.command.use
            // Se necesita un permiso específico para recargar.
            if (!sender.hasPermission("tphistory.command.reload")) {
                sender.sendMessage(plugin.getLangMessage("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            // Actualizar el formato de fecha después de recargar la configuración
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

                // Verificar permiso para ver historial de otros jugadores
                if (!sender.hasPermission("tphistory.command.listplayers")) {
                    sender.sendMessage(plugin.getLangMessage("no-permission"));
                    return true;
                }
            } else {
                try {
                    // Si el argumento no es "player" ni su valor, intenta parsearlo como número de página
                    int parsedPage = Integer.parseInt(args[i]);
                    
                    // Verificar permiso para especificar página al ver historial de otros
                    // Solo se aplica si se está viendo el historial de OTRO jugador y se especifica una página
                    if (targetPlayerName != null && !sender.getName().equalsIgnoreCase(targetPlayerName) && !sender.hasPermission("tphistory.command.playerpages")) {
                            sender.sendMessage(plugin.getLangMessage("no-permission"));
                            return true;
                    }
                    page = parsedPage;

                } catch (NumberFormatException e) {
                    // Si el argumento no es "player" ni un número válido, es un uso inválido
                    sender.sendMessage(plugin.getLangMessage("invalid-arguments")
                                .replace("%usage%", plugin.getLangMessageRaw("tphist-usage")
                                    .replace("%command_alias%", plugin.getCommandAlias())));
                    return true;
                }
            }
        }

        // Si no se especificó un jugador, el sender debe ser un jugador para ver su propio historial
        if (targetPlayerName == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getLangMessage("player-only-command"));
                return true;
            }
            targetPlayerName = sender.getName(); // Por defecto, es el propio jugador
        }

        final String finalTargetPlayerName = targetPlayerName;
        final int finalPage = page;

        // Ejecutar la consulta de la base de datos de forma asíncrona para no congelar el servidor
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                int totalEntries = database.getTeleportHistoryCount(finalTargetPlayerName);
                if (totalEntries == 0) {
                    sender.sendMessage(plugin.getLangMessage("no-history-found").replace("%player%", finalTargetPlayerName));
                    return;
                }

                int totalPages = (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE);

                // Validar el número de página solicitado
                if (finalPage < 1 || finalPage > totalPages) {
                    sender.sendMessage(plugin.getLangMessage("page-out-of-bounds")
                                .replace("%requested_page%", String.valueOf(finalPage))
                                .replace("%total_pages%", String.valueOf(totalPages))
                                .replace("%player%", finalTargetPlayerName));
                    return;
                }

                int offset = (finalPage - 1) * ENTRIES_PER_PAGE;
                // Obtener el historial paginado desde la base de datos
                ResultSet rs = database.getTeleportHistory(finalTargetPlayerName, ENTRIES_PER_PAGE, offset);

                List<Teleport> teleportsOnPage = new ArrayList<>();
                while (rs.next()) {
                    // Extraer los datos de cada teletransporte del ResultSet
                    String playerName = rs.getString("player_name");
                    long timestamp = rs.getLong("timestamp");
                    String fromWorldName = rs.getString("world_from");
                    double fromX = rs.getDouble("x_from");
                    double fromY = rs.getDouble("y_from");
                    double fromZ = rs.getDouble("z_from");
                    String toWorldName = rs.getString("world_to");
                    double toX = rs.getDouble("x_to");
                    double toY = rs.getDouble("y_to");
                    double toZ = rs.getDouble("z_to"); // ¡Corregido a "z_to" para coincidir con la tabla!

                    // Crear objetos Location (pueden ser null si el mundo no existe/carga)
                    org.bukkit.World fromWorld = Bukkit.getWorld(fromWorldName);
                    org.bukkit.Location fromLoc = (fromWorld != null) ? new org.bukkit.Location(fromWorld, fromX, fromY, fromZ) : null;

                    org.bukkit.World toWorld = Bukkit.getWorld(toWorldName);
                    org.bukkit.Location toLoc = (toWorld != null) ? new org.bukkit.Location(toWorld, toX, toY, toZ) : null;

                    // Solo añadir el teletransporte si ambas ubicaciones son válidas
                    if (fromLoc != null && toLoc != null) {
                        teleportsOnPage.add(new Teleport(playerName, timestamp, fromLoc, toLoc));
                    } else {
                        plugin.getLogger().log(Level.WARNING, "Skipping teleport entry due to unknown world: FromWorld=" + fromWorldName + ", ToWorld=" + toWorldName + " for player " + playerName + ". This entry will not be displayed.");
                    }
                }
                rs.close(); // Cerrar el ResultSet

                // Si no hay entradas en esta página (quizás todas fueron filtradas por mundos desconocidos)
                if (teleportsOnPage.isEmpty() && totalEntries > 0) {
                        sender.sendMessage(plugin.getLangMessage("no-entries-on-page")
                                .replace("%player%", finalTargetPlayerName)
                                .replace("%page%", String.valueOf(finalPage)));
                    return;
                }
                // Si totalEntries es 0, ya se manejó al inicio del método.


                // Enviar el encabezado del historial (definido sin prefijo en lang.yml)
                String header = plugin.getLangMessageRaw("player-history-header")
                                           .replace("%player%", finalTargetPlayerName);
                sender.sendMessage(header);

                // Mensaje de click-info (sin prefijo)
                if (sender instanceof Player) {
                    TextComponent clickInfoBase = new TextComponent(plugin.getLangMessageRaw("teleport-click-info-base"));
                    TextComponent clickInfoCommand = new TextComponent(plugin.getLangMessageRaw("teleport-click-info-command"));
                    clickInfoBase.addExtra(clickInfoCommand);
                    ((Player) sender).spigot().sendMessage(clickInfoBase);
                } else {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-manual-info"));
                }

                // Separador inicial (sin prefijo)
                if (!teleportsOnPage.isEmpty()) {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                }

                // Mostrar cada entrada de teletransporte
                for (int i = 0; i < teleportsOnPage.size(); i++) {
                    Teleport teleport = teleportsOnPage.get(i);
                    String formattedTimestamp = dateFormat.format(new Date(teleport.getTimestamp()));

                    TextComponent lineComponent = new TextComponent(); // Componente para la línea individual (sin prefijo)

                    // Construir la línea de teletransporte usando las claves de lang.yml
                    TextComponent timestampPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-timestamp").replace("%timestamp%", formattedTimestamp));
                    lineComponent.addExtra(timestampPart);

                    String fromCoords = String.format(plugin.getLangMessageRaw("teleport-entry-coords"),
                            teleport.getFromX(), teleport.getFromY(), teleport.getFromZ());
                    TextComponent fromCoordsPart = new TextComponent(fromCoords);

                    if (sender instanceof Player) { // Añadir hover event para "FROM"
                        String hoverTextFrom = plugin.getLangMessageRaw("hover-from-coords")
                                .replace("%x%", String.format("%.0f", teleport.getFromX()))
                                .replace("%y%", String.format("%.0f", teleport.getFromY()))
                                .replace("%z%", String.format("%.0f", teleport.getFromZ()))
                                .replace("%world%", teleport.getFromWorld());
                        fromCoordsPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverTextFrom)));
                    }
                    lineComponent.addExtra(fromCoordsPart);

                    TextComponent fromWorldPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-world").replace("%world%", teleport.getFromWorld()));
                    lineComponent.addExtra(fromWorldPart);

                    TextComponent arrowPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-arrow"));
                    lineComponent.addExtra(arrowPart);

                    String toCoords = String.format(plugin.getLangMessageRaw("teleport-entry-coords-to"),
                            teleport.getToX(), teleport.getToY(), teleport.getToZ());
                    TextComponent toCoordsPart = new TextComponent(toCoords);

                    if (sender instanceof Player) { // Añadir hover event para "TO"
                        String hoverTextTo = plugin.getLangMessageRaw("hover-to-coords")
                                .replace("%x%", String.format("%.0f", teleport.getToX()))
                                .replace("%y%", String.format("%.0f", teleport.getToY()))
                                .replace("%z%", String.format("%.0f", teleport.getToZ()))
                                .replace("%world%", teleport.getToWorld());
                        toCoordsPart.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverTextTo)));
                    }
                    lineComponent.addExtra(toCoordsPart);

                    TextComponent toWorldPart = new TextComponent(plugin.getLangMessageRaw("teleport-entry-world-to").replace("%world%", teleport.getToWorld()));
                    lineComponent.addExtra(toWorldPart);

                    // Hacer la línea clickeable para jugadores
                    if (sender instanceof Player) {
                        Player playerSender = (Player) sender;
                        String teleportCommandFormat = plugin.getPluginConfig().getString("teleport-command.teleport-command-format", "/tpopos %x% %y% %z% %world%");
                        
                        String teleportCommand = teleportCommandFormat
                                .replace("%world%", teleport.getToWorld())
                                .replace("%x%", String.format("%.0f", teleport.getToX()))
                                .replace("%y%", String.format("%.0f", teleport.getToY()))
                                .replace("%z%", String.format("%.0f", teleport.getToZ()));

                        lineComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, teleportCommand));
                        playerSender.spigot().sendMessage(lineComponent);
                    } else {
                        // Para la consola, enviar el texto plano
                        sender.sendMessage(lineComponent.toPlainText());
                    }

                    // Separador después de cada entrada (excepto la última)
                    if (i < teleportsOnPage.size() - 1) {
                        sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                    }
                }

                // Separador final (sin prefijo)
                if (!teleportsOnPage.isEmpty()) {
                    sender.sendMessage(plugin.getLangMessageRaw("teleport-separator"));
                }

                // --- Lógica de la línea de Paginación ---
                TextComponent paginationComponent = new TextComponent();

                // Añadir información de la página actual/total
                String pageInfo = plugin.getLangMessageRaw("pagination-info")
                                           .replace("%current_page%", String.valueOf(finalPage))
                                           .replace("%total_pages%", String.valueOf(totalPages));
                paginationComponent.addExtra(pageInfo);

                // Añadir botón "Anterior" si no es la primera página
                if (finalPage > 1) {
                    // Añadir un separador si ya hay contenido (info de página)
                    if (!paginationComponent.toPlainText().trim().isEmpty()) {
                        paginationComponent.addExtra(plugin.getLangMessageRaw("pagination-separator"));
                    }
                    TextComponent previousPage = new TextComponent(plugin.getLangMessageRaw("pagination-previous"));
                    String prevCommand = "/" + plugin.getCommandAlias() + " ";
                    if (!finalTargetPlayerName.equals(sender.getName())) { // Mantener el jugador si se está viendo el historial de otro
                        prevCommand += "player " + finalTargetPlayerName + " ";
                    }
                    prevCommand += (finalPage - 1); // Comando para la página anterior
                    previousPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, prevCommand));
                    if (sender instanceof Player) { // Hover event para jugadores
                        previousPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getLangMessageRaw("hover-pagination-previous"))));
                    }
                    paginationComponent.addExtra(previousPage);
                }

                // Añadir botón "Siguiente" si no es la última página
                if (finalPage < totalPages) {
                    // Añadir un separador si ya hay contenido (info de página o botón "anterior")
                    if (!paginationComponent.toPlainText().trim().isEmpty()) {
                        paginationComponent.addExtra(plugin.getLangMessageRaw("pagination-separator"));
                    }
                    TextComponent nextPage = new TextComponent(plugin.getLangMessageRaw("pagination-next"));
                    String nextCommand = "/" + plugin.getCommandAlias() + " ";
                    if (!finalTargetPlayerName.equals(sender.getName())) { // Mantener el jugador si se está viendo el historial de otro
                        nextCommand += "player " + finalTargetPlayerName + " ";
                    }
                    nextCommand += (finalPage + 1); // Comando para la página siguiente
                    nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, nextCommand));
                    if (sender instanceof Player) { // Hover event para jugadores
                        nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(plugin.getLangMessageRaw("hover-pagination-next"))));
                    }
                    paginationComponent.addExtra(nextPage);
                }

                // Enviar la línea de paginación completa al sender (¡AHORA SIN PREFIJO!)
                if (!paginationComponent.toPlainText().trim().isEmpty()) { // Solo enviar si hay algo en el componente
                    if (sender instanceof Player) {
                        // Anterior: TextComponent finalPaginationLine = new TextComponent(plugin.getPrefix()); finalPaginationLine.addExtra(paginationComponent); ((Player) sender).spigot().sendMessage(finalPaginationLine);
                        // NUEVO: Envía el paginationComponent directamente para evitar el prefijo
                        ((Player) sender).spigot().sendMessage(paginationComponent);
                    } else {
                        sender.sendMessage(paginationComponent.toPlainText()); // Para la consola, solo texto plano
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
        // Si el usuario no tiene el permiso base, no se sugiere NADA.
        if (!sender.hasPermission("tphistory.command.use")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Sugerir "player" si tiene permiso para listar otros jugadores
            if (sender.hasPermission("tphistory.command.listplayers")) {
                completions.add("player");
            }
            // Sugerir "reload" si tiene permiso para recargar
            if (sender.hasPermission("tphistory.command.reload")) {
                completions.add("reload");
            }

            // Filtrar las sugerencias basadas en lo que el usuario ha escrito
            List<String> filteredCompletions = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(args[0].toLowerCase())) {
                    filteredCompletions.add(s);
                }
            }
            return filteredCompletions;

        } else if (args.length == 2 && args[0].equalsIgnoreCase("player") && sender.hasPermission("tphistory.command.listplayers")) {
            // Sugerir nombres de jugadores online si el primer argumento es "player"
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            return completions;
        } else if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
            // Sugerir "page" después de /tph player <playername>
            // La sugerencia de "page" ya se limita a quienes tienen tphistory.command.listplayers por el if anterior.
            if ("page".startsWith(args[2].toLowerCase())) {
                completions.add("page");
            }
            return completions;
        }
        // No hay sugerencias para otros casos
        return Collections.emptyList();
    }
}