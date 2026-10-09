package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "rawsay", description = "Broadcast a MiniMessage-formatted message to the server.",
        usage = "/<command> <message>", aliases = {"bcraw"})
public class RawSayCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("message", StringArgumentType.greedyString())
                .executes(executes(ctx -> broadcast(StringArgumentType.getString(ctx, "message")))));
    }
}
