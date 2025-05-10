package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@CommandParameters(name = "orbit", description = ":trol:", usage = "/<command> <player> <<power> | stop>")
public class OrbitCommand extends KoolCommand implements Listener
{
    private final Map<UUID, Integer> orbitMap = new HashMap<>();

    public OrbitCommand()
    {
        super();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!orbitMap.containsKey(target.getUniqueId()) && !target.isOnline())
        {
            msg(sender, playerNotFound);
            return true;
        }

        int strength = 100;

        if (args.length >= 2)
        {
            if (args[1].equalsIgnoreCase("stop"))
            {
                orbitMap.remove(target.getUniqueId());
                if (target instanceof Player player)
                {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.removePotionEffect(PotionEffectType.LEVITATION);

                    FUtil.staffAction(sender, "Stopped orbiting <player>",
                            Placeholder.unparsed("player", target.getName()));
                    return true;
                }
            }
            else
            {
                try
                {
                    strength = Math.min(Integer.parseInt(args[1]), 1);
                }
                catch (NumberFormatException ex)
                {
                    msg(sender, "<red>Invalid number: <value>",
                            Placeholder.unparsed("value", String.valueOf(strength)));
                    return true;
                }
            }
        }

        orbitMap.put(target.getUniqueId(), strength);

        if (target instanceof Player player)
        {
            grantPotionEffects(player);
        }

        FUtil.staffAction(sender, "Started orbiting <player>",
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        if (args.length == 1)
        {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        else if (args.length == 2)
        {
            List<String> params = new ArrayList<>(List.of("50", "100", "150"));

            if (Bukkit.getOfflinePlayer(args[0]) instanceof Player player && orbitMap.containsKey(player.getUniqueId()))
            {
                params.add("stop");
            }

            return params;
        }

        return List.of();
    }

    @EventHandler
    public void onEffectRemoval(EntityPotionEffectEvent event)
    {
        if (event.getEntity() instanceof Player player && orbitMap.containsKey(player.getUniqueId())
                && (event.getAction() == EntityPotionEffectEvent.Action.REMOVED || event.getAction() == EntityPotionEffectEvent.Action.CLEARED))
        {
            grantPotionEffects(player);
        }
    }

    private void grantPotionEffects(Player player)
    {
        if (player.getGameMode() != GameMode.ADVENTURE)
        {
            player.setGameMode(GameMode.ADVENTURE);
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, Integer.MAX_VALUE,
                orbitMap.getOrDefault(player.getUniqueId(), 0)));
    }
}
