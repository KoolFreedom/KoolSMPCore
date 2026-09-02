package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandParameters(name = "unban", description = "Unban a player or IP address.", usage = "/<command> <playerOrIp>",
        aliases = {"pardon", "pardon-ip", "unbanip"})
public class UnbanCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", StringArgumentType.word())
                .executes(executes(ctx -> unban(sender(ctx), StringArgumentType.getString(ctx, "target")))));
    }

    private void unban(CommandSender sender, String targetArg)
    {
        Ban ban = plugin.getBanManager().removeBan(targetArg);

        if (ban == null)
        {
            msg(sender, "<red>An entry could not be found which fit the criteria.");
        }
        else if (!ban.canExpire())
        {
            msg(sender, "<red>Permanent bans cannot be removed from in-game for security reasons.");
        }
        else
        {
            String name = ban.getName() != null ? ban.getName() : ban.getUuid() != null ? Bukkit.getOfflinePlayer(ban.getUuid()).getName() : null;

            if (name != null)
            {
                FUtil.staffAction(sender, "Unbanned <player>", Placeholder.unparsed("player", name));
            }
            else
            {
                FUtil.staffAction(sender, "Unbanned an IP address");
            }
        }
    }
}
