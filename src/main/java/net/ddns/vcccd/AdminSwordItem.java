package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminSwordItem implements CommandExecutor {
	
	private final Main main;
	
	public AdminSwordItem(Main main) {
		this.main = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		ItemStack adminSword = new ItemStack(Material.NETHERITE_SWORD); // Changed variable name to follow Java conventions
		ItemMeta adminSwordMeta = adminSword.getItemMeta(); // Changed variable name to follow Java conventions
		
		// Visible name of the item in the game (Translated from "Admin Sword")
		adminSwordMeta.setDisplayName(ChatColor.RED + "Espada de Administrador"); 
		adminSword.setItemMeta(adminSwordMeta);
		
		if(sender instanceof Player) {
			Player player = (Player) sender;
			player.getInventory().setItem(player.getInventory().firstEmpty(), adminSword);
			// Message sent to the player (Translated from "You have received the Admin Sword...")
			player.sendMessage(main.getPluginPrefix() + "Has recibido la Espada de Administrador..."); 
		}
		return true; // Simpler 'return true'
	}

}