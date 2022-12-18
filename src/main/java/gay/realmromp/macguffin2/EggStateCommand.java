package gay.realmromp.macguffin2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class EggStateCommand  implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.isOp()) {
            sender.sendMessage(Egg.data.toString());
        } else {
            sender.sendMessage("Not opped");
        }
        return true;
    }
}
