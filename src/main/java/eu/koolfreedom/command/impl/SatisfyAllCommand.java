package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

@CommandParameters(name = "satisfyall", description = "Feed and heal everyone online.", aliases = {"feedall"})
public class SatisfyAllCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue());
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        });
        FUtil.staffAction(sender, "Healed all players");
        return true;
    }
}