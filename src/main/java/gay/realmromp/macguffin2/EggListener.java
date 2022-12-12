package gay.realmromp.macguffin2;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;

import static gay.realmromp.macguffin2.Macguffin2.COLOR;

public class EggListener implements Listener {

    private final Macguffin2 plugin;
    private final Logger logger;

    public EggListener(Macguffin2 plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            logger.info("Intercepted teleport");
            event.setCancelled(true);

            if (Egg.data.holder != null) {
                Player player = Bukkit.getPlayer(Egg.data.holder);
                if (player != null) {
                    player.sendMessage(Component.text("Egg tapped; timer has been reset.").color(COLOR));
                }
            }

            Egg.data.expiry = Instant.now().getEpochSecond() + Macguffin2.TAP_INTERVAL;
            Egg.save();
        }
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
        if (Egg.data.state != State.VIRGIN) {
            if (Egg.data.state != State.RESPAWNED
                    && event.getEntityType() == EntityType.DROPPED_ITEM
                    && ((Item) event.getEntity()).getItemStack().getType() == Material.DRAGON_EGG) {
                logger.info("Egg dropped");
                ((Item) event.getEntity()).setCanMobPickup(false);
                ((Item) event.getEntity()).setCanPlayerPickup(false);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    event.getEntity().remove();
                    plugin.resetEgg();
                }, 1); // Server complains if we don't let it exist for a full tick

            } else if (Egg.data.state == State.RESPAWNED
                    && event.getEntityType() == EntityType.DROPPED_ITEM
                    && ((Item) event.getEntity()).getItemStack().getType() == Material.DRAGON_EGG) {
                Egg.data.state = State.ITEM;
                Egg.save();
            }
        } else {
            // Fixes being able to push the egg into an unloaded end chunk while in the virgin state
            // by making it functionally impossible to push it far enough in time
            if (event.getEntityType() == EntityType.DROPPED_ITEM
                    && ((Item) event.getEntity()).getItemStack().getType() == Material.DRAGON_EGG) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (Egg.data.state == State.VIRGIN) {
                        event.getEntity().remove();
                        plugin.resetEgg();
                    }
                }, 20 * 15);
            }
        }

    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
            event.getItem().remove();
        }

        UUID thrower = event.getItem().getThrower();
        if (thrower != null) {
            Player player = Bukkit.getPlayer(thrower);

            if (player != null) {
                plugin.getServer().broadcast(Component.text(player.getName() + " tried to throw the egg in a hopper!").color(COLOR));
            }
        }

        plugin.getLogger().info("Intercepted hopper pickup");

        plugin.resetEgg();

    }

    // This is actually for hoppers/dispensers, and does not get called for players putting items in containers.
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (Egg.data.state != State.VIRGIN) {

            if (event.getItem().getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                plugin.getServer().broadcast(Component.text("Hopper shenanigans detected!").color(COLOR));

                plugin.resetEgg();

                plugin.getLogger().info("Intercepted inventory move");
            }
        }
    }

    // THIS is the one for that
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (Egg.data.state != State.VIRGIN) {

            Player player = (Player) event.getWhoClicked();
            Inventory clicked = event.getClickedInventory();

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack stack = event.getCurrentItem();
                if (stack != null && stack.getType() == Material.DRAGON_EGG) {
                    event.setCancelled(true);
                    plugin.getLogger().info("Intercepted shift click");
                }
            }

            if (player.getInventory() != clicked && event.getCursor() != null
                    && event.getCursor().getType() == Material.DRAGON_EGG) {
                event.setCancelled(true);
                plugin.getLogger().info("Intercepted drag");
            }

            if (event.isCancelled()) {
                plugin.getServer().broadcast(Component.text("Someone tried to put the egg in a container!").color(COLOR));
                player.getInventory().remove(Material.DRAGON_EGG);
                plugin.resetEgg();
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (Egg.data.state != State.VIRGIN) {

            if (event.getPlayer().getInventory().contains(Material.DRAGON_EGG)) {
                plugin.getServer().broadcast(Component.text("Someone logged out with the egg in their inventory!").color(COLOR));
                event.getPlayer().getInventory().remove(Material.DRAGON_EGG);
                plugin.resetEgg();
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (item.getItemStack().getType() == Material.DRAGON_EGG) {
                UUID thrower = item.getThrower();
                if (thrower != null) {
                    Player player = Bukkit.getPlayer(thrower);
                    if (player != null) {
                        StringBuilder messageBuilder = new StringBuilder("Someone ");

                        switch (event.getCause()) {
                            case ENTITY_EXPLOSION, BLOCK_EXPLOSION -> messageBuilder.append("blew up the egg!");
                            case CONTACT -> messageBuilder.append("dropped the egg on a cactus or berry bush!");
                            case FIRE, FIRE_TICK -> messageBuilder.append("burned the egg!");
                            case LAVA -> messageBuilder.append("threw the egg in lava!");
                            case LIGHTNING ->
                                    messageBuilder.append("let the egg be struck by lightning (and possibly caused the lightning)!");
                            case VOID -> messageBuilder.append("threw the egg into the void!");
                            default -> messageBuilder.append("allowed the egg to be destroyed!");
                        }

                        messageBuilder.append(" Point and laugh at this user!");

                        plugin.getServer().broadcast(Component.text(messageBuilder.toString()).color(COLOR));
                    }
                } else {
                    plugin.getServer().broadcast(Component.text("The egg was destroyed!").color(COLOR));
                }

                plugin.getLogger().info("Intercepted entity damage");
                item.remove();
                plugin.resetEgg();
            }
        }
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            UUID thrower = event.getEntity().getThrower();
            if (thrower != null) {
                Player player = Bukkit.getPlayer(thrower);
                if (player != null) {
                    plugin.getServer().broadcast(Component.text("Someone let the egg despawn! What a dumbass!").color(COLOR));
                    plugin.getLogger().info("Intercepted despawn");
                    plugin.resetEgg();
                }
            }
        }
    }

    @EventHandler
    public void onPickupItem(EntityPickupItemEvent event) {
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            if (!(event.getEntity() instanceof Player player)) {
                // Ensure mobs can't pick it up
                event.setCancelled(true);
            } else {
                Egg.data.state = State.INVENTORY;
                Egg.data.holder = player.getUniqueId();
                Egg.data.expiry = Instant.now().getEpochSecond() + Macguffin2.INVENTORY_TIME_LIMIT;
                Egg.save();
                player.sendMessage(Component.text("You have 20 minutes to place the egg, or it will teleport to spawn. Make haste!"));
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    // It's technically possible to mine the egg like a normal block in certain situations. TIL.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            Egg.data.setLocation(null);
            Egg.save();
            plugin.resetEgg();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            if (Egg.data.state != State.RESPAWNED) {
                plugin.getLogger().info("Intercepted block place");
                if (Egg.data.holder != null) {
                    Player player = Bukkit.getPlayer(Egg.data.holder);
                    if (player != null) {
                        player.sendMessage(Component.text("Egg placed. It must be right-clicked every 36 hours, or it "
                                + "will teleport back to spawn.").color(COLOR));
                    }
                }

                Egg.data.state = State.PLACED;
                Egg.data.setLocation(event.getBlockPlaced().getLocation());
                Egg.data.expiry = Instant.now().getEpochSecond() + Macguffin2.TAP_INTERVAL;
                Egg.save();
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getTo() == Material.DRAGON_EGG) {
            Egg.data.setLocation(event.getBlock().getLocation());
            Egg.save();
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().getType() == Material.DRAGON_EGG
                || event.getPlayer().getInventory().getItemInOffHand().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }

        if (event.getRightClicked() instanceof Player targetPlayer) {
            if (Egg.data.state == State.INVENTORY && targetPlayer.getUniqueId() == Egg.data.holder) {
                event.getPlayer().sendMessage(Component.text(targetPlayer.getName() + " is kinda sus").color(COLOR));
            } else {
                event.getPlayer().sendMessage(Component.text(targetPlayer.getName() + " isn't sus").color(COLOR));
            }
        }
    }

    @EventHandler
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        if (Egg.data.state == State.INVENTORY && event.getEntity().getUniqueId() == Egg.data.holder
            && event.getAction() == EntityPotionEffectEvent.Action.ADDED
            && event.getModifiedType() == PotionEffectType.INVISIBILITY) {
            event.setCancelled(true);
        }
    }
}
