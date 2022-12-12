package gay.realmromp.macguffin2;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EggSniffCommand implements CommandExecutor {

    public static final int SNIFF_RADIUS = 16 * 10; // 10 chunks = server render distance

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            Location loc;
            if (Egg.data.state == State.PLACED || Egg.data.state == State.RESPAWNED) {
                loc = Egg.data.getLocation();
            } else if (Egg.data.state == State.INVENTORY) {
                loc = Objects.requireNonNull(Bukkit.getPlayer(Egg.data.holder)).getLocation();
            } else {
                return true;
            }

            Location pl = player.getLocation();
            if (loc.getBlockX() - SNIFF_RADIUS <= pl.getBlockX() && pl.getBlockX() <= loc.getBlockX() + SNIFF_RADIUS
                && loc.getBlockZ() - SNIFF_RADIUS <= pl.getBlockZ() && pl.getBlockZ() <= loc.getBlockZ() + SNIFF_RADIUS) {
                player.sendMessage(Component.text("The smell of egg emanates from (" + loc.getBlockX() + ", "
                    + loc.getBlockZ() + ")...").color(Macguffin2.COLOR));
            } else {
                player.sendMessage(Component.text("You don't smell the egg.").color(Macguffin2.COLOR));
            }
        }

        return true;
    }
}
