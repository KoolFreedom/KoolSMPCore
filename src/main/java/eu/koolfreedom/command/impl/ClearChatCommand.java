package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.command.FreedomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClearChatCommand extends FreedomCommand {
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (!sender.hasPermission("kf.admin"))
        {
            msg(Messages.MSG_NO_PERMS);
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(KoolSMPCore.main, () ->
        {
            for (Player players : Bukkit.getOnlinePlayers())
            {
                for (int i = 0; i < 2500; i++)
                    players.sendMessage(Component.text(getRandomColorCode().repeat(KoolSMPCore.random.nextInt(1, 12))));
                players.sendMessage(Component.text("The chat has been cleared by ", NamedTextColor.RED).append(Component.text(sender.getName(), NamedTextColor.GOLD)));
                players.sendMessage(Component.text(""));
            }
        });
        return true;
    }

    private String getRandomColorCode()
    {
        char[] colors = "0123456789abcdefklmnor".toCharArray();
        return "§" + colors[KoolSMPCore.random.nextInt(colors.length)];
    }
}