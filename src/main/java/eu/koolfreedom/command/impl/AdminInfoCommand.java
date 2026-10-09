package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.ConfigEntry;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

@CommandParameters(name = "admininfo", description = "Displays info about applying for staff", aliases = {"ai", "staffinfo",})
public class AdminInfoCommand extends KoolCommand
{
    private static final List<Component> ADMIN_INFO = ConfigEntry.ADMININFO.getStringList()
            .stream().map(info -> MiniMessage.miniMessage().deserialize(info)).toList();

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx ->
        {
            if (ADMIN_INFO.isEmpty())
            {
                msg(sender(ctx), "<red>There is currently nothing configured for config section 'admininfo', contact the server's administrator to resolve this error.");
                return;
            }

            ADMIN_INFO.forEach(component -> msg(sender(ctx), component));
        }));
    }
}
