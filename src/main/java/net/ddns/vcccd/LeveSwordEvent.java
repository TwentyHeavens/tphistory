package net.ddns.vcccd;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LeveSwordEvent implements Listener {
	
	// Helper Methods:
	//================================
	private int RNG(int scope) {
        return (new Random().nextInt(scope));
    }
	//================================
	
	@EventHandler
	public void onLeveSworduse(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player) {
			Player player = (Player) event.getDamager();
			
			// Check if the item in hand has metadata; if not, it's not the Leve Sword.
			if(player.getInventory().getItemInMainHand().getItemMeta() == null) {
				return; // Exit if there's no item meta to check
			} 
            
            // Check if the item is the "Espada Leve" (Leve-Sword)
            if(player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(ChatColor.GRAY + "Espada Leve")) { 
				
                // 1 in 4 chance for the effect to activate (0, 1, 2, 3 -> 3 is the 4th number)
				if(RNG(4) == 3) { 
					// Ensure the damaged entity is a LivingEntity before trying to apply effects
					if (event.getEntity() instanceof LivingEntity) {
						LivingEntity referenceEntity = (LivingEntity) event.getEntity(); // Changed variable name for consistency
						
						// Apply Levitation effect
						referenceEntity.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 50, 3)); // 50 ticks (2.5 seconds), amplifier 3
						
						// Spawn particles around the entity
						double x = referenceEntity.getLocation().getX();
						double y = referenceEntity.getLocation().getY();
						double z = referenceEntity.getLocation().getZ();

                        // Spawn particles in a small area around the entity
						for (int i = -2; i <= 2; i++) { // Changed loop condition to include 2
                            for (int j = -2; j <= 2; j++) { // Added second loop for more even spread
                                referenceEntity.getWorld().spawnParticle(Particle.CLOUD, x + i, y, z + j, 2); // Reduced count per particle for visual smoothness
                            }
						}
					}
                    // If the entity is not a LivingEntity (e.g., an item frame, arrow), do nothing.
				}
			}
		}
	}

}