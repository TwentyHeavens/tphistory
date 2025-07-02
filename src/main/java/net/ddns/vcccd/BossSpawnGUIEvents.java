package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import net.ddns.vcccd.BelzebuthEntity;
import net.ddns.vcccd.BathinEntity;

public class BossSpawnGUIEvents implements Listener {

    private final Main main;
    private final BossBars bossBarsManager;

    public BossSpawnGUIEvents(Main main, BossBars bossBarsManager) {
        this.main = main;
        this.bossBarsManager = bossBarsManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = main.getConfig();
        Location playerLocation = player.getLocation();

        if (event.getView().getTitle().equals("Generar Jefe")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getItemMeta() == null) {
                return;
            }

            String clickedDisplayName = event.getCurrentItem().getItemMeta().getDisplayName();
            String strippedDisplayName = ChatColor.stripColor(clickedDisplayName);

            if (strippedDisplayName.equals("☠ Belzebuth ☠")) {
                player.closeInventory();
                new BelzebuthEntity(config.getInt("Belzebuth.Health"), player.getLocation(), player.getWorld(), main, bossBarsManager);
                player.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "¡Has generado a Belzebuth!");

            } else if (strippedDisplayName.equals("⸸ Bathin ⸸")) {
                player.closeInventory();
                new BathinEntity(config.getInt("Bathin.Health"), player.getLocation(), player.getWorld(), main, bossBarsManager);
                player.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "¡Has generado a Bathin!");

            } else if (clickedDisplayName.equals(ChatColor.BLACK + ".")) {
                player.closeInventory();
            }
        }
    }
}