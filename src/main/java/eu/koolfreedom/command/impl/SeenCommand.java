package eu.koolfreedom.command.impl;

import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.note.NoteManager;
import eu.koolfreedom.note.PlayerNote;
import eu.koolfreedom.listener.MuteManager;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.stats.PlaytimeManager;
import eu.koolfreedom.stats.PlaytimeManager.PlaytimeData;
import eu.koolfreedom.util.FUtil;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandParameters(name = "seen", description = "Shows information about a player or IP", usage = "/seen <player|ip>")
public class SeenCommand extends KoolCommand
{
    private final BanManager banManager = plugin.getBanManager();
    private final NoteManager noteManager = plugin.getNoteManager();
    private final MuteManager muteManager = plugin.getMuteManager();
    private final FreezeManager freezeManager = plugin.getFreezeManager();
    private final PlaytimeManager playtimeManager = plugin.getPlaytimeManager();
    private final IEssentials essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public boolean run(CommandSender sender, Player player, Command cmd, String label, String[] args)
    {
        if (args.length != 1)
        {
            msg(sender, "<gray>Usage: /seen <player|ip>");
            return true;
        }

        String input = args[0];

        if (FUtil.isValidIP(input))
        {
            List<OfflinePlayer> targets = FUtil.getOfflinePlayersByIp(input);
            if (targets.isEmpty())
            {
                msg(sender, "<red>No players found with IP <white>" + input + "</white>.");
                return true;
            }
            msg(sender, FUtil.miniMessage("<gold>Players with IP <white>" + input + "</white>:"));
            targets.forEach(p -> displayPlayer(sender, p, input));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(input);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline()))
        {
            target = Bukkit.getPlayerExact(input);
            if (target == null)
            {
                msg(sender, "<red>Player not found: <white>" + input + "</white>");
                return true;
            }
        }
        displayPlayer(sender, target, null);
        return true;
    }

    private void displayPlayer(CommandSender sender, OfflinePlayer target, String forcedIp)
    {
        UUID uuid = target.getUniqueId();
        String name = target.getName() != null ? target.getName() : uuid.toString();
        msg(sender, FUtil.miniMessage("<red>Player Info: <gold>" + name + "</gold> <gray>(" + uuid + ")</gray>"));

        PlaytimeData pt = playtimeManager.get(uuid);

        long first = pt.firstJoin;
        long last = target.getLastPlayed();

        // Your stored playtime in seconds
        long storedPlaytime = pt.totalPlaySeconds;

        // Bukkit stats playtime fallback (also in seconds)
        long bukkitPlaytime = 0;
        try {
            int ticks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
            bukkitPlaytime = ticks / 20L;
        } catch (Exception ignored) {}

        // Decide which playtime to show
        long played = storedPlaytime;
        boolean usingFallback = false;

        if (bukkitPlaytime > played) {
            played = bukkitPlaytime;
            // Don't override storedPlaytime here — only use for display
        }

        // Only fallback if no valid playtime recorded
        if (played == 0 && first > 0 && last > first) {
            played = (last - first) / 1000L;
            usingFallback = true;
        }

        // Update playtime data if we have a bigger number than stored (optional)
        if (played > storedPlaytime) {
            pt.totalPlaySeconds = played;
            playtimeManager.set(uuid, pt);
        }

        // Format times for display
        if (first == 0) first = target.getFirstPlayed();
        if (last == 0) last = target.getLastPlayed();
        pt.firstJoin = first;
        pt.lastSeen = last;
        playtimeManager.set(uuid, pt);

        String firstStr = first > 0 ? fmt.format(Instant.ofEpochMilli(first)) : "Unknown";
        String lastStr = last > 0 ? fmt.format(Instant.ofEpochMilli(last)) : "Unknown";
        String playStr = played > 0 ? formatDuration(played) : "Unknown";

        // Append fallback label if needed
        if (usingFallback) {
            playStr += " (fallback)";
        }

        String ip = forcedIp != null ? forcedIp : plugin.getAltManager().getLastIP(uuid).orElse("Unknown");
        List<String> alts = FUtil.getOfflinePlayersByIp(ip).stream()
                .map(OfflinePlayer::getName)
                .filter(n -> n != null && !n.equalsIgnoreCase(name))
                .collect(Collectors.toList());

        List<PlayerNote> notes = noteManager.getNotes(uuid);

        boolean muted = muteManager != null && muteManager.isMuted(target);
        boolean frozen = target.isOnline() && freezeManager != null && freezeManager.isFrozen((Player) target);
        boolean banned = banManager.isBanned(target);
        boolean opped = target.isOp();
        boolean white = target.isWhitelisted();

        msg(sender, FUtil.miniMessage("<red>First Join:</red> <yellow>" + firstStr));
        msg(sender, FUtil.miniMessage("<red>Last Seen:</red>  <yellow>" + lastStr));
        msg(sender, FUtil.miniMessage("<red>Playtime:</red>   <yellow>" + playStr));

        if (sender.hasPermission("kfc.seen.viewip"))
        {
            Component ipComp = Component.text(ip).clickEvent(ClickEvent.copyToClipboard(ip))
                    .hoverEvent(Component.text("Click to copy IP"));
            sender.sendMessage(FUtil.miniMessage("<red>IP:</red> ").append(ipComp));
        }
        else
        {
            msg(sender, FUtil.miniMessage("<red>IP:</red> <gray>[Hidden]</gray>"));
        }

        if (!alts.isEmpty())
        {
            msg(sender, FUtil.miniMessage("<red>Alts:</red> <yellow>" + String.join(", ", alts)));
        }

        if (!notes.isEmpty())
        {
            msg(sender, FUtil.miniMessage("<red>Notes:</red>"));
            for (PlayerNote n : notes)
            {
                msg(sender, FUtil.miniMessage("  <gray>- [" + n.getTimestamp() + "] " + n.getAuthor() + ": " + n.getMessage() + "</gray>"));
            }
        }
        else
        {
            msg(sender, FUtil.miniMessage("<red>Notes:</red> <gray>None"));
        }

        String status = (muted ? "Muted" : "Not Muted") + ", " + (frozen ? "Frozen" : "Not Frozen");
        msg(sender, FUtil.miniMessage("<red>Status:</red> <yellow>" + status + "</yellow>"));
        msg(sender, FUtil.miniMessage("<red>OP:</red> <yellow>" + (opped ? "Yes" : "No") + "</yellow>"));
        msg(sender, FUtil.miniMessage("<red>Banned:</red> <yellow>" + (banned ? "Yes" : "No") + "</yellow>"));
        msg(sender, FUtil.miniMessage("<red>Whitelisted:</red> <yellow>" + (white ? "Yes" : "No") + "</yellow>"));
    }

    private String formatDuration(long totalSeconds)
    {
        if (totalSeconds <= 0) return "0s";
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long secs = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (secs > 0) sb.append(secs).append("s");
        return sb.toString().trim();
    }
}
