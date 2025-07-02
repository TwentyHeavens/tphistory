package net.ddns.vcccd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.ddns.vcccd.BelzebuthEntity;
import net.ddns.vcccd.BathinEntity;

public class SpawnBossCommand implements CommandExecutor, TabCompleter {

    private final Main main;
    private final FileConfiguration config;
    private final BossBars bossBarsManager;

    private static final List<String> BOSS_NAMES = Arrays.asList("belzebuth", "bathin");
    private static final List<String> SPAWN_TYPES = Arrays.asList("player", "coordinates");

    public SpawnBossCommand(Main main, BossBars bossBarsManager) {
        this.main = main;
        this.config = main.getConfig();
        this.bossBarsManager = bossBarsManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "Uso: /spawnboss <jefe> <player|coordinates> [jugador|x] [y] [z] [mundo]");
            return true;
        }

        String bossName = args[0].toLowerCase();
        String spawnType = args[1].toLowerCase();
        Location spawnLocation = null;
        World world = null;

        if (!BOSS_NAMES.contains(bossName)) {
            sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Jefe desconocido: " + bossName);
            return true;
        }

        switch (spawnType) {
            case "player":
                if (args.length < 3) {
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "Uso: /spawnboss <jefe> player <jugador>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Jugador no encontrado: " + args[2]);
                    return true;
                }
                spawnLocation = targetPlayer.getLocation();
                world = targetPlayer.getWorld();
                break;

            case "coordinates":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Este tipo de spawn requiere coordenadas y debe ser ejecutado por un jugador (para obtener el mundo actual por defecto) o especificar el mundo.");
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "Uso: /spawnboss <jefe> coordinates <x> <y> <z> [mundo]");
                    return true;
                }

                if (args.length < 5) {
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "Uso: /spawnboss <jefe> coordinates <x> <y> <z> [mundo]");
                    return true;
                }

                try {
                    double x = Double.parseDouble(args[2]);
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]);

                    if (args.length == 6) {
                        world = Bukkit.getWorld(args[5]);
                        if (world == null) {
                            sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Mundo no encontrado: " + args[5]);
                            return true;
                        }
                    } else {
                        world = ((Player) sender).getWorld();
                    }
                    spawnLocation = new Location(world, x, y, z);

                } catch (NumberFormatException e) {
                    sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Coordenadas inválidas. Usa números para X, Y, Z.");
                    return true;
                }
                break;

            default:
                sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Tipo de spawn desconocido. Usa 'player' o 'coordinates'.");
                sender.sendMessage(main.getPluginPrefix() + ChatColor.YELLOW + "Uso: /spawnboss <jefe> <player|coordinates> [jugador|x] [y] [z] [mundo]");
                return true;
        }

        if (spawnLocation == null || world == null) {
            sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "No se pudo determinar una ubicación de spawn válida.");
            return true;
        }

        boolean success = spawnBoss(bossName, spawnLocation, world);

        if (success) {
            sender.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "Has generado a " + bossName + " exitosamente en " + world.getName() + " a " + spawnLocation.getBlockX() + "," + spawnLocation.getBlockY() + "," + spawnLocation.getBlockZ() + ".");
        } else {
            sender.sendMessage(main.getPluginPrefix() + ChatColor.RED + "Error al intentar generar a " + bossName + ".");
        }

        return true;
    }

    private boolean spawnBoss(String name, Location location, World world) {
        switch (name) {
            case "belzebuth":
                new BelzebuthEntity(config.getInt("Belzebuth.Health"), location, world, main, bossBarsManager);
                break;
            case "bathin":
                new BathinEntity(config.getInt("Bathin.Health"), location, world, main, bossBarsManager);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partialBossName = args[0].toLowerCase();
            completions.addAll(BOSS_NAMES.stream()
                    .filter(name -> name.startsWith(partialBossName))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String partialSpawnType = args[1].toLowerCase();
            completions.addAll(SPAWN_TYPES.stream()
                    .filter(type -> type.startsWith(partialSpawnType))
                    .collect(Collectors.toList()));
        } else if (args.length >= 3) {
            String spawnType = args[1].toLowerCase();
            if (spawnType.equals("player")) {
                if (args.length == 3) {
                    String partialPlayerName = args[2].toLowerCase();
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(name -> name.toLowerCase().startsWith(partialPlayerName))
                            .collect(Collectors.toList()));
                }
            } else if (spawnType.equals("coordinates")) {
                if (!(sender instanceof Player)) {
                    return completions;
                }
                Player player = (Player) sender;

                if (args.length == 3) {
                    completions.add(String.valueOf((int) player.getLocation().getX()));
                } else if (args.length == 4) {
                    completions.add(String.valueOf((int) player.getLocation().getY()));
                } else if (args.length == 5) {
                    completions.add(String.valueOf((int) player.getLocation().getZ()));
                } else if (args.length == 6) {
                    String partialWorldName = args[5].toLowerCase();
                    completions.addAll(Bukkit.getWorlds().stream()
                            .map(World::getName)
                            .filter(name -> name.toLowerCase().startsWith(partialWorldName))
                            .collect(Collectors.toList()));
                }
            }
        }
        return completions;
    }
}