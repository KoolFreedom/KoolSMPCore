package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.util.FUtil;
import eu.koolfreedom.util.KoolSMPCoreBase;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginListener extends KoolSMPCoreBase implements Listener
{
    public LoginListener(KoolSMPCore plugin)
    {
        this.main = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
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
    }
}
