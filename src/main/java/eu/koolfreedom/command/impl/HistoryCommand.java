package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.punishment.Punishment;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

@CommandParameters(name = "history", description = "Shows a player's punishment history.", usage = "/history <player> [page-size-limit]")
public class HistoryCommand extends KoolCommand
{
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> showHistory(sender(ctx), StringArgumentType.getString(ctx, "target"), DEFAULT_LIMIT)))
                .then(argument("limit", IntegerArgumentType.integer(1, MAX_LIMIT))
                        .executes(executes(ctx -> showHistory(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "limit"))))));
    }

    private void showHistory(CommandSender sender, String target, int limit)
    {
        Collection<Punishment> history = plugin.getRecordKeeper().getPunishmentHistory(target, limit);
        if (history.isEmpty())
        {
            msg(sender, "<gray>No punishment history found for <yellow><target></yellow>.",
                    Placeholder.unparsed("target", target));
            return;
        }

        msg(sender, "<dark_gray>━━━━━━━━ <gold>Punishment History: <target> (<count>)</gold> ━━━━━━━━",
                Placeholder.unparsed("target", target), Placeholder.unparsed("count", String.valueOf(history.size())));
        for (Punishment punishment : history)
        {
            msg(sender, "<dark_gray>• <yellow><type></yellow> <gray>by <white><staff></white> <dark_gray>| <gray><date> <dark_gray>| <gray><reason>",
                    Placeholder.unparsed("type", valueOr(punishment.getType(), "UNKNOWN")),
                    Placeholder.unparsed("staff", valueOr(punishment.getBy(), "Unknown")),
                    Placeholder.unparsed("date", DATE_FORMAT.format(Instant.ofEpochMilli(punishment.getIssued()))),
                    Placeholder.unparsed("reason", valueOr(punishment.getReason(), "No reason provided")));
        }
    }

    private String valueOr(String value, String fallback)
    {
        return value == null || value.isBlank() ? fallback : value;
    }
}
