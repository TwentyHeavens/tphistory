package com.trickypr.tpHistory.store;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import com.trickypr.tpHistory.objects.Teleport;
import org.bukkit.Location;
import org.bukkit.Bukkit; // Necesario para Bukkit.getScheduler()

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLite {
    private final TeleportTracerPlugin plugin;
    private final String databasePath;
    private Connection connection;

    public SQLite(TeleportTracerPlugin plugin, String dbFileName) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + dbFileName;
    }

    // Inicializa la conexión a la base de datos y crea la tabla si no existe
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC"); // Carga el driver JDBC de SQLite
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            createTable();
            plugin.getLogger().info("Base de datos SQLite inicializada en: " + databasePath);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "JDBC de SQLite no encontrado. Asegúrate de que el driver esté en tu classpath.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin); // Deshabilita el plugin si no encuentra el driver
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar o inicializar la base de datos SQLite.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin); // Deshabilita el plugin si hay un error de SQL
        }
    }

    // Crea la tabla 'teleport_history' si no existe
    private void createTable() throws SQLException {
        // Confirmado: z_to es el nombre de la columna para la coordenada Z de destino
        String sql = "CREATE TABLE IF NOT EXISTS teleport_history (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "player_name TEXT NOT NULL," +
                     "timestamp INTEGER NOT NULL," +
                     "world_from TEXT NOT NULL," +
                     "x_from REAL NOT NULL," +
                     "y_from REAL NOT NULL," +
                     "z_from REAL NOT NULL," +
                     "world_to TEXT NOT NULL," +
                     "x_to REAL NOT NULL," +
                     "y_to REAL NOT NULL," +
                     "z_to REAL NOT NULL" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    // Cierra la conexión a la base de datos
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexión a la base de datos SQLite cerrada.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al cerrar la conexión SQLite: " + e.getMessage(), e);
        }
    }

    /**
     * Inserta un nuevo registro de teletransporte para un jugador y, si es necesario,
     * elimina el registro más antiguo para mantener el límite.
     * Este método es llamado por TeleportTracerPlugin.logTeleport().
     */
    public void addTeleportRecord(String playerName, Location fromLoc, Location toLoc) {
        // Ejecutar la operación de base de datos en un hilo asíncrono
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Re-establecer la conexión si se ha cerrado
                if (connection == null || connection.isClosed()) {
                    plugin.getLogger().warning("Conexión a la base de datos SQLite no está activa al intentar añadir registro. Intentando re-inicializar...");
                    initialize();
                    if (connection == null || connection.isClosed()) {
                        throw new SQLException("No se pudo establecer o re-establecer la conexión a la base de datos para añadir el registro.");
                    }
                }

                // Insertar el nuevo registro
                String insertSql = "INSERT INTO teleport_history (player_name, timestamp, world_from, x_from, y_from, z_from, world_to, x_to, y_to, z_to) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    ps.setString(1, playerName);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, fromLoc.getWorld().getName());
                    ps.setDouble(4, fromLoc.getX());
                    ps.setDouble(5, fromLoc.getY());
                    ps.setDouble(6, fromLoc.getZ());
                    ps.setString(7, toLoc.getWorld().getName());
                    ps.setDouble(8, toLoc.getX());
                    ps.setDouble(9, toLoc.getY());
                    ps.setDouble(10, toLoc.getZ());
                    ps.executeUpdate();
                }

                // Después de la inserción, aplicar la lógica para limitar el historial
                limitTeleportHistory(playerName);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al guardar el registro de teletransporte para " + playerName + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Limita el historial de teletransporte para un jugador específico.
     * Si el número de entradas excede el límite configurado en config.yml,
     * se eliminan las entradas más antiguas.
     */
    private void limitTeleportHistory(String playerName) {
        // Obtener el límite de entradas desde la configuración
        int maxEntries = plugin.getPluginConfig().getInt("settings.max-teleport-history-per-player", 100); // 100 es el valor por defecto si no se encuentra
        try {
            // Contar el número actual de entradas para el jugador
            int currentEntries = getTeleportHistoryCount(playerName);

            // Si el número actual excede el límite máximo
            if (currentEntries > maxEntries) {
                int entriesToDelete = currentEntries - maxEntries; // Cuántas entradas necesitamos eliminar
                
                // Consulta para eliminar las N entradas más antiguas para este jugador
                // Ordenamos por timestamp ascendente y luego por id ascendente para garantizar
                // que siempre se eliminen los registros más antiguos de manera consistente.
                String deleteSql = "DELETE FROM teleport_history WHERE player_name = ? AND id IN (" +
                                   "SELECT id FROM teleport_history WHERE player_name = ? ORDER BY timestamp ASC, id ASC LIMIT ?)";
                try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
                    ps.setString(1, playerName); // Parámetro para el DELETE principal
                    ps.setString(2, playerName); // Parámetro para la subconsulta SELECT
                    ps.setInt(3, entriesToDelete); // Límite de entradas a eliminar
                    int deletedRows = ps.executeUpdate();
                    plugin.getLogger().log(Level.INFO, "Eliminadas " + deletedRows + " entradas de historial antiguas para " + playerName + ". (Límite: " + maxEntries + ")");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al limitar el historial de teletransporte para " + playerName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene un conjunto de resultados (ResultSet) del historial de teletransporte
     * para un jugador, con límite y offset para paginación.
     */
    public ResultSet getTeleportHistory(String playerName, int limit, int offset) throws SQLException {
        // Re-establecer la conexión si se ha cerrado
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().warning("Conexión a la base de datos SQLite no está activa al intentar obtener historial. Intentando re-inicializar...");
            initialize();
            if (connection == null || connection.isClosed()) {
                throw new SQLException("No se pudo establecer o re-establecer la conexión a la base de datos.");
            }
        }
        // Consulta para obtener registros ordenados de más reciente a más antiguo
        String sql = "SELECT * FROM teleport_history WHERE player_name = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, playerName);
        ps.setInt(2, limit);
        ps.setInt(3, offset);
        return ps.executeQuery();
    }

    /**
     * Obtiene el número total de entradas de historial para un jugador específico.
     * Utilizado para calcular el número total de páginas.
     */
    public int getTeleportHistoryCount(String playerName) throws SQLException {
        // Re-establecer la conexión si se ha cerrado
        if (connection == null || connection.isClosed()) {
            plugin.getLogger().warning("Conexión a la base de datos SQLite no está activa al intentar obtener conteo. Intentando re-inicializar...");
            initialize();
            if (connection == null || connection.isClosed()) {
                throw new SQLException("No se pudo establecer o re-establecer la conexión a la base de datos.");
            }
        }
        String sql = "SELECT COUNT(*) FROM teleport_history WHERE player_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}