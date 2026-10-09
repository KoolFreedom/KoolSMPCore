package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.util.BuildProperties;
import eu.koolfreedom.util.FLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandParameters(name = "koolsmpcore", description = "Display information about the plugin or reload it.",
        usage = "/<command> [reload]")
public class KoolSMPCoreCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.executes(executes(ctx ->
                {
                    final String authors = String.join(", ", plugin.getPluginMeta().getAuthors());
                    BuildProperties build = plugin.getBuildMeta();
                    msg(sender(ctx), "<gradient:#38d6c8:#8eb8ff><bold>KoolSMPCore</bold></gradient> <dark_gray>•</dark_gray> <gray>Core plugin of KoolFreedom SMP");
                    msg(sender(ctx), "<gray>Version <white>" + build.getVersion() + "." + build.getNumber());
                    msg(sender(ctx), "<gray>Built <white>" + build.getDate() + " <dark_gray>by</dark_gray> <white>" + build.getAuthor());
                    msg(sender(ctx), "<gray>Authors <white><authors>",
                            Placeholder.unparsed("authors", authors));
                }))
                .then(literal("reload").executes(executes(ctx ->
                {
                    if (!sender(ctx).hasPermission("kfc.senior"))
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
