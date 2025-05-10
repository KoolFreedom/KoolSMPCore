package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.log.FLog;
import lombok.Getter;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

@Getter
public class LuckPermsListener
{
	private final LuckPerms api;

	// EVenTsUBScRipTIon<NOdemutateeVENt> uSED wIthOuT 'TRY'-WiTh-REsouRCES sTateMent
	@SuppressWarnings("resource")
	public LuckPermsListener()
	{
		RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
		if (provider != null)
		{
			FLog.info("Successfully loaded the LuckPerms API.");
			api = provider.getProvider();
		}
		else
		{
			throw new IllegalArgumentException("Unable to get the LuckPerms API despite the plugin existing");
		}

		// Register an event listener for NodeMutateEvent to track when players are added or removed to groups
		//
		// I actually spent a lot of time evaluating which event to use based on how many times the event gets called,
		//  how specific the event can be, and the surface of potential cases where it gets called
		//
		// UserDataRecalculateEvent is an incredibly generic event that gets called in a lot of cases without a good way
		//  to determine *what* caused the event to happen. Leaving the server calls the event. Updating permission
		//  nodes for a particular group that the user isn't even in calls the event.
		//
		// UserTrackEvent is an event that, as the name implies, only gets called when the player is promoted/demoted
		//  from a group in a track. It does not get called if their primary group is set directly, which is something
		//  we do in the /doom command to reset their rank completely, so there would be cases where it just won't
		//  update that event was hooked into.
		//
		// NodeMutateEvent was the best fit since it only gets called when a node change happens, and we can figure out
		//  whether an update is necessary or not with the parameters it gives us.
		api.getEventBus().subscribe(KoolSMPCore.getInstance(), NodeMutateEvent.class, event ->
		{
			if (event.getTarget() instanceof User user)
			{
				// Player must be online and the node changing must include an inheritance node
				boolean shouldUpdate = Bukkit.getPlayer(user.getUniqueId()) != null && switch (event)
				{
					case NodeClearEvent clear ->
							clear.getNodes().stream().anyMatch(node -> node instanceof InheritanceNode);
					case NodeRemoveEvent remove -> remove.getNode() instanceof InheritanceNode;
					case NodeAddEvent add -> add.getNode() instanceof InheritanceNode;
					default -> false;
				};

				if (shouldUpdate)
				{
					Player player = Bukkit.getPlayer(user.getUniqueId());

					if (player != null)
					{
						player.playerListName(KoolSMPCore.getInstance().groupCosmetics.getColoredName(player));
					}
				}
			}
		});
	}
}
