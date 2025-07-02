package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import net.ddns.vcccd.BossBars; 

public class BelzebuthEntity {

    private final BossBars bossBarsManager; 

    @SuppressWarnings("deprecation")
    public BelzebuthEntity(int health, Location local, World world, Main main, BossBars bossBarsManager) { 
        this.bossBarsManager = bossBarsManager; 

        Zombie Belzebuth = (Zombie) world.spawnEntity(local, EntityType.ZOMBIE);
        EntityEquipment equipment = Belzebuth.getEquipment();

        ItemStack[] zombieArmor = {
                new ItemStack(Material.NETHERITE_BOOTS),
                new ItemStack(Material.NETHERITE_LEGGINGS),
                new ItemStack(Material.NETHERITE_CHESTPLATE),
                new ItemStack(Material.NETHERITE_HELMET)
        };

        equipment.setArmorContents(zombieArmor);
        equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));

        Belzebuth.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠"));
        Belzebuth.setCustomNameVisible(true);
        Belzebuth.setAI(true);
        
        Belzebuth.setAdult();
        Belzebuth.setMaxHealth(health);
        Belzebuth.setHealth(health);
        Belzebuth.setRemoveWhenFarAway(main.getConfig().getBoolean("BelcebuDeSpawn"));
    }


}