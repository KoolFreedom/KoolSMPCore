package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.util.FLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandParameters(name = "koolsmpcore", description = "Display information about the plugin or reload it.",
        usage = "/<command> [reload]")
public class KoolSMPCoreCommand extends KoolCommand
{
    private static final String DIVIDER = "<dark_gray><strikethrough>                                        ";

    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx ->
                {
                    String authors = String.join(", ", plugin.getPluginMeta().getAuthors());

                    msg(sender(ctx), DIVIDER);
                    msg(sender(ctx), "<gold><b>KoolSMPCore</b> <gray>- The Core Plugin of KoolFreedom SMP");
                    msg(sender(ctx), "<dark_gray> » <gray>Version <white><version>",
                            Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()));
                    msg(sender(ctx), "<dark_gray> » <gray>Authors <white><authors>",
                            Placeholder.unparsed("authors", authors));
                    msg(sender(ctx), DIVIDER);
                }))
                .then(literal("reload").executes(executes(ctx ->
                {
                    if (!sender(ctx).hasPermission("venomcore.reload"))
                    {
                        msg(sender(ctx), noPermission);
                        return;
                    }

                    try
                    {
                        MainConfig.load();
                        plugin.getGroupManager().loadGroups();
                        plugin.getChatListener().loadFilters();
                        plugin.resetAnnouncer();
                        msg(sender(ctx), "<gray>Reloaded config");
                    }
                    catch (Exception e)
                    {
                        FLog.error("Could not reload configuration", e);
                    }
                })));
    }
}
