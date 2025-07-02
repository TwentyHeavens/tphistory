package net.ddns.vcccd;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import net.ddns.vcccd.BelzebuthEntity;
import net.ddns.vcccd.BathinEntity;

public class SpawnInWorld implements Listener {

	private final Main main;
	private FileConfiguration config;
	private final BossBars bossBarsManager; 

	public SpawnInWorld(Main main, BossBars bossBarsManager) {
		this.main = main;
		this.config = main.getConfig();
		this.bossBarsManager = bossBarsManager; 
	}

	private int RNG(int scope) {
		return (new Random().nextInt(28 + scope));
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if (this.main.getConfig().getBoolean("SpawnInWorld")) {
			Chunk chunk = event.getChunk();
			World chunkWorld = chunk.getWorld();
			String worldReference[] = config.getString("Worlds").split(",");
			for (String worldName : worldReference) {
				if (worldName.equals(chunkWorld.getName())) {
					int x = chunk.getX() * 16 + (int) (Math.random() * 16);
					int z = chunk.getZ() * 16 + (int) (Math.random() * 16);
					int y = chunk.getWorld().getHighestBlockYAt(x, z) + 4;

					Location spawnLocation = new Location(chunkWorld, x, y, z);

					switch (RNG(this.config.getInt("SpawnRNG"))) {
						case 1:
							if (this.config.getBoolean("Belzebuth.Spawn")) { 
								new BelzebuthEntity(this.config.getInt("Belzebuth.Health"), spawnLocation, chunkWorld, main, bossBarsManager);
							}
							break;
						case 7:
							if (this.config.getBoolean("Bathin.Spawn")) { 
								new BathinEntity(this.config.getInt("Bathin.Health"), spawnLocation, chunkWorld, main, bossBarsManager);
							}
							break;
						default:
							assert true;
					}
				}
			}
		}
	}
}