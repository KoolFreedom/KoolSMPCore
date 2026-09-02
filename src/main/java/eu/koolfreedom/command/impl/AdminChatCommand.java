package eu.koolfreedom.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.key.Namespaced;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.NamespacedKey;

@CommandParameters(name = "adminchat", description = "Speak with other staff members", aliases = {"o", "oc", "ac"})
public class AdminChatCommand extends KoolCommand
{
    private final Namespaced key = NamespacedKey.fromString("ingame_command", plugin);

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("message", StringArgumentType.greedyString())
                .executes(executes(ctx ->
                {
                    FUtil.adminChat(ctx.getSource().getSender(),
                            plugin.getGroupManager().getSenderGroup(ctx.getSource().getSender()),
                            Component.text(StringUtils.join(StringArgumentType.getString(ctx, "message"), " ")), key);
                })));
    }
}
