package com.trickypr.tpHistory.store;

import com.trickypr.tpHistory.TeleportTracerPlugin;
import org.bukkit.Location;
import org.bukkit.Bukkit;

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

    /**
     * Inicializa la conexión a la base de datos y crea la tabla si no existe.
     * Este método debe ser llamado al habilitar el plugin.
     */
    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            createTable(this.connection);
            plugin.getLogger().info("Base de datos SQLite inicializada en: " + databasePath);
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "JDBC de SQLite no encontrado. Asegúrate de que el driver esté en tu classpath.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al conectar o inicializar la base de datos SQLite.", e);
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    /**
     * Cierra la conexión a la base de datos.
     * Este método debe ser llamado cuando el plugin se deshabilita para liberar recursos.
     */
    public void close() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("Conexión a la base de datos SQLite cerrada.");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al cerrar la conexión SQLite: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Obtiene la conexión actual a la base de datos.
     * Intenta reconectar si la conexión es nula, está cerrada o es inválida.
     * @return La conexión a la base de datos.
     * @throws SQLException Si no se puede establecer o re-establecer la conexión.
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(5)) {
            plugin.getLogger().warning("Conexión a la base de datos SQLite no está activa. Intentando re-establecer...");
            try {
                Class.forName("org.sqlite.JDBC");
                this.connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
                plugin.getLogger().info("Conexión a la base de datos SQLite re-establecida.");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().log(Level.SEVERE, "JDBC de SQLite no encontrado al intentar reconectar.", e);
                throw new SQLException("JDBC de SQLite no encontrado al intentar reconectar.", e);
            }
        }
        return connection;
    }

    // Crea la tabla 'teleport_history' si no existe
    private void createTable(Connection conn) throws SQLException {
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
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Inserta un nuevo registro de teletransporte para un jugador y, si es necesario,
     * elimina el registro más antiguo para mantener el límite.
     * Este método es llamado por TeleportTracerPlugin.logTeleport().
     */
    public void addTeleportRecord(String playerName, Location fromLoc, Location toLoc) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = getConnection()) {
                String insertSql = "INSERT INTO teleport_history (player_name, timestamp, world_from, x_from, y_from, z_from, world_to, x_to, y_to, z_to) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
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

                limitTeleportHistory(conn, playerName);

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error al guardar el registro de teletransporte para " + playerName + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Limita el historial de teletransporte para un jugador específico.
     * Si el número de entradas excede el límite configurado en config.yml,
     * se eliminan las entradas más antiguas.
     * @param conn La conexión a la base de datos a usar.
     * @param playerName El nombre del jugador cuyo historial se va a limitar.
     * @throws SQLException Si ocurre un error de SQL.
     */
    private void limitTeleportHistory(Connection conn, String playerName) throws SQLException {
        int maxEntries = plugin.getPluginConfig().getInt("settings.max-teleport-history-per-player", 100);
        int currentEntries = getTeleportHistoryCount(conn, playerName);

        if (currentEntries > maxEntries) {
            int entriesToDelete = currentEntries - maxEntries;
            String deleteSql = "DELETE FROM teleport_history WHERE player_name = ? AND id IN (" +
                               "SELECT id FROM teleport_history WHERE player_name = ? ORDER BY timestamp ASC, id ASC LIMIT ?)";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, playerName);
                ps.setString(2, playerName);
                ps.setInt(3, entriesToDelete);
                int deleted = ps.executeUpdate();
                plugin.getLogger().info("Eliminadas " + deleted + " entradas de historial antiguas para " + playerName);
            }
        }
    }

    /**
     * Obtiene un conjunto de resultados (ResultSet) del historial de teletransporte
     * para un jugador, con límite y offset para paginación.
     * @param playerName El nombre del jugador.
     * @param limit El número máximo de entradas a devolver.
     * @param offset El desplazamiento (cuántas entradas saltar desde el principio).
     * @return Un ResultSet que contiene las entradas del historial. QUIEN LLAMA DEBE CERRAR EL RESULTSET Y EL PREPAREDSTATEMENT.
     * @throws SQLException Si ocurre un error de SQL.
     */
    public ResultSet getTeleportHistory(String playerName, int limit, int offset) throws SQLException {
        Connection conn = getConnection();
        String sql = "SELECT * FROM teleport_history WHERE player_name = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, playerName);
        ps.setInt(2, limit);
        ps.setInt(3, offset);
        return ps.executeQuery();
    }

    /**
     * Obtiene el número total de entradas de historial para un jugador específico.
     * Utilizado para calcular el número total de páginas.
     * @param playerName El nombre del jugador.
     * @return El número total de entradas.
     * @throws SQLException Si ocurre un error de SQL.
     */
    public int getTeleportHistoryCount(String playerName) throws SQLException {
        return getTeleportHistoryCount(getConnection(), playerName);
    }

    /**
     * Método interno para contar entradas usando una conexión existente.
     * @param conn La conexión a la base de datos a usar.
     * @param playerName El nombre del jugador.
     * @return El número total de entradas.
     * @throws SQLException Si ocurre un error de SQL.
     */
    private int getTeleportHistoryCount(Connection conn, String playerName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM teleport_history WHERE player_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}