package net.ddns.vcccd;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BossSpawnGUI implements CommandExecutor {

	private final Main plugin;

	public BossSpawnGUI(Main plugin) {
		this.plugin = plugin;
	}

	private Inventory bossMenu = Bukkit.createInventory(null, 9, "Generar Jefe");

	private void itemLore(ArrayList<String> Lore, ItemStack item) {
		ItemMeta temp = item.getItemMeta();
		if (temp != null) {
			temp.setLore(Lore);
			item.setItemMeta(temp);
		}
	}

	private ItemStack createHead(String name, Material headMaterial) {
		ItemStack returnHead = new ItemStack(headMaterial);
		ItemMeta returnHeadMeta = returnHead.getItemMeta();
		if (returnHeadMeta != null) {
			returnHeadMeta.setDisplayName(name);
			returnHead.setItemMeta(returnHeadMeta);
		}
		return returnHead;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		ItemStack belzebuthHead, bathinHead;
		belzebuthHead = createHead(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠"), Material.ZOMBIE_HEAD);
		bathinHead = createHead(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸"), Material.ENDER_PEARL); 

		ArrayList<String> belzebuthLore = new ArrayList<>();
		belzebuthLore.add(ChatColor.WHITE + "Zombie de netherita que");
		belzebuthLore.add(ChatColor.WHITE + "invoca esbirros zombie bebe...");

		ArrayList<String> bathinLore = new ArrayList<>();
		bathinLore.add(ChatColor.WHITE + "Un Enderman");
		bathinLore.add(ChatColor.WHITE + "Que domina el viaje espacial...");

		itemLore(belzebuthLore, belzebuthHead);
		itemLore(bathinLore, bathinHead);

		bossMenu.clear();

		ItemStack blank = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta a = blank.getItemMeta();
		if (a != null) {
			a.setDisplayName(ChatColor.BLACK + ".");
			blank.setItemMeta(a);
		}

		for (int i = 0; i < bossMenu.getSize(); i++) {
			bossMenu.setItem(i, blank);
		}

		bossMenu.setItem(2, belzebuthHead);
		bossMenu.setItem(6, bathinHead);

		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.openInventory(bossMenu);
		}

		return true;
	}
}