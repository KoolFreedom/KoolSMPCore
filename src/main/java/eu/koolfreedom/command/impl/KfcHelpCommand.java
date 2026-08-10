package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "kfchelp", description = "Sends a help message about KoolSmpCore", usage = "/kfchelp")

public class KfcHelpCommand extends KoolCommand {

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args) {
        msg(sender, "<dark_gray>--------------------------------");
        msg(sender, "<gold><Bold>KoolSmpCore Help"
        msg(sender, "<red>If you need help setting up KoolSmpCore, please visit our discord here: https://discord.gg/MTYrSgVkmd");
        msg(sender, "<red>Or run /help KoolSmpCore for help with commands.");
        msg(sender, "<red>You can also configure stuff in the plugins config.yml folder.");
        msg(sender, "<red>This can be found in<gold> plugins/koolsmpcore/config.yml");
        msg(sender, "<dark_gray>--------------------------------");
        
        return true;
    }
}
