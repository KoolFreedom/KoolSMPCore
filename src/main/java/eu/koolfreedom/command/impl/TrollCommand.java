package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@CommandParameters(name = "troll", description = "Troll someone.", usage = "/<command> <player>")
public class TrollCommand extends KoolCommand
{
    // if this doesn't scream "i'm running out of ideas, please help", idk what does...
    // i'm going as far as making a fucking BLATANTLY OBVIOUS troll command... what am i doing...

    // this is just a piss-poor half-assed "troll" command that is surely going to get annoying quick
    // literally all it does is play loud noises and give stupid effects
    @Override
    public boolean run(CommandSender sender, Player player, Command cmd, String s, String[] args)
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

        if (target.equals(player))
        {
            msg(sender, "<red>You can't troll yourself.");
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 255, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 255, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 120, 255, false, false, true));
        target.playSound(target.getLocation(), "minecraft:entity.ender_dragon.growl", 100, 2);
        target.playSound(target.getLocation(), "minecraft:entity.wither.ambient", 100, 2);
        target.playSound(target.getLocation(), "minecraft:entity.ghast.scream", 100, 2);

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 ? Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(sender.getName())).toList() : List.of();
    }
}
