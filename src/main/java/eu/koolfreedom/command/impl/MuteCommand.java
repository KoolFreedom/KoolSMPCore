package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@CommandParameters(name = "mute", description = "Mutes a player with optional duration and reason.", usage = "/<command> <player>")
public class MuteCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("purge").executes(executes(ctx -> purge(sender(ctx)))))
                .then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target")))));
    }

    private void purge(CommandSender sender)
    {
        int unmuted = plugin.getMuteManager().wipeMutes();
        msg(sender, "<gray><amount> players were unmuted.",
                Placeholder.unparsed("amount", String.valueOf(unmuted)));
        FUtil.staffAction(sender, "Unmuted all players");
    }

    private void toggleMute(CommandSender sender, Player target)
    {
        MuteManager mum = plugin.getMuteManager();

        if (target.hasPermission("kfc.command.mute.immune"))
        {
            msg(sender, "<red>That player can't be muted.");
            return;
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName();

        if (mum.isMuted(uuid))
        {
            mum.unmute(uuid);
            FUtil.staffAction(sender, "Unmuted <player>", Placeholder.unparsed("player", name));
            plugin.getAutoUndoManager().cancelAutoUnmute(uuid);
            return;
        }

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
    }
}
