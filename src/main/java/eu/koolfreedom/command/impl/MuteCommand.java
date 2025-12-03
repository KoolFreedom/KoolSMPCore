package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@CommandParameters(name = "mute", description = "Mutes a player with optional duration and reason.", usage = "/<command> <player>")
public class MuteCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String label, String[] args)
    {
        MuteManager mum = plugin.getMuteManager();

        if (args.length == 0)
        {
            msg(sender, "<gray><amount> player(s) are currently muted.",
                    Placeholder.unparsed("amount", String.valueOf(mum.getMuteCount())));
            return false;
        }

        if (args[0].equalsIgnoreCase("purge"))
        {
            int unmuted = mum.wipeMutes();
            msg(sender, "<gray><amount> players were unmuted.",
                    Placeholder.unparsed("amount", String.valueOf(unmuted)));
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

        UUID uuid = target.getUniqueId();
        String name = target.getName();

        if (mum.isMuted(uuid))
        {
            mum.unmute(uuid);
            FUtil.staffAction(sender, "Unmuted <player>", Placeholder.unparsed("player", name));
            plugin.getAutoUndoManager().cancelAutoUnmute(uuid);
            return true;
        }

        // Mute
        mum.mute(uuid);
        FUtil.staffAction(sender, "Muted <player>", Placeholder.unparsed("player", name));
        plugin.getAutoUndoManager().scheduleAutoUnmute(target);

        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                .uuid(uuid)
                .name(name)
                .ip(FUtil.getIp(target))
                .by(sender.getName())
                .type("MUTE")
                .build());

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        return args.length == 1
                ? Bukkit.getOnlinePlayers().stream()
                .filter(player -> !player.hasPermission("kfc.command.mute.immune"))
                .map(Player::getName).toList()
                : List.of();
    }
}
