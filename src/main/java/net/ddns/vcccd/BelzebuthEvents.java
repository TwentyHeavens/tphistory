package net.ddns.vcccd;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class BelzebuthEvents implements Listener {

    private final Main main;
    private final Random random = new Random();

    private static final String BELZEBUTH_STRIPPED_NAME = "☠ Belzebuth ☠";
    private static final String MINION_STRIPPED_NAME = "Esbirro de Belzebuth";

    public BelzebuthEvents(Main main) {
        this.main = main;
    }

    private int RNG(int scope) {
        return random.nextInt(scope);
    }

    private ItemStack CustomItem(Material item, String name) {
        ItemStack returnItem = new ItemStack(item);
        ItemMeta returnItemData = returnItem.getItemMeta();
        if (returnItemData != null) {
            returnItemData.setDisplayName(name);
            returnItem.setItemMeta(returnItemData);
        }
        return returnItem;
    }

    @EventHandler
    public void onBelzebuthDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie)) {
            return;
        }

        Zombie belzebuth = (Zombie) event.getEntity();
        String rawName = belzebuth.getCustomName();

        if (rawName == null) {
            main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Entidad Zombie sin nombre personalizado. No se procesa como Belzebuth.");
            return;
        }

        String strippedName = ChatColor.stripColor(rawName);
        boolean isBelzebuth = strippedName.equals(BELZEBUTH_STRIPPED_NAME);

        main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Nombre original de la entidad: '" + rawName + "' | Nombre sin color (stripped): '" + strippedName + "' | ¿Es Belzebuth? " + isBelzebuth);

        if (isBelzebuth) {
            event.getDrops().clear();

            Player killer = belzebuth.getKiller();

            if (killer != null) {
                String command = "kit give ripbelzebuth " + killer.getName();
                main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Asesino: " + killer.getName() + ". Comando a ejecutar: '" + command + "'");
                try {
                    boolean commandSuccess = main.getServer().dispatchCommand(main.getServer().getConsoleSender(), command);
                    main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Resultado de dispatchCommand: " + commandSuccess);
                    if (commandSuccess) {
                        killer.sendMessage(main.getPluginPrefix() + ChatColor.GREEN + "¡Has recibido un kit por derrotar a Belzebuth!");
                    } else {
                        killer.sendMessage(main.getPluginPrefix() + ChatColor.RED + "¡Error al entregar el kit 'ripbelzebuth'!");
                    }
                } catch (Exception e) {
                    main.getLogger().severe(main.getPluginPrefix() + "ERROR: Excepción al ejecutar comando para kit de Belzebuth: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Asesino es NULL (la muerte no fue atribuida a un jugador). No se entregó el kit.");
            }
        } else {
            main.getLogger().info(main.getPluginPrefix() + "DEBUG: Belzebuth Death - Entidad Zombie no identificada como Belzebuth por el nombre.");
        }
    }


    @EventHandler
    public void onMinionDeath(EntityDeathEvent event) {
        Entity dyingEntity = event.getEntity();
        String rawName = dyingEntity.getCustomName();

        if (rawName == null) {
            return;
        }

        String strippedName = ChatColor.stripColor(rawName);

        boolean isMinionZombie = dyingEntity instanceof Zombie && strippedName.equals(MINION_STRIPPED_NAME);

        boolean isMinionChicken = dyingEntity instanceof Chicken &&
                                 !dyingEntity.getPassengers().isEmpty() &&
                                 dyingEntity.getPassengers().stream().anyMatch(passenger ->
                                     passenger instanceof Zombie &&
                                     ChatColor.stripColor(passenger.getCustomName()).equals(MINION_STRIPPED_NAME)
                                 );

        if (isMinionZombie || isMinionChicken) {
            event.getDrops().clear();
            main.getLogger().info(main.getPluginPrefix() + "DEBUG: Minion Death - Entidad '" + strippedName + "' (" + dyingEntity.getType().name() + ") murió. Drops limpiados.");
        }
    }


    @EventHandler
    public void onBelzebuthHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie)) {
            return;
        }

        Zombie belzebuth = (Zombie) event.getEntity();
        String rawName = belzebuth.getCustomName();
        if (rawName == null) {
            return;
        }

        String strippedName = ChatColor.stripColor(rawName);
        boolean isBelzebuth = strippedName.equals(BELZEBUTH_STRIPPED_NAME);

        if (!isBelzebuth) return;

        if (RNG(4) == 3) {
            Location loc = belzebuth.getLocation();
            for (int i = 0; i < 360; i += 6) {
                double xOffset = 4 * Math.cos(Math.toRadians(i));
                double zOffset = 4 * Math.sin(Math.toRadians(i));
                loc.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, loc.getX() + xOffset, loc.getY(), loc.getZ() + zOffset, 1);
                loc.getWorld().spawnParticle(Particle.LAVA, loc.getX() + xOffset, loc.getY(), loc.getZ() + zOffset, 1);
            }

            for (Entity near : belzebuth.getNearbyEntities(4, 4, 4)) {
                if (near instanceof Player) {
                    Player player = (Player) near;
                    Vector vector = player.getLocation().toVector().subtract(loc.toVector()).normalize();
                    vector.setY(0.5);
                    player.setVelocity(vector.multiply(1.5));
                }
            }
        }

        if (RNG(6) == 5) {
            belzebuth.getWorld().spawnEntity(belzebuth.getLocation(), EntityType.FIREBALL);
        }

        if (RNG(4) == 3) {
            World world = belzebuth.getWorld();
            Location spawnLoc = belzebuth.getLocation();

            Chicken chicken = (Chicken) world.spawnEntity(spawnLoc, EntityType.CHICKEN);
            chicken.setPersistent(false);

            Zombie minionZombie = (Zombie) world.spawnEntity(spawnLoc, EntityType.ZOMBIE);
            minionZombie.setBaby();
            minionZombie.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lEsbirro de Belzebuth"));
            minionZombie.setCustomNameVisible(true);
            minionZombie.setPersistent(false);

            chicken.addPassenger(minionZombie);

            EntityEquipment equip = minionZombie.getEquipment();
            if (equip != null) {
                equip.setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
                equip.setItemInMainHandDropChance(0.0f);
                equip.setHelmet(new ItemStack(Material.IRON_HELMET));
                equip.setHelmetDropChance(0.0f);
            }
        }
    }
}