package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class OrbitCommand extends FreedomCommand implements TabCompleter {
    private static final Map<UUID, Integer> isOrbited = new HashMap<>();

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GRAY + "Usage: /" + commandLabel + " <player> [<power> | stop]");
            return true;
        }

        if (!sender.hasPermission("kf.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        int strength = 100;

        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("stop")) {
                stopOrbiting(target);
                sender.sendMessage(ChatColor.GREEN + "Stopped orbiting " + target.getName() + ".");
                return true;
            }

            try {
                strength = Math.max(1, Math.min(150, Integer.parseInt(args[1])));
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid number for strength. Must be between 1 and 150.");
                return true;
            }
        }

        if (isPlayerOrbited(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + target.getName() + " is already orbiting.");
            return true;
        }

        startOrbiting(target, strength);
        FUtil.adminAction(sender.getName(), "Orbiting " + target.getName(), false);
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return playerNames;
        } else if (args.length == 2) {
            return Arrays.asList("stop", "50", "100", "150");
        }
        return Collections.emptyList();
    }

    private void startOrbiting(Player player, int strength) {
        player.setGameMode(GameMode.SURVIVAL);
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, Integer.MAX_VALUE, strength, false, false));
        isOrbited.put(player.getUniqueId(), strength);
    }

    private void stopOrbiting(Player player) {
        player.removePotionEffect(PotionEffectType.LEVITATION);
        isOrbited.remove(player.getUniqueId());
    }

    public static boolean isPlayerOrbited(UUID playerId) {
        return isOrbited.containsKey(playerId);
    }

    public static Integer getOrbitStrength(UUID playerId) {
        return isOrbited.get(playerId);
    }
}
