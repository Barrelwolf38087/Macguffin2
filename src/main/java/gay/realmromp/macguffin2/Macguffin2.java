package gay.realmromp.macguffin2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class Macguffin2 extends JavaPlugin {

    // 36 hours in seconds
    public static final long TAP_INTERVAL = 60 * 60 * 36;

//     20 minutes in seconds
    public static final long INVENTORY_TIME_LIMIT = 60 * 20;

    // This one's for testing
//    public static final long INVENTORY_TIME_LIMIT = 30;
//    public static final long TAP_INTERVAL = 30;


    public static final TextColor COLOR = TextColor.color(227, 60 , 34);

    public void resetEgg() {
        // Delete the old egg
        if (Egg.data.holder != null) {
            Player player = Bukkit.getPlayer(Egg.data.holder);
            if (player == null) {
                getLogger().severe("Warning: last holder had UUID " + Egg.data.holder.toString()
                        + ", but this player does not exist! The egg may be duped.\n");
            } else {
                getLogger().info("Removing egg from " + player.getName() + " (UUID " + player.getUniqueId()
                    + ")'s inventory");

                Egg.removeFrom(player);
            }
        }

        Location loc = Egg.data.getLocation();
        if (loc != null) {
            Block block = loc.getBlock();
            block.setType(Material.AIR);

//            // Doesn't work :(
//            getServer().getScheduler().runTaskLater(this, () -> {
//                Sign sign = (Sign) block.getState();
//                sign.line(0, Component.text("getting milk brb"));
//            }, 1);

            getLogger().info("Egg removed from " + loc);
        }

        // Make the new one

        Egg.data.state = State.RESPAWNED;

        // Hyrum's Law:
        // With a sufficient number of users of an API, it does not matter what you promise in the contract:
        // all observable behaviors of your system will be depended on by somebody.
        World world = getServer().getWorlds().get(0); // Forums say this gets the "main" overworld (undocumented)

        Block spawnAbove = world.getHighestBlockAt(0, 0);
        if (spawnAbove.getLocation().getBlockY() > 319) {
            spawnAbove = world.getBlockAt(0, 319, 0);
        }

        spawnAbove.setType(Material.OBSIDIAN);

        Block spawnAt = spawnAbove.getRelative(BlockFace.UP);
        spawnAt.setType(Material.DRAGON_EGG);

        Egg.data.setLocation(spawnAt.getLocation());

        getLogger().info("Egg respawned");
        getServer().sendActionBar(Component.text("The egg has been respawned!").color(COLOR));

        Egg.save();
    }

    @Override
    public void onEnable() {
        getLogger().info("Starting Macguffin " + this.getDescription().getVersion());
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (!dataFolder.mkdir()) {
                getLogger().severe("Data folder does not exist, but we cannot create it!");
                Bukkit.shutdown();
            }
        }
        File dataFile = new File(dataFolder, "data.bin");
        if (!dataFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe(e.toString());
                Bukkit.shutdown();
            }
        }

        Egg.load();

        getServer().getPluginManager().registerEvents(new EggListener(this), this);

        EggTimeCheckTask timeCheckTask = new EggTimeCheckTask(this);
        timeCheckTask.runTaskTimer(this, 0, 20 * 30);

        Objects.requireNonNull(getCommand("sniff")).setExecutor(new EggSniffCommand());

        getLogger().info("Successfully started Macguffin " + this.getDescription().getVersion());
    }


}
