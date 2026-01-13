package eu.koolfreedom.listener.impl;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;

public class CosmeticManager extends KoolListener
{

    @EventHandler
    public void onServerPing(ServerListPingEvent event)
    {
        /* ------------------------------------------------------------ */
        /*  0) Grab the BanManager once                                */
        /* ------------------------------------------------------------ */
        BanManager banManager = KoolSMPCore.getInstance().getBanManager();

        /* ------------------------------------------------------------ */
        /*  1) IP‑ban check                                            */
        /* ------------------------------------------------------------ */
        String ip = event.getAddress().getHostAddress();          // raw IP string
        Optional<Ban> ipBan = banManager.findBan(ip);             // BanManager API

        if (ipBan.isPresent())
        {
            Ban ban = ipBan.get();                     // never expired (findBan filters)

            // Build a short MOTD for the ping screen
            StringBuilder mm = new StringBuilder("<red><b>You are banned from this server.");

            if (ban.getReason() != null)
                mm.append("<newline><red>Reason:</red> <yellow><reason>");

            event.motd(FUtil.miniMessage(
                    mm.toString(),
                    Placeholder.unparsed("reason",
                            ban.getReason() == null ? "No reason specified" : ban.getReason())));
            return; // don’t run any further MOTD logic
        }

        /* ------------------------------------------------------------ */
        /*  2) Whitelist / server‑full fall‑backs                      */
        /* ------------------------------------------------------------ */
        if (Bukkit.hasWhitelist())
        {
            event.motd(FUtil.miniMessage(ConfigEntry.SERVER_WHITELIST_MOTD.getString()));
            return;
        }

        if (Bukkit.getOnlinePlayers().size() >= Bukkit.getMaxPlayers())
        {
            event.motd(FUtil.miniMessage(ConfigEntry.SERVER_FULL_MOTD.getString()));
            return;
        }

        /* ------------------------------------------------------------ */
        /*  3) Normal MOTD                                             */
        /* ------------------------------------------------------------ */
        event.motd(FUtil.miniMessage(ConfigEntry.SERVER_MOTD.getString()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        String header = ConfigEntry.SERVER_TABLIST_HEADER.getString();
        String footer = ConfigEntry.SERVER_TABLIST_FOOTER.getString();

        if (header != null)
        {
            player.sendPlayerListHeader(FUtil.miniMessage(header));
        }

        if (footer != null)
        {
            player.sendPlayerListFooter(FUtil.miniMessage(footer));
        }

        player.playerListName(KoolSMPCore.getInstance().getGroupManager().getColoredName(player));

        KoolSMPCore.getInstance().getGroupManager().applyNametagColor(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e)
    {
        String name = e.getPlayer().getName();
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : main.getTeams())
        {
            team.removeEntry(name);
        }
    }

}
