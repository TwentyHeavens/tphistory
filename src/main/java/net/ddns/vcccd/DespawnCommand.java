package net.ddns.vcccd;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.World;

public class DespawnCommand implements CommandExecutor {
    private final Main main;

    public DespawnCommand(Main main) {
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int despawnedCount = 0;

        final String BELZEBUTH_STRIPPED_NAME = "☠ Belzebuth ☠";
        final String BELZEBUTH_MINION_STRIPPED_NAME = "Esbirro de Belzebuth";
        final String BATHIN_STRIPPED_NAME = "⸸ Bathin ⸸";

        for (World world : main.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }

                String customName = entity.getCustomName();

                if (customName != null) {
                    String strippedName = ChatColor.stripColor(customName);

                    if (entity instanceof Zombie) {
                        if (strippedName.equals(BELZEBUTH_STRIPPED_NAME) || strippedName.equals(BELZEBUTH_MINION_STRIPPED_NAME)) {
                            entity.remove();
                            despawnedCount++;
                            continue;
                        }
                    }
                    else if (entity instanceof Enderman) {
                        if (strippedName.equals(BATHIN_STRIPPED_NAME)) {
                            entity.remove();
                            despawnedCount++;
                            continue;
                        }
                    }
                    else if (entity instanceof Chicken) {
                        if (!entity.getPassengers().isEmpty()) {
                            for (Entity passenger : entity.getPassengers()) {
                                if (passenger instanceof Zombie) {
                                    String passengerName = ChatColor.stripColor(passenger.getCustomName());
                                    if (passengerName != null && passengerName.equals(BELZEBUTH_MINION_STRIPPED_NAME)) {
                                        entity.remove();
                                        despawnedCount++;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (despawnedCount > 0) {
                player.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "Se eliminaron " + despawnedCount + " entidades de jefes y esbirros en todos los mundos.");
            } else {
                player.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "No se encontraron entidades de jefes o esbirros para eliminar en ningún mundo.");
            }
        } else {
            if (despawnedCount > 0) {
                main.getLogger().info(main.getPluginPrefix() + "Se eliminaron " + despawnedCount + " entidades de jefes y esbirros en todos los mundos.");
            } else {
                main.getLogger().info(main.getPluginPrefix() + "No se encontraron entidades de jefes o esbirros para eliminar en ningún mundo.");
            }
        }
        return true;
    }
}