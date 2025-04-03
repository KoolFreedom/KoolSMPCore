package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectateCommand extends FreedomCommand
{
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        if (args.length == 0)
        {
            return false;
        }

        Player player = getPlayer(args[0]);
        if (player == null)
        {
            msg(Messages.PLAYER_NOT_FOUND);
            return true;
        }

        if (player.getGameMode().equals(GameMode.SPECTATOR))
        {
            msg("You cannot spectate other players that are in spectator mode.", ChatColor.RED);
            return true;
        }

        if (!playerSender.getGameMode().equals(GameMode.SPECTATOR))
        {
            playerSender.setGameMode(GameMode.SPECTATOR);
        }

        if (playerSender.getWorld() != player.getWorld())
        {
            PaperLib.teleportAsync(playerSender, player.getLocation());
        }

        playerSender.setSpectatorTarget(player);
        return true;
    }
}