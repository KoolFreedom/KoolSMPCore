package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import java.util.ArrayList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReportCommand implements CommandExecutor {
    private final ArrayList<Player> antispam = new ArrayList<>();

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        Player player;
        if (commandSender instanceof Player) {
            player = (Player)commandSender;
        } else {
            commandSender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /" + s + " <player> <reason>", NamedTextColor.RED));
            return true;
        }
        Player playerArg = Bukkit.getServer().getPlayer(args[0]);
        if (playerArg == null) {
            player.sendMessage(Component.text("Could not find the specified player on the server.", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("You did not provide a valid reason.", NamedTextColor.RED));
            return true;
        }
        String reason = StringUtils.join(args, " ", 1, args.length).replace("\\n", "").replace("`", "");
        if (reason.isBlank()) {
            player.sendMessage(Component.text("You did not provide a valid reason.", NamedTextColor.RED));
            return true;
        }
        if (!this.antispam.contains(player)) {
            this.antispam.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(KoolSMPCore.main, () -> this.antispam.remove(player), 60L);
        } else {
            player.sendMessage(Component.text("You're only allowed to execute that command every 3 seconds.", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(Component.newline().append(Component.text("Successfully submitted the report to staff.", NamedTextColor.GREEN)).appendNewline());
        if (player.getName().equals(playerArg.getName()))
            player.sendMessage(Component.text("Also, why would you report yourself?", NamedTextColor.RED));
        String channelId = KoolSMPCore.main.getConfig().getString("discord.report-channel-id", "1320586156135551026");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord bcast #" + channelId + " ```Received report from " + player
                .getName() + " reporting " + playerArg.getName() + " for " + reason + "```");
        for (Player players : Bukkit.getOnlinePlayers()) {
            if (!players.hasPermission("kf.admin"))
                continue;
            players.sendMessage(Component.newline()
                    .append(Component.text("[", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Staff", NamedTextColor.RED))
                    .append(Component.text("]", NamedTextColor.DARK_GRAY))
                    .append(Component.text(" Received report from ", NamedTextColor.GREEN))
                    .append(Component.text(player.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" reporting ", NamedTextColor.GREEN))
                    .append(Component.text(playerArg.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" for ", NamedTextColor.GREEN))
                    .append(Component.text(reason, NamedTextColor.GOLD))
                    .appendNewline());
        }
        return true;
    }
}