package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.Command;
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
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String s, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        String sub = args[0].toLowerCase();

        switch(sub)
        {
            case "cry" ->
            {
                broadcast("<aqua><name> has started to cry :(", Placeholder.unparsed("name", sender.getName()));
                return true;
            }
            case "shit" ->
            {
                broadcast("<#823d08><name> Has done a big wet sloppy shit", Placeholder.unparsed("name", sender.getName()));
                return true;
            }  
            case "hug" ->
            {
                if (args.length < 2)
                {
                    msg(sender, "<red>Please specify a player.");
                    return true;
                }

                Player player = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                if (player == null)
                {
                    msg(sender, playerNotFound);
                    return true;
                }

                if (player.equals(playerSender))
                {
                    msg(sender, "<red>You can't hug yourself.");
                    return true;
                }

                broadcast("<#E6B1C9><sender> gave <target> a warm hug!",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("target", player.getName()));
                return true;
            }
            case "kiss" ->
            {
                if (args.length < 2)
                {
                    msg(sender, "<red>Please specify a player.");
                    return true;
                }

                Player player = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                if (player == null)
                {
                    msg(sender, playerNotFound);
                    return true;
                }

                if (player.equals(playerSender))
                {
                    msg(sender, "<red>You must be pretty desperate for love if you're trying to kiss yourself.");
                    return true;
                }

                broadcast("<#FFABDB><sender> gave <target> a kiss on the cheek. Awww! <dark_red><b>♥",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("target", player.getName()));
                return true;
            }
            case "pat" ->
            {
                if (args.length < 2)
                {
                    msg(sender, "<red>Please specify a player.");
                    return true;
                }

                Player player = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                if (player == null)
                {
                    msg(sender, playerNotFound);
                    return true;
                }

                if (player.equals(playerSender))
                {
                    msg(sender, "<red>You can't pat yourself.");
                    return true;
                }

                broadcast("<#F06FF6><sender> gave <target> a pat on the head.",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("target", player.getName()));

                return true;
            }
            case "poke" ->
            {
                if (args.length < 2)
                {
                    msg(sender, "<red>Please specify a player.");
                    return true;
                }

                Player player = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                if (player == null)
                {
                    msg(sender, playerNotFound);
                    return true;
                }

                if (player.equals(playerSender))
                {
                    msg(sender, "<red>You accidentally poked yourself in the eye.");
                    playerSender.damage(playerSender.getHealth() / 10);
                    playerSender.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 255, false, false, true));
                    return true;
                }

                broadcast("<#FF7EBB><sender> poked <target>.",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("target", player.getName()));

                return true;
            }
            case "ship" ->
            {
                if (args.length < 3)
                {
                    msg(sender, "<red>Usage: /reaction ship <player1> <player2>");
                    return true;
                }

                final Player player1 = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                final Player player2 = FUtil.getPlayer(args[2], sender.hasPermission("kfc.command.see_vanished_players"));

                if (player1 == null || player2 == null)
                {
                    msg(sender, "<red>One of those players could not be found.");
                    return true;
                }

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

                    return true;
                }

                broadcast("<#FF8C8C><sender> ships <player1> x <player2>! <dark_red><b>♥",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("player1", player1.getName()),
                        Placeholder.unparsed("player2", player2.getName()));

                return true;
            }
            case "slap" ->
            {
                if (args.length < 2)
                {
                    msg(sender, "<red>Please specify a player.");
                    return true;
                }

                Player target = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));
                if (target == null)
                {
                    msg(sender, playerNotFound);
                    return true;
                }

                if (target.equals(playerSender))
                {
                    playerSender.damage(Math.max(playerSender.getHealth() / 4, 1.0));
                    msg(sender, "<red>Ouch! That looks like it must have hurt.");
                    return true;
                }

                broadcast("<#ff0004><sender> gave <player> a nice slap to the face!",
                        Placeholder.unparsed("sender", sender.getName()),
                        Placeholder.unparsed("player", target.getName()));
                return true;
            }
            default ->
            {
                msg(sender, "<red>Please provide a valid reaction: cry, hug, kiss, pat, poke, ship, slap");
                return true;
            }
        }
    }
}
