package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@CommandParameters(name = "reaction", description = "Reactions", usage = "/<command> <reaction>")
public class ReactionCommand extends KoolCommand
{
    @Override
    public void build(LiteralArgumentBuilder<CommandSourceStack> root)
    {
        root.then(literal("cry").executes(executes(ctx -> cry(sender(ctx)))))
                .then(literal("hug").then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> hug(sender(ctx), playerSender(ctx), player(ctx, "target"))))))
                .then(literal("kiss").then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> kiss(sender(ctx), playerSender(ctx), player(ctx, "target"))))))
                .then(literal("pat").then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> pat(sender(ctx), playerSender(ctx), player(ctx, "target"))))))
                .then(literal("poke").then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> poke(sender(ctx), playerSender(ctx), player(ctx, "target"))))))
                .then(literal("slap").then(argument("target", ArgumentTypes.player())
                        .executes(executes(ctx -> slap(sender(ctx), playerSender(ctx), player(ctx, "target"))))))
                .then(literal("ship").then(argument("player1", ArgumentTypes.player())
                        .then(argument("player2", ArgumentTypes.player())
                                .executes(executes(ctx -> ship(sender(ctx), playerSender(ctx),
                                        player(ctx, "player1"), player(ctx, "player2")))))));
    }

    private void cry(CommandSender sender)
    {
        broadcast("<aqua><name> has started to cry :(", Placeholder.unparsed("name", sender.getName()));
    }

    private void hug(CommandSender sender, Player playerSender, Player target)
    {
        if (target.equals(playerSender))
        {
            msg(sender, "<red>You can't hug yourself.");
            return;
        }

        broadcast("<#E6B1C9><sender> gave <target> a warm hug!",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()));
    }

    private void kiss(CommandSender sender, Player playerSender, Player target)
    {
        if (target.equals(playerSender))
        {
            msg(sender, "<red>You must be pretty desperate for love if you're trying to kiss yourself.");
            return;
        }

        broadcast("<#FFABDB><sender> gave <target> a kiss on the cheek. Awww! <dark_red><b>♥",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()));
    }

    private void pat(CommandSender sender, Player playerSender, Player target)
    {
        if (target.equals(playerSender))
        {
            msg(sender, "<red>You can't pat yourself.");
            return;
        }

        broadcast("<#F06FF6><sender> gave <target> a pat on the head.",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()));
    }

    private void poke(CommandSender sender, Player playerSender, Player target)
    {
        if (target.equals(playerSender))
        {
            msg(sender, "<red>You accidentally poked yourself in the eye.");
            playerSender.damage(playerSender.getHealth() / 10);
            playerSender.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 255, false, false, true));
            return;
        }

        broadcast("<#FF7EBB><sender> poked <target>.",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("target", target.getName()));
    }

    private void slap(CommandSender sender, Player playerSender, Player target)
    {
        if (target.equals(playerSender))
        {
            playerSender.damage(Math.max(playerSender.getHealth() / 4, 1.0));
            msg(sender, "<red>Ouch! That looks like it must have hurt.");
            return;
        }

        broadcast("<#ff0004><sender> gave <player> a nice slap to the face!",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("player", target.getName()));
    }

    private void ship(CommandSender sender, Player playerSender, Player player1, Player player2)
    {
        if (player1.equals(player2))
        {
            msg(sender, "<red>You can't ship someone with themselves.");

            if (playerSender != null)
            {
                msg(sender, "<yellow>But here's a cookie for trying.");

                final ItemStack stack = new ItemStack(Material.COOKIE);
                final ItemMeta meta = stack.getItemMeta();
                meta.displayName(Component.text("Idiot of the Day Award").color(NamedTextColor.GOLD));
                meta.lore(List.of(FUtil.miniMessage("<gradient:gold:yellow:gold>Imagine trying to ship someone with themselves.")));
                stack.setItemMeta(meta);

                if (!playerSender.getInventory().addItem(stack).isEmpty())
                {
                    msg(sender, "<red>Oh wait, your inventory is already full. Nevermind!");
                }
            }

            return;
        }

        broadcast("<#FF8C8C><sender> ships <player1> x <player2>! <dark_red><b>♥",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("player1", player1.getName()),
                Placeholder.unparsed("player2", player2.getName()));
    }
}
