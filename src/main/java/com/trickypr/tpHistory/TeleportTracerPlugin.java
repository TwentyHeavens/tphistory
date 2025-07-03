package com.trickypr.tpHistory;

import com.trickypr.tpHistory.commands.TPHistoryCommand;
import com.trickypr.tpHistory.events.TeleportEvent;
import com.trickypr.tpHistory.store.SQLite;
import com.trickypr.tpHistory.objects.Teleport; // Tu objeto Teleport

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location; // ¡Necesario para construir objetos Location!
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.logging.Level;

public class TeleportTracerPlugin extends JavaPlugin {

    private SQLite database;
    private FileConfiguration pluginConfig;
    private FileConfiguration langConfig;
    private File langFile;
    private String commandAlias;

    @Override
    public void onEnable() {
        // Cargar configuración principal (config.yml)
        saveDefaultConfig();
        pluginConfig = getConfig();

        // Cargar configuración de idioma (lang.yml)
        setupLangConfig();

        // Inicializar la base de datos SQLite PRIMERO
        String dbFileName = pluginConfig.getString("database.filename", "tphistory.db");
        this.database = new SQLite(this, dbFileName);
        this.database.initialize();

        // Obtener el alias del comando principal. Asegúrate de que tu plugin.yml tiene el comando "tphist".
        String mainCommandName = "tphist";
        PluginCommand command = getCommand(mainCommandName);

        if (command != null) {
            this.commandAlias = pluginConfig.getString("command.alias", mainCommandName);
            command.setAliases(Collections.singletonList(this.commandAlias));

            // Ahora que 'database' está inicializado, podemos pasarlo al constructor de TPHistoryCommand
            TPHistoryCommand tpHistoryExecutor = new TPHistoryCommand(this, database);
            command.setExecutor(tpHistoryExecutor);
            command.setTabCompleter(tpHistoryExecutor);
            getLogger().info("Comando /" + mainCommandName + " (alias: /" + this.commandAlias + ") registrado.");
        } else {
            getLogger().severe("Comando 'tphist' no encontrado en plugin.yml! Por favor, asegúrate de que esté definido.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar el listener de teletransportes
        getServer().getPluginManager().registerEvents(new TeleportEvent(this), this);

        getLogger().info("TPHistory ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("TPHistory ha sido deshabilitado.");
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        pluginConfig = getConfig();
        setupLangConfig();

        String mainCommandName = "tphist";
        PluginCommand command = getCommand(mainCommandName);
        if (command != null) {
            this.commandAlias = pluginConfig.getString("command.alias", mainCommandName);
            command.setAliases(Collections.singletonList(this.commandAlias));
            // No es necesario recrear el executor aquí, ya tiene la referencia al plugin y database
        }

        getLogger().info("Configuración de TPHistory recargada.");
    }

    private void setupLangConfig() {
        langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultLangStream = getResource("lang.yml");
        if (defaultLangStream != null) {
            YamlConfiguration defaultLangConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultLangStream));
            boolean changed = false;
            for (String key : defaultLangConfig.getKeys(true)) {
                if (!langConfig.contains(key)) {
                    langConfig.set(key, defaultLangConfig.get(key));
                    changed = true;
                }
            }
            if (changed) {
                try {
                    langConfig.save(langFile);
                    getLogger().info("lang.yml ha sido actualizado con nuevas claves.");
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "No se pudo guardar lang.yml después de la actualización.", e);
                }
            }
            try {
                defaultLangStream.close();
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Error al cerrar el InputStream de defaultLangStream", e);
            }
        }
    }

    public String getLangMessage(String key) {
        String message = langConfig.getString("messages." + key, "&cMensaje no encontrado: " + key);
        String prefix = getPrefix();

        // Esta lógica decide si añadir el prefijo o no.
        // Las claves listadas aquí NO tendrán el prefijo.
        if (!key.equals("player-history-header") &&
            !key.equals("teleport-separator") &&
            !key.startsWith("teleport-entry-") &&
            !key.equals("teleport-click-info-base") &&
            !key.equals("teleport-click-info-command") &&
            !key.equals("teleport-manual-info") &&
            !key.equals("pagination-previous") &&
            !key.equals("pagination-next") &&
            !key.equals("pagination-info") &&
            !key.equals("pagination-separator") &&
            !key.startsWith("hover-")) { // Se mantiene esta exclusión para los mensajes de hover
            message = prefix + message;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLangMessageRaw(String key) {
        String message = langConfig.getString("messages." + key, "&cRaw mensaje no encontrado: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', langConfig.getString("messages.prefix", "&7[&bTPHistory&7] "));
    }

    public SQLite getDatabase() {
        return database;
    }

    public FileConfiguration getPluginConfig() {
        return pluginConfig;
    }

    public String getCommandAlias() {
        return commandAlias;
    }

    /**
     * Registra un teletransporte de forma asíncrona para no causar lag.
     * @param teleport El objeto Teleport a registrar.
     */
    public void logTeleport(Teleport teleport) {
        // CORRECCIÓN: Tu objeto Teleport no guarda Locations directamente.
        // Necesitamos reconstruirlas para pasarlas a database.addTeleportRecord.

        // Obtenemos los mundos del servidor. Esto es crucial.
        // Asegúrate de que los mundos existan o esto puede causar NullPointerExceptions.
        Location fromLoc = new Location(
            Bukkit.getWorld(teleport.getFromWorld()),
            teleport.getFromX(),
            teleport.getFromY(),
            teleport.getFromZ()
        );

        Location toLoc = new Location(
            Bukkit.getWorld(teleport.getToWorld()),
            teleport.getToX(),
            teleport.getToY(),
            teleport.getToZ()
        );

        // Ahora, la verificación de nulidad de las Locations reconstruidas
        // y la llamada a addTeleportRecord.
        if (fromLoc.getWorld() == null || toLoc.getWorld() == null) {
            getLogger().warning("Intento de loguear teletransporte con mundos desconocidos para " + teleport.getPlayerName() + ". Ignorando.");
            return;
        }

        // Llamar al método addTeleportRecord de la base de datos con los parámetros correctos.
        // Bukkit.getScheduler().runTaskAsynchronously(this, () -> { // addTeleportRecord ya es async en SQLite.java
            database.addTeleportRecord(teleport.getPlayerName(), fromLoc, toLoc);
            getLogger().fine("Teleport logged for " + teleport.getPlayerName() + ": " +
                                     teleport.getFromWorld() + " -> " + teleport.getToWorld());
        // });
        // El manejo de excepciones ya está dentro de addTeleportRecord en SQLite.java
    }
}