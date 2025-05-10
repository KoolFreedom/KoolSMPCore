package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "clearchat", description = "Clears the chat for non-admins online", aliases = {"cc", "cleanchat"})
public class ClearChatCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            // Messages are not unique between players with this implementation, mainly for better optimization, but it
            //  still works pretty effectively
            for (int i = 0; i < 2000; i++)
            {
                // Randomized space and newlines to bypass anti-spam measures
                Component blank = Component.space();
                for (int f = 0; f < FUtil.randomNumber(0, 250); f++)
                {
                    blank = blank.append(Component.newline());
                }

                final Component blankComponent = blank.color(TextColor.color(FUtil.randomNumber(0, 0xFFFFFF)));

                Bukkit.getOnlinePlayers().stream().filter(player -> !player.hasPermission("kfc.command.clearchat.immune"))
                        .forEach(player -> player.sendMessage(blankComponent));
            }

            FUtil.staffAction(sender, "Cleared the chat");
        });

        return true;
    }
}