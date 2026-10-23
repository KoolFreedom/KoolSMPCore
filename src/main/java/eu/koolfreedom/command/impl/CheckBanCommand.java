package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

@CommandParameters(name = "checkban", description = "Check a player's active ban", usage = "/checkban <player|uuid|ip>")
public class CheckBanCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> check(sender(ctx), StringArgumentType.getString(ctx, "target")))));
    }

    private void check(CommandSender sender, String target)
    {
        Ban ban = plugin.getBanManager().findBan(target).orElse(null);
        if (ban == null)
        {
            msg(sender, "<gray><target> does not have an active ban.", Placeholder.unparsed("target", target));
            return;
        }

        msg(sender, "<red><target> is actively banned. <gray>Duration: <yellow><duration></yellow> <gray>Reason: <white><reason>",
                Placeholder.unparsed("target", ban.getName() == null ? target : ban.getName()),
                Placeholder.unparsed("duration", ban.getDurationString()),
                Placeholder.unparsed("reason", ban.getReason() == null || ban.getReason().isBlank()
                        ? "No reason provided" : ban.getReason()));
    }
}
