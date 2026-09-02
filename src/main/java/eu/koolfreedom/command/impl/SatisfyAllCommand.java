package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;

import java.util.Objects;

@CommandParameters(name = "satisfyall", description = "Feed and heal everyone online.", aliases = {"feedall"})
public class SatisfyAllCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx -> satisfyAll(sender(ctx))));
    }

    private void satisfyAll(CommandSender sender)
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            player.setFireTicks(0);
        });
        FUtil.staffAction(sender, "Healed all players");
    }
}
