package gay.realmromp.macguffin2;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;

import static gay.realmromp.macguffin2.Macguffin2.COLOR;

public class EggTimeCheckTask extends BukkitRunnable {

    private final Macguffin2 plugin;

    public EggTimeCheckTask(Macguffin2 plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (Instant.now().getEpochSecond() >= Egg.data.expiry) {
            if (Egg.data.state == State.INVENTORY) {
                plugin.getLogger().info("Resetting egg (inventory timeout)");
                if (Egg.data.holder != null) {
                    Player player = Bukkit.getPlayer(Egg.data.holder);
                    if (player != null) {
                        player.sendMessage(Component.text("Dumbass").color(COLOR));
                        Bukkit.broadcast(Component.text(player.getName() + " took too long to place the egg! "
                            + "Laugh at this user.").color(COLOR));
//                        Egg.removeFrom(player);
                        plugin.resetEgg();
                    }
                } else {
                    plugin.getLogger().severe("Egg timed out in inventory, but no holder was set! The egg is duped.");
                }
                plugin.resetEgg();
            } else if (Egg.data.state == State.PLACED) {
                plugin.getLogger().info("Resetting egg (placed timeout)");
                plugin.resetEgg();
            }
        }
    }

}
