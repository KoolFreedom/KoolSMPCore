/*
/
/ I realize this class is called FreedomCommand (similar to TotalFreedomMod), and that was because I was
/ wanting to re-create TFM's FrontDoor and it had calls to FreedomCommand and I was honestly
/ too lazy to change the name, this will remain like this considering my server name *is* "KoolFreedom"
/
*/
package eu.koolfreedom.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation")
public abstract class FreedomCommand implements CommandExecutor
{
    private static final Map<String, FreedomCommand> commands = new HashMap<>();

    public static Map<String, FreedomCommand> getCommands() {
        return commands;
    }

    public static FreedomCommand getFrom(Command command) {
        return commands.get(command.getName().toLowerCase());
    }

    public void register(String name) {
        commands.put(name.toLowerCase(), this);
    }

    protected CommandSender sender;

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String commandLabel, @NotNull String[] args)
    {
        try
        {
            boolean run = run(sender, sender instanceof ConsoleCommandSender ? null : (Player)sender, cmd, commandLabel, args, sender instanceof ConsoleCommandSender);
            if (!run)
            {
                sender.sendMessage(ChatColor.WHITE + cmd.getUsage().replace("<command>", cmd.getLabel()));
                return true;
            }
        }
        catch (CommandFailException ex)
        {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        }
        return false;
    }

    public abstract boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole);

    protected boolean isConsole()
    {
        return sender instanceof ConsoleCommandSender;
    }

    protected Player getPlayer(String name)
    {
        return Bukkit.getPlayer(name);
    }

    public class CommandFailException extends RuntimeException
    {

        private static final long serialVersionUID = -92333791173123L;

        public CommandFailException(String message)
        {
            super(message);
        }

    }

    protected void msg(CommandSender sender, String message)
    {
        sender.sendMessage(ChatColor.GRAY + message);
    }

    protected void msg(Player player, String message)
    {
        player.sendMessage(ChatColor.GRAY + message);
    }

    protected void msg(Player player, String message, ChatColor color)
    {
        player.sendMessage(color + message);
    }

    protected void msg(String message)
    {
        msg(sender, message);
    }

    protected void msg(String message, ChatColor color)
    {
        msg(color + message);
    }

    protected void msg(String message, net.md_5.bungee.api.ChatColor color)
    {
        msg(color + message);
    }
}
