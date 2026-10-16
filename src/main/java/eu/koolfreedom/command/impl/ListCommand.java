package eu.koolfreedom.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.command.KoolCommand;
import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CommandParameters(name = "list", description = "List online players by rank", permission = "kfc.list", aliases = {"who"})
public class ListCommand extends KoolCommand
{
	@Override
	public void build(LiteralArgumentBuilder<CommandSourceStack> root)
	{
		root.executes(executes(ctx -> sendPlayerList(sender(ctx), false)))
				.then(literal("vanished")
						.requires(source -> source.getSender().hasPermission("kfc.admin"))
						.executes(executes(ctx -> sendPlayerList(sender(ctx), true))));
	}

	private void sendPlayerList(CommandSender sender, boolean includeVanish)
	{
		List<String> playerNames = FUtil.getPlayerList(includeVanish);
		Map<GroupManagement.Group, List<Component>> playersByGroup = new LinkedHashMap<>();
		GroupManagement groups = KoolSMPCore.getInstance().getGroupManager();

		for (String playerName : playerNames)
		{
			Player player = Bukkit.getPlayerExact(playerName);
			if (player == null)
			{
				continue;
			}

			GroupManagement.Group group;
			User user = plugin.getLuckPermsBridge() == null ? null : plugin.getLuckPermsBridge().getApi().getUserManager().getUser(player.getUniqueId());
			if (user != null)
			{
				group = groups.getGroupByName(user.getPrimaryGroup());
			}
			else
			{
				group = groups.getSenderGroup(player);
			}

			playersByGroup.computeIfAbsent(group, ignored -> new ArrayList<>())
					.add(Component.text(player.getName()).color(group.getColor()));
		}

		msg(sender, Component.text("Online players (" + playerNames.size() + "):", NamedTextColor.GOLD));
		for (Map.Entry<GroupManagement.Group, List<Component>> entry : playersByGroup.entrySet())
		{
			Component line = entry.getKey().getDisplayName()
					.append(Component.text(": ", NamedTextColor.GRAY))
					.append(Component.join(JoinConfiguration.commas(true), entry.getValue()));
			msg(sender, line);
		}
	}
}