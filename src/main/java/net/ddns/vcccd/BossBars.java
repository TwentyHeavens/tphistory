package net.ddns.vcccd;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class BossBars implements CommandExecutor, Listener {

    private final Main main;

    private BossBar BelzebuthBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠"), BarColor.RED, BarStyle.SOLID);
    private BossBar BathinBar = Bukkit.createBossBar(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸"), BarColor.PURPLE, BarStyle.SOLID);

    private Map<UUID, String> activeBossEntityNames = new HashMap<>();

    private final double DISPLAY_DISTANCE = 25.0;

    public BossBars(Main main) {
        this.main = main;
        cleanUpAllBossBars();
        startBossBarUpdateTask();
    }

    public void registerBossEntity(LivingEntity bossEntity) {
        String strippedName = ChatColor.stripColor(bossEntity.getCustomName());

        if (strippedName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠")))) {
            activeBossEntityNames.put(bossEntity.getUniqueId(), strippedName);
            BelzebuthBar.setVisible(true);
        } else if (strippedName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸")))) {
            activeBossEntityNames.put(bossEntity.getUniqueId(), strippedName);
            BathinBar.setVisible(true);
        }
    }

    private void updateBossBar(BossBar bar, LivingEntity entity, int maxHealth) {
        if (!entity.isDead() && entity.isValid()) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, entity.getHealth() / maxHealth)));
            bar.setTitle(entity.getCustomName() != null ? entity.getCustomName() : bar.getTitle());
        } else {
            disableBar(bar);
            activeBossEntityNames.remove(entity.getUniqueId());
        }
    }

    private void startBossBarUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, String>> iterator = activeBossEntityNames.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, String> entry = iterator.next();
                    UUID entityId = entry.getKey();
                    String bossName = entry.getValue();
                    Entity entity = Bukkit.getEntity(entityId);

                    BossBar currentBar = null;

                    if (bossName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠")))) {
                        currentBar = BelzebuthBar;
                    } else if (bossName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸")))) {
                        currentBar = BathinBar;
                    }

                    if (entity instanceof LivingEntity && currentBar != null) {
                        LivingEntity livingEntity = (LivingEntity) entity;
                        FileConfiguration config = main.getConfig();
                        int maxHealth = 0;

                        if (bossName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠")))) {
                            maxHealth = config.getInt("Belzebuth.Health");
                        } else if (bossName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸")))) {
                            maxHealth = config.getInt("Bathin.Health");
                        }

                        if (maxHealth > 0) {
                            updateBossBar(currentBar, livingEntity, maxHealth);
                        } else {
                            Bukkit.getLogger().warning(main.getPluginPrefix() + "Salud para el jefe " + bossName + " no configurada, eliminando su BossBar.");
                            disableBar(currentBar);
                            iterator.remove();
                            continue;
                        }

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.getWorld().equals(livingEntity.getWorld()) && p.getLocation().distance(livingEntity.getLocation()) <= DISPLAY_DISTANCE) {
                                if (!currentBar.getPlayers().contains(p)) {
                                    currentBar.addPlayer(p);
                                    currentBar.setVisible(true);
                                }
                            } else {
                                if (currentBar.getPlayers().contains(p)) {
                                    currentBar.removePlayer(p);
                                }
                            }
                        }
                    } else {
                        if (currentBar != null) {
                            disableBar(currentBar);
                        }
                        iterator.remove();
                        Bukkit.getLogger().warning(main.getPluginPrefix() + "Entidad de jefe con UUID " + entityId + " (" + bossName + ") no encontrada o ya no está activa, eliminando su BossBar.");
                    }
                }
            }
        }.runTaskTimer(main, 0L, 20L);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity().getCustomName() == null) {
            return;
        }

        LivingEntity damagedEntity = (LivingEntity) event.getEntity();
        String strippedCustomName = ChatColor.stripColor(damagedEntity.getCustomName());

        if (strippedCustomName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠")))) {
            if (!activeBossEntityNames.containsKey(damagedEntity.getUniqueId())) {
                registerBossEntity(damagedEntity);
            }
        } else if (strippedCustomName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸")))) {
            if (!activeBossEntityNames.containsKey(damagedEntity.getUniqueId())) {
                registerBossEntity(damagedEntity);
            }
        }
    }

    private void disableBar(BossBar bar) {
        bar.setProgress(0);
        bar.setVisible(false);
        bar.removeAll();
    }

    public void cleanUpAllBossBars() {
        BelzebuthBar.setVisible(false);
        BelzebuthBar.removeAll();
        BathinBar.setVisible(false);
        BathinBar.removeAll();
        activeBossEntityNames.clear();
        Bukkit.getLogger().info(main.getPluginPrefix() + "Limpiadas las BossBars existentes al iniciar/recargar.");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity().getCustomName() == null) {
            return;
        }

        String strippedCustomName = ChatColor.stripColor(event.getEntity().getCustomName());
        UUID entityId = event.getEntity().getUniqueId();

        if (strippedCustomName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&c☠ &lBelzebuth &c☠")))) {
            disableBar(BelzebuthBar);
        } else if (strippedCustomName.equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', "&5⸸ &lBathin &5⸸")))) {
            disableBar(BathinBar);
        }
        activeBossEntityNames.remove(entityId);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            disableBar(BelzebuthBar);
            disableBar(BathinBar);
            activeBossEntityNames.clear();
            player.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "¡Las barras de jefe Belzebuth y Bathin han sido eliminadas!");
        } else {
            sender.sendMessage(main.getPluginPrefix() + "Este comando solo puede ser ejecutado por un jugador.");
        }
        return true;
    }
}