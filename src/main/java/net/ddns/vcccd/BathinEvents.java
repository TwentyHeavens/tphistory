package net.ddns.vcccd;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class BathinEvents implements Listener {

    private final Main main;

    public BathinEvents(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onBathinAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Enderman)) {
            return;
        }

        Enderman bathin = (Enderman) event.getDamager();

        if (bathin.getCustomName() == null) {
            return;
        }

        String strippedCustomName = ChatColor.stripColor(bathin.getCustomName());
        boolean isBathin = strippedCustomName.equals("⸸ Bathin ⸸");

        if (isBathin) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Random rand = new Random();
            Player player = (Player) event.getEntity();
            Location playerLocal = player.getLocation();

            Location randomPlayerTeleportLoc = new Location(
                bathin.getWorld(),
                playerLocal.getX() + (rand.nextInt(101) - 50),
                playerLocal.getY(),
                playerLocal.getZ() + (rand.nextInt(101) - 50)
            );

            Location highestPlayerSpawn = player.getWorld().getHighestBlockAt(randomPlayerTeleportLoc).getLocation();
            highestPlayerSpawn.setY(highestPlayerSpawn.getY() + 1);

            Location randomBathinTeleportLoc = new Location(
                bathin.getWorld(),
                bathin.getLocation().getX() + (rand.nextInt(11) - 5),
                bathin.getLocation().getY(),
                bathin.getLocation().getZ() + (rand.nextInt(11) - 5)
            );
            Location highestBathinSpawn = bathin.getWorld().getHighestBlockAt(randomBathinTeleportLoc).getLocation();
            highestBathinSpawn.setY(highestBathinSpawn.getY() + 1);

            player.teleport(highestPlayerSpawn);
            bathin.teleport(highestBathinSpawn);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.DARK_PURPLE + "Bathin intenta poseerte"));
        }
    }

    @EventHandler
    public void onBathinDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Enderman)) {
            return;
        }

        Enderman bathin = (Enderman) event.getEntity();

        if (bathin.getCustomName() == null) {
            return;
        }

        String strippedCustomName = ChatColor.stripColor(bathin.getCustomName());
        boolean isBathin = strippedCustomName.equals("⸸ Bathin ⸸");

        if (isBathin) {
            event.getDrops().clear();

            Player killer = event.getEntity().getKiller();

            if (killer != null) {
                String commandToExecute = "kit give ripbathin " + killer.getName();
                main.getServer().dispatchCommand(main.getServer().getConsoleSender(), commandToExecute);

                killer.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "¡Has recibido un kit por derrotar a Bathin!");
            }
        }
    }
}