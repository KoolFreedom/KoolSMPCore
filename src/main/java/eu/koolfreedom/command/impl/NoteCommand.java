package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.note.PlayerNote;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CommandParameters(
        name = "note",
        description = "Add or view staff notes",
        usage = "/note <add|view|remove> <player> [message|index]",
        aliases = {"notes"}
)
public class NoteCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("add").then(argument("target", StringArgumentType.word())
                        .then(argument("message", StringArgumentType.greedyString())
                                .executes(executes(ctx -> add(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "message")))))))
                .then(literal("remove").then(argument("target", StringArgumentType.word())
                        .then(argument("index", IntegerArgumentType.integer(1))
                                .executes(executes(ctx -> remove(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                        IntegerArgumentType.getInteger(ctx, "index")))))))
                .then(literal("view").then(argument("target", StringArgumentType.word())
                        .executes(executes(ctx -> view(sender(ctx), StringArgumentType.getString(ctx, "target"))))));
    }

    private void add(CommandSender sender, String targetName, String message)
    {
        if (!sender.hasPermission("kfc.notes.add"))
        {
            msg(sender, noPermission);
            return;
        }

        OfflinePlayer target = resolveTarget(sender, targetName);
        if (target == null) return;

        PlayerNote note = new PlayerNote(sender.getName(), message, LocalDateTime.now());
        plugin.getNoteManager().addNote(target.getUniqueId(), note);

        msg(sender, "<green>Note added for <yellow><player></yellow>.",
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
    }

    private void remove(CommandSender sender, String targetName, int index)
    {
        if (!sender.hasPermission("kfc.notes.remove"))
        {
            msg(sender, noPermission);
            return;
        }

        OfflinePlayer target = resolveTarget(sender, targetName);
        if (target == null) return;

        UUID targetUUID = target.getUniqueId();
        List<PlayerNote> noteList = plugin.getNoteManager().getNotes(targetUUID);

        if (noteList.isEmpty())
        {
            msg(sender, "<red>That player has no notes.");
            return;
        }

        int zeroIndex = index - 1;
        if (zeroIndex < 0 || zeroIndex >= noteList.size())
        {
            msg(sender, "<red>Note index out of bounds.");
            return;
        }

        // Get the note first, then remove via NoteManager which handles both the DB write and the
        // session cache — never mutate the unmodifiable list returned by getNotes() directly.
        PlayerNote removed = noteList.get(zeroIndex);
        plugin.getNoteManager().removeNote(targetUUID, removed);

        msg(sender, "<green>Removed note #<index> from <yellow><player></yellow>.",
                Placeholder.unparsed("index", String.valueOf(index)),
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
    }

    private void view(CommandSender sender, String targetName)
    {
        if (!sender.hasPermission("kfc.notes.view"))
        {
            msg(sender, noPermission);
            return;
        }

        OfflinePlayer target = resolveTarget(sender, targetName);
        if (target == null) return;

        List<PlayerNote> notes = plugin.getNoteManager().getNotes(target.getUniqueId());
        if (notes.isEmpty())
        {
            msg(sender, "<gray>No notes for <player>.",
                    Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
            return;
        }

        msg(sender, "<gold>Notes for <yellow><player></yellow>:</gold>",
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));

        for (int i = 0; i < notes.size(); i++)
        {
            PlayerNote n = notes.get(i);
            msg(sender,
                    "<gray>" + (i + 1) + ". <white>[<time>] <author>: <message></white>",
                    Placeholder.unparsed("time", n.timestamp().toString()),
                    Placeholder.unparsed("author", n.author()),
                    Placeholder.unparsed("message", n.message()));
        }
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String targetName)
    {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline())
        {
            msg(sender, playerNotFound);
            return null;
        }
        return target;
    }
}
