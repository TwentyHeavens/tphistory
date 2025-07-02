package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import net.ddns.vcccd.BossBars; 
import net.ddns.vcccd.Main; 


public class BathinEntity {

    @SuppressWarnings("deprecation")
    public BathinEntity(int health, Location local, World world, Main main, BossBars bossBarsManager) {
        Enderman bathin = (Enderman) world.spawnEntity(local, EntityType.ENDERMAN);

        bathin.setCustomName(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸"));
        bathin.setCustomNameVisible(true);
        bathin.setMaxHealth(health);
        bathin.setHealth(health);

        bathin.setRemoveWhenFarAway(main.getConfig().getBoolean("Bathin.DeSpawn")); 

    }
}