package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.MuteManager;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "mute", description = "Mutes a player.", usage = "/<command> <player>")
public class MuteCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        MuteManager mum = plugin.muteManager;
        if (args.length == 0)
        {
            msg(sender, "<gray><amount> player(s) are currently muted.",
                    Placeholder.unparsed("amount", String.valueOf(mum.getMuteCount())));
            return false;
        }

        if (args[0].equalsIgnoreCase("purge") && Bukkit.getPlayer("purge") == null)
        {
            msg(sender, "<gray><amount> players were unmuted.",
                    Placeholder.unparsed("amount", String.valueOf(mum.wipeMutes())));
            FUtil.staffAction(sender, "Unmuted all players");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        if (target.hasPermission("kfc.command.mute.immune"))
        {
            msg(sender, "<red>That player can't be muted.");
            return true;
        }

        mum.setMuted(target, !mum.isMuted(target));
        if (mum.isMuted(target))
        {
            FUtil.staffAction(sender, "Muted <player>", Placeholder.unparsed("player", target.getName()));
            plugin.recordKeeper.recordPunishment(Punishment.builder()
                    .uuid(target.getUniqueId())
                    .name(target.getName())
                    .ip(FUtil.getIp(target))
                    .by(sender.getName())
                    .type("MUTE")
                    .build());
        }
        else
        {
            FUtil.staffAction(sender, "Unmuted <player>", Placeholder.unparsed("player", target.getName()));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream()
                .filter(player ->!player.hasPermission("kfc.command.mute.immune"))
                .map(Player::getName).toList() : List.of();
    }
}
