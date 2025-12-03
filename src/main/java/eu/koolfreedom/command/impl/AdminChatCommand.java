package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import eu.koolfreedom.util.FUtil;
import org.bukkit.entity.Player;

@CommandParameters(name = "adminchat", description = "Speak with other staff members", aliases = {"o", "oc", "ac"})
public class AdminChatCommand extends KoolCommand
{
    private final Namespaced key = NamespacedKey.fromString("ingame_command", plugin);

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        FUtil.adminChat(sender, plugin.getGroupManager().getSenderGroup(sender), Component.text(StringUtils.join(args, " ")), key);
        return true;
    }
}
