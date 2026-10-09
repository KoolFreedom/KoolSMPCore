package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
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
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        // Target stays a free-typed word rather than ArgumentTypes.player() - orbiting has to keep working (and
        // "stop" has to keep working) on players who are already in orbitMap but have since gone offline.
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> startOrbit(sender(ctx), StringArgumentType.getString(ctx, "target"), 100)))
                .then(literal("stop").executes(executes(ctx -> stopOrbit(sender(ctx), StringArgumentType.getString(ctx, "target")))))
                .then(argument("strength", IntegerArgumentType.integer(1))
                        .executes(executes(ctx -> startOrbit(sender(ctx), StringArgumentType.getString(ctx, "target"),
                                IntegerArgumentType.getInteger(ctx, "strength"))))));
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String name)
    {
        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        if (!orbitMap.containsKey(target.getUniqueId()) && !target.isOnline())
        {
            msg(sender, playerNotFound);
            return null;
        }
        return target;
    }

    private void startOrbit(CommandSender sender, String targetName, int strength)
    {
        OfflinePlayer target = resolveTarget(sender, targetName);
        if (target == null) return;

        orbitMap.put(target.getUniqueId(), strength);

        if (target instanceof Player player)
        {
            grantPotionEffects(player);
        }

        FUtil.staffAction(sender, "Started orbiting <player>",
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
    }

    private void stopOrbit(CommandSender sender, String targetName)
    {
        OfflinePlayer target = resolveTarget(sender, targetName);
        if (target == null) return;

        orbitMap.remove(target.getUniqueId());
        if (target instanceof Player player)
        {
            player.setGameMode(GameMode.SURVIVAL);
            player.removePotionEffect(PotionEffectType.LEVITATION);
        }
        FUtil.staffAction(sender, "Stopped orbiting <player>",
                Placeholder.unparsed("player", Objects.requireNonNull(target.getName())));
    }

    @EventHandler
    public void onEffectRemoval(EntityPotionEffectEvent event)
    {
        // Cast to the stable supertype before calling getEntity() - EntityEvent's getEntity() has
        // always returned plain Entity and always will. Calling it through a reference typed as
        // EntityPotionEffectEvent binds to whatever return type THAT specific class declares, which is
        // exactly what's version-sensitive here (some Paper builds add a covariant override narrowing
        // it to LivingEntity, some don't). This works on both, because when a class DOES override with
        // a covariant return type, the compiler is required to also generate a synthetic bridge method
        // matching the original Entity-returning signature for binary compatibility - so the older
        // signature is always callable either way.
        if (((EntityEvent) event).getEntity() instanceof Player player
                && orbitMap.containsKey(player.getUniqueId())
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
