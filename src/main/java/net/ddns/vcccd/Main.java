package net.ddns.vcccd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	private ConsoleCommandSender console = getServer().getConsoleSender();
	private String pluginPrefix;

	private BossBars bossBarsManager;

	public ConsoleCommandSender getConsole() {
		return this.console;
	}

	public String getPluginPrefix() {
		if (pluginPrefix == null) {
			pluginPrefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("General.PluginPrefix", "&e・&aRey Boss&e・&f» "));
		}
		return this.pluginPrefix;
	}

	public BossBars getBossBarsManager() {
		return bossBarsManager;
	}

	@Override
	public void onEnable() {
		FileConfiguration config = this.getConfig();

		config.addDefault("General.PluginPrefix", "&e・&aRey Boss&e・&f» ");
		config.addDefault("Belzebuth.Health", 300);
		config.addDefault("Bathin.Health", 300);
		config.addDefault("SpawnRNG", 500);
		config.addDefault("SpawnInWorld", false);
		config.addDefault("Belzebuth.Spawn", true);
		config.addDefault("Bathin.Spawn", true);
		config.addDefault("Belzebuth.DeSpawn", true);
		config.addDefault("Bathin.DeSpawn", true);
		config.addDefault("Worlds", "world,city");

		this.saveDefaultConfig();
		this.pluginPrefix = getPluginPrefix();

		this.bossBarsManager = new BossBars(this);


		this.getCommand("bossesmax").setExecutor(new BossSpawnGUI(this));

		this.getCommand("espadadeladmin").setExecutor(new AdminSwordItem(this));

		this.getCommand("spawnboss").setExecutor(new SpawnBossCommand(this, this.bossBarsManager));

		this.getCommand("despawnentities").setExecutor(new DespawnCommand(this));

		this.getCommand("removebars").setExecutor(this.bossBarsManager);

		getServer().getPluginManager().registerEvents(new BossSpawnGUIEvents(this, this.bossBarsManager), this);

		getServer().getPluginManager().registerEvents(new AdminSwordListener(), this);
		getServer().getPluginManager().registerEvents(new BelzebuthEvents(this), this);
		getServer().getPluginManager().registerEvents(new BathinEvents(this), this);

		getServer().getPluginManager().registerEvents(new SpawnInWorld(this, this.bossBarsManager), this);

		getServer().getPluginManager().registerEvents(this.bossBarsManager, this);


		console.sendMessage(getPluginPrefix() + "El Plugin BossesMax ha sido cargado exitosamente.");
		console.sendMessage(getPluginPrefix() + "Tenga en cuenta que esto no significa que todas las funciones funcionarán.");
		console.sendMessage(getPluginPrefix() + "Si encuentra problemas con el plugin, repórtelos en:");
		console.sendMessage(getPluginPrefix() + ChatColor.GREEN + "https://github.com/s5y-ux/BossesMax/issues");
		console.sendMessage(getPluginPrefix() + "Confía en mí, lo veré.");

	}

	@Override
	public void onDisable() {
		if (bossBarsManager != null) {
			bossBarsManager.cleanUpAllBossBars();
		}
		console.sendMessage(getPluginPrefix() + "El Plugin BossesMax ha sido deshabilitado.");
	}
}