package eu.koolfreedom.command.impl;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.listener.impl.MuteManager;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.TimeOffset;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandParameters(name = "mute", description = "Mutes a player with an optional duration and reason.", usage = "/<command> <player> [duration] [reason]")
public class MuteCommand extends KoolCommand
{
    private static final Pattern DURATION_FORMAT = Pattern.compile("(?:[0-9]+(?:mil|mo|ms|s|m|h|d|w|y|c))+");

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("purge").executes(executes(ctx -> purge(sender(ctx)))))
                .then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target"), null)))
                        .then(argument("spec", StringArgumentType.greedyString())
                                .executes(executes(ctx -> toggleMute(sender(ctx), player(ctx, "target"),
                                        StringArgumentType.getString(ctx, "spec"))))));
    }

    private void purge(CommandSender sender)
    {
        int unmuted = plugin.getMuteManager().wipeMutes();

        msg(sender, "<gray><amount> players were unmuted.",
                Placeholder.unparsed("amount", String.valueOf(unmuted)));
        FUtil.staffAction(sender, "Unmuted all players");
    }

    private void toggleMute(CommandSender sender, Player target, String spec)
    {
        MuteManager mum = plugin.getMuteManager();

        if (target.hasPermission("kfc.admin"))
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
            return;
        }

        String trimmed = spec == null ? "" : spec.trim();
        long durationTicks = Long.MAX_VALUE;
        String reason = null;

        if (!trimmed.isEmpty())
        {
            String[] parts = trimmed.split("\\s+", 2);
            String first = parts[0].toLowerCase(Locale.ROOT);
            if (isDurationCandidate(first))
            {
                durationTicks = parseMuteDuration(first);
                if (durationTicks <= 0)
                {
                    msg(sender, "<red>Invalid duration format. Example: 1d, 2h, 30m, perm");
                    return;
                }
                reason = parts.length > 1 ? parts[1].trim() : null;
            }
            else
            {
                reason = trimmed;
            }
        }

        if (durationTicks == Long.MAX_VALUE)
        {
            mum.mute(uuid);
        }
        else
        {
            mum.mute(uuid, durationTicks);
        }

        FUtil.staffAction(sender, "Muted <player>", Placeholder.unparsed("player", name));

        plugin.getRecordKeeper().recordPunishment(Punishment.builder()
                .uuid(uuid)
                .name(name)
                .ip(FUtil.getIp(target))
                .by(sender.getName())
                .reason(reason == null ? "" : reason)
                .type("MUTE")
                .build());
    }

    private boolean isDurationCandidate(String value)
    {
        return value.equals("perm") || value.equals("permanent") || (!value.isEmpty() && Character.isDigit(value.charAt(0)));
    }

    private long parseMuteDuration(String durationArg)
    {
        if (durationArg == null || durationArg.isBlank())
        {
            return Long.MAX_VALUE;
        }

        String dur = durationArg.toLowerCase(Locale.ROOT);
        if (dur.equals("perm") || dur.equals("permanent"))
        {
            return Long.MAX_VALUE;
        }

        if (!DURATION_FORMAT.matcher(dur).matches())
        {
            return -1L;
        }

        try
        {
            long ticks = TimeOffset.getOffset(dur) / 50L;
            return ticks > 0 ? ticks : -1L;
        }
        catch (NumberFormatException exception)
        {
            return -1L;
        }
    }

}
