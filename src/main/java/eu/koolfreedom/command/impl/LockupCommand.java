package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.listener.impl.LockupManager;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

@CommandParameters(
        name        = "lockup",
        description = "Toggle lock‑up for a player (opens their inventory and immobilises them).",
        usage       = "/<command> <player>",
        aliases     = {"lock", "lockplayer"}
)
public class LockupCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx ->
                {
                    Player target = player(ctx, "target");
                    LockupManager lm = plugin.getLockupManager();
                    boolean nowLocked = lm.toggle(target);

                    FUtil.staffAction(sender(ctx),
                            (nowLocked ? "Locked-up " : "Unlocked ") + "<player>",
                            Placeholder.unparsed("player", target.getName()));

                    msg(sender(ctx), "<gray>" + target.getName() + " is now "
                            + (nowLocked ? "<red>locked-up" : "<green>free") + ".");

                    if (nowLocked) target.openInventory(target.getInventory());
                })));
    }
}
