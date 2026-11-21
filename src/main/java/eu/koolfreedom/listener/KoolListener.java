package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.listener.impl.LockupManager;
import eu.koolfreedom.player.PlayerRegistry;
import eu.koolfreedom.util.FUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.Listener;

public abstract class KoolListener implements Listener
{
    public KoolListener()
    {
        KoolSMPCore.getInstance().getServer().getPluginManager()
                .registerEvents(this, KoolSMPCore.getInstance());
    }

    protected final KoolSMPCore plugin = KoolSMPCore.getInstance();
    protected final PlayerRegistry playerRegistry = plugin.getPlayerRegistry();
    protected final FreezeManager freezeManager = plugin.getFreezeManager();
    protected final LockupManager lockupManager = plugin.getLockupManager();

    protected Component mmDeserialize(String message, TagResolver... placeholders)
    {
        return FUtil.miniMessage(message, placeholders);
    }
}