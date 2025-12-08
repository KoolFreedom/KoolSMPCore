package eu.koolfreedom.listener;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.listener.impl.MuteManager;
import org.bukkit.event.Listener;

/**
 * Yes, this class can be seen as completely useless, but I'm also fucking stupid.
 * I'm going to forget to register listeners if I continue to make them, so I made this out of desperation.
 *
 * I'm not a good developer at all, I'm actually kinda slow. This helps..... a lot.....
 *
 * - gamingto12
 */
public abstract class KoolListener implements Listener
{
    public KoolListener()
    {
        KoolSMPCore.getInstance().getServer().getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
    }

    protected KoolSMPCore plugin;
    protected MuteManager muteManager;
}
