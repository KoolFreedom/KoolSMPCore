package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "mutechat", description = "Mutes the chat for non-staff")
public class MuteChatCommand extends KoolCommand
{
    @Setter
    @Getter
    private static boolean chatMuted = false;

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        chatMuted = !chatMuted;
        String status = chatMuted ? "Muted" : "Unmuted";
        FUtil.staffAction(sender, status + " global chat");
        return true;
    }
}
