package gay.realmromp.macguffin2;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nullable;
import java.io.*;
import java.util.UUID;

public class Egg implements Serializable {
    @Serial
    private static final long serialVersionUID = 42069L;

//    private static boolean loaded = false;
    public static Egg data;
    public static void load() {
            File dataDir = JavaPlugin.getProvidingPlugin(Macguffin2.class).getDataFolder();
            File dataFile = new File(dataDir, "data.bin");

            try {
                BukkitObjectInputStream in = new BukkitObjectInputStream(new FileInputStream(dataFile));
                data = (Egg) in.readObject();
//                loaded = true;

            } catch (Exception e) {
                if (e instanceof EOFException) {
                    setVirgin();
                } else {
                    JavaPlugin.getProvidingPlugin(Macguffin2.class).getLogger().severe("Fatal: " + e);
                    Bukkit.shutdown();
                }

            }
    }

    public static void save() {
        File dataDir = JavaPlugin.getProvidingPlugin(Macguffin2.class).getDataFolder();
        File dataFile = new File(dataDir, "data.bin");

        if (dataFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFile.delete();
        }
        try {
            BukkitObjectOutputStream out = new BukkitObjectOutputStream(new FileOutputStream(dataFile));
            out.writeObject(data);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // only run ONCE EVER!!! RESETS THE EGG STATE!!!!
    public static void setVirgin() {
        data = new Egg();
        data.state = State.VIRGIN;
        data.world = null;
        data.holder = null;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean hasEgg(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        if (stack.getType() == Material.DRAGON_EGG) {
            return true;
        } else if (stack.getType() == Material.BUNDLE) {
            return ((BundleMeta) stack.getItemMeta()).getItems().stream().anyMatch((ItemStack is) -> is.getType() == Material.DRAGON_EGG);
        }

        return false;
    }

    public static void removeFrom(Player player) {
        if (hasEgg(player.getItemOnCursor())) {
            // What's that? The bundle had other stuff? Tough shit lol
            player.setItemOnCursor(null);
        } else {
            ItemStack[] stacks = player.getInventory().getContents();
            for (ItemStack stack : stacks) {
                if (hasEgg(stack)) {
                    player.getInventory().remove(stack);
                }
            }
        }
    }

    private String world;
    private double x, y, z;

    public State state;
    public UUID holder; // contains last holder in non-inventory states

    public long expiry; // unix timestamp for when the egg should respawn

    public void setLocation(@Nullable Location loc) {
        world = loc != null ? loc.getWorld().getName() : null;
        x = loc != null ? loc.getX() : 0;
        y = loc != null ? loc.getY() : 0;
        z = loc != null ? loc.getZ() : 0;
    }

    public Location getLocation() {
        if (this.world == null) {
            return null;
        }
        World w = Bukkit.getWorld(this.world);
        if (w == null) {
            JavaPlugin.getProvidingPlugin(Macguffin2.class)
                    .getLogger().severe("Could not load world " + this.world + "! Exiting!");
            Bukkit.shutdown();
        }

        return new Location(w, x, y, z);
    }

    private Egg(){}

    public String toString() {
        return "State: " + state.toString() + "\n" + "World: " + world + "\n" +
                "X: " + x + "\n" +
                "Y: " + y + "\n" +
                "Z: " + z;
    }
}
