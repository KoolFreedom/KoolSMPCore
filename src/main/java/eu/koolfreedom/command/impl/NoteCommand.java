package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.note.PlayerNote;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@CommandParameters(
        name = "note",
        description = "Add or view staff notes",
        usage = "/note <add|view|remove> <player> [message|index]",
        aliases = {"notes"}
)
@SuppressWarnings("ConstantConditions")
public class NoteCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player player, Command cmd,
                       String label, String[] args)
    {
        if (args.length < 2) // need at least sub-cmd + player
            return false;

        String sub = args[0].toLowerCase();
        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
        {
            msg(sender, playerNotFound);
            return true;
        }

        UUID uuid = target.getUniqueId();

        switch (sub) {
            /* ========== /note add ========== */
            case "add" -> {
                if (!sender.hasPermission("kfc.notes.add")) {
                    msg(sender, "<red>You lack permission to add notes.");
                    return true;
                }
                if (args.length < 3) return false; // no message given

                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                PlayerNote note = new PlayerNote(sender.getName(), message, LocalDateTime.now());
                plugin.getNoteManager().addNote(uuid, note);

                msg(sender, "<green>Note added for <yellow><player></yellow>.",
                        Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
                return true;
            }

            /* ========== /note remove ======== */
            case "remove" -> {
                if (!sender.hasPermission("kfc.notes.remove")) {
                    msg(sender, "<red>No permission!");
                    return true;
                }
                if (args.length < 3) return false;

                handleNoteRemove(sender, target, args[2]);
                return true;
            }

            /* ========== /note view ========== */
            case "view" -> {
                if (!sender.hasPermission("kfc.notes.view")) {
                    msg(sender, "<red>You lack permission to view notes.");
                    return true;
                }

                var notes = plugin.getNoteManager().getNotes(uuid);
                if (notes.isEmpty()) {
                    msg(sender, "<gray>No notes for <player>.",
                            Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
                    return true;
                }

                msg(sender, "<gold>Notes for <yellow><player></yellow>:</gold>",
                        Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));

                for (int i = 0; i < notes.size(); i++) {
                    PlayerNote n = notes.get(i);
                    msg(sender,
                            "<gray>" + (i+1) + ". <white>[<time>] <author>: <message></white>",
                            Placeholder.unparsed("time", n.timestamp().toString()),
                            Placeholder.unparsed("author", n.author()),
                            Placeholder.unparsed("message", n.message())
                    );
                }
                return true;
            }
        }

        return false; // unknown sub-command
    }

    private void handleNoteRemove(CommandSender sender, OfflinePlayer target, String indexArg)
    {
        UUID targetUUID = target.getUniqueId();
        List<PlayerNote> noteList = plugin.getNoteManager().getNotes(targetUUID);

        if (noteList == null || noteList.isEmpty())
        {
            sender.sendMessage(FUtil.miniMessage("<red>That player has no notes."));
            return;
        }

        int index;
        try {
            index = Integer.parseInt(indexArg) - 1;
        } catch (NumberFormatException ex) {
            sender.sendMessage(FUtil.miniMessage("<red>Invalid note index: <i>" + indexArg + "</i>"));
            return;
        }

        if (index < 0 || index >= noteList.size()) {
            sender.sendMessage(FUtil.miniMessage("<red>Note index out of bounds."));
            return;
        }

        PlayerNote removed = noteList.remove(index);
        plugin.getNoteManager().removeNote(targetUUID, removed);

        sender.sendMessage(FUtil.miniMessage("<green>Removed note #" + (index + 1) +
                " from <yellow>" + target.getName() + "</yellow>."));
    }
}
