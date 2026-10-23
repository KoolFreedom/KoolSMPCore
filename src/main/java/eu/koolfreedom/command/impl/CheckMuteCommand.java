package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandParameters(name = "checkmute", description = "Check a player's active mute", usage = "/checkmute <player>")
public class CheckMuteCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> check(sender(ctx), StringArgumentType.getString(ctx, "target")))));
    }

    private void check(CommandSender sender, String target)
    {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(target);
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline()))
        {
            msg(sender, playerNotFound);
            return;
        }

        if (plugin.getMuteManager().isMuted(player))
        {
            msg(sender, "<yellow><target> is actively muted.",
                    Placeholder.unparsed("target", player.getName() == null ? target : player.getName()));
        }
        else
        {
            msg(sender, "<gray><target> is not actively muted.",
                    Placeholder.unparsed("target", player.getName() == null ? target : player.getName()));
        }
    }
}
