package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "freeze", description = "Freeze players", usage = "/freeze <player>")
public class FreezeCommand extends KoolCommand
{
    private final FreezeManager freezeManager = plugin.getFreezeManager();

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .executes(executes(ctx -> fool(sender(ctx), player(ctx, "target")))));
    }

    private void fool(CommandSender sender, Player target)
    {
        if (freezeManager.isFrozen(target))
        {
            freezeManager.unfreeze(target);
            FUtil.staffAction(sender, "Unfroze <target>", Placeholder.unparsed("target", target.getName()));
            msg(sender, "<gray>Unfroze <target>", Placeholder.unparsed("target", target.getName()));
            msg(target, "<gray>You have been unfrozen");
            return;
        }

        freezeManager.freeze(target);
        FUtil.staffAction(sender, "Froze <target>", Placeholder.unparsed("target", target.getName()));
        msg(sender, "<gray>You have frozen <target>", Placeholder.unparsed("target", target.getName()));
        msg(target, "<gray>You have been frozen!");
    }
}
