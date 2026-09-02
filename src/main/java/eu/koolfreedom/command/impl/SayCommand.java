package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "say", description = "Broadcast an official-looking message to the server.",
        usage = "/<command> <message>", aliases = "broadcast")
public class SayCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("message", StringArgumentType.greedyString())
                .executes(executes(ctx ->
                {
                    FUtil.broadcast(ConfigEntry.FORMATS_SAY.getString(), Placeholder.unparsed("name", sender(ctx).getName()),
                            Placeholder.unparsed("message", StringUtils.join(StringArgumentType.getString(ctx, "message"), " ")));
                })));
    }
}