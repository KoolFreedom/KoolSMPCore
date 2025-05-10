package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@CommandParameters(name = "ship", description = "Ship two players together, then proceed to get laughed at.",
        usage = "/<command> <player1> <player2>")
public class ShipCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args)
    {
        if (args.length != 2)
        {
            return false;
        }

        final Player player1 = FUtil.getPlayer(args[0], sender.hasPermission("kfc.command.see_vanished_players"));
        final Player player2 = FUtil.getPlayer(args[1], sender.hasPermission("kfc.command.see_vanished_players"));

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

        broadcast("<green><sender> ships <player1> x <player2>! <dark_red><b>♥",
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.unparsed("player1", player1.getName()),
                Placeholder.unparsed("player2", player2.getName()));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command command, String commandLabel, String[] args)
    {
        return args.length == 1 || args.length == 2 ? FUtil.getPlayerList(sender.hasPermission("kfc.command.see_vanished_players")) : List.of();
    }
}
