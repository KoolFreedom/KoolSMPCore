package eu.koolfreedom.command.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.*;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

@CommandParameters(name = "doom", description = "Ban someone from the server, but with extra violence.",
        usage = "/<command> <player> [reason]", aliases = {"obliterate"})
public class DoomCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null)
        {
            msg(sender, playerNotFound);
            return true;
        }

        String reason = args.length >= 2 ? String.join(" ", ArrayUtils.remove(args, 0)) : null;

        eviscerate(target, sender, reason);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }

    public static void eviscerate(Player target, CommandSender sender, String reason)
    {
        // Get the technical side of things out of the way first, we want this user banned first and foremost
        final Ban ban = Ban.fromPlayer(target, sender.getName(), reason != null ? reason : "You've met with a terrible fate, haven't you?", Long.MAX_VALUE);
        final KoolSMPCore plugin = KoolSMPCore.getInstance();
        plugin.getBanManager().addBan(ban);
        plugin.getRecordKeeper().recordPunishment(Punishment.fromBan(ban));

        // Remove their groups if present
        if (plugin.getGroupManager().getVaultPermissions() != null)
        {
            Permission vault = plugin.getGroupManager().getVaultPermissions();
            final List<String> groups = Arrays.stream(vault.getPlayerGroups(target)).toList();
            groups.forEach(group -> {
                try
                {
                    // Remove them globally if the plugin supports it
                    if (!vault.playerRemoveGroup(null, target, group))
                        // Remove them in that specific world if the plugin supports it but not global permissions
                        vault.playerRemoveGroup(target, group);
                }
                // Just in case...
                catch (UnsupportedOperationException ex)
                {
                    // See them try to get around this HAHAHAHAHAHAHAHA
                    PermissionAttachment attachment = target.addAttachment(plugin);
                    attachment.getPermissions().forEach((key, value) ->
                            attachment.setPermission(key, false));
                    target.recalculatePermissions();
                }
            });

            // Update their tab name forcefully
            target.playerListName(plugin.getGroupManager().getColoredName(target));
        }
        // Do it with LuckPerms instead, since their API might do a better job than Vault
        else if (plugin.getLuckPermsBridge() != null)
        {
            LuckPerms luckPerms = plugin.getLuckPermsBridge().getApi();

            luckPerms.getUserManager().deletePlayerData(target.getUniqueId());
            luckPerms.getUserManager().modifyUser(target.getUniqueId(), user ->
            {
                user.setPrimaryGroup("default");
                user.data().clear();
            });
        }

        // Stop them dead in their tracks
        target.spawnParticle(Particle.ASH, target.getLocation(), Integer.MAX_VALUE, 1, 1, 1, 1, null, true);

        // Now we can have our fun
        FUtil.broadcast("<red><sender> is swinging the Russian Hammer over <player>",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("player", target.getName()));

        // ZAP
        target.setGameMode(GameMode.ADVENTURE);
        target.getWorld().strikeLightning(target.getLocation());
        target.getWorld().createExplosion(target.getLocation(), 0F, false);

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                FUtil.broadcast("<red><player> will be completely eviscerated.",
                        Placeholder.unparsed("player", target.getName()));

                if (!target.isOnline())
                {
                    FUtil.broadcast("<red>Not even running away will save you.");
                }

                target.getWorld().strikeLightning(target.getLocation());
                target.getWorld().createExplosion(target.getLocation(), 0F, false);
            }
        }.runTaskLater(plugin, 20L);

        for (int i = 1; i < 4; i++)
        {
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    target.getWorld().strikeLightning(target.getLocation());
                    target.getWorld().createExplosion(target.getLocation(), 0F, false);
                }
            }.runTaskLater(plugin, 30 * i);
        }

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if (target.isOnline())
                {
                    target.kick(ban.getKickMessage());
                }

                target.getWorld().strikeLightning(target.getLocation());
                target.getWorld().createExplosion(target.getLocation(), 0F, false);

                FUtil.staffAction(sender, "Banned <player> with excessive violence",
                        Placeholder.unparsed("player", target.getName()));
            }
        }.runTaskLater(plugin, 135L);
    }
}