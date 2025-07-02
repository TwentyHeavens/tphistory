package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class AdminSwordListener implements Listener{
	
	@EventHandler
	public void useAdminSword(EntityDamageByEntityEvent event) {
		if(event.getDamager() instanceof Player) {
			Player player = (Player) event.getDamager();
			
            // Replaced 'assert true' with a more conventional check.
            // If the item in hand has no ItemMeta, it's not the Admin Sword, so just return.
			if(player.getInventory().getItemInMainHand().getItemMeta() == null) {
				return;
			} 
            
            // Translated from "Admin Sword"
            // The display name of the sword
            if(player.getInventory().getItemInMainHand().getItemMeta().getDisplayName().equals(ChatColor.RED + "Espada de Administrador")) { 
				if(event.getEntity() instanceof Mob) {
					Mob mob = (Mob) event.getEntity();
					mob.setHealth(0); // Instantly kill the mob
                    event.setCancelled(true); // Prevent normal damage application
				}
			}
		}
	}

}