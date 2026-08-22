package eu.koolfreedom.api;

import eu.koolfreedom.player.PlayerRegistry;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Thin facade over PlayerRegistry's alt persistence.
 * Kept so existing call sites don't need changing.
 */
public class AltManager
{
    private final PlayerRegistry playerRegistry;

    public AltManager(PlayerRegistry playerRegistry)
    {
        this.playerRegistry = playerRegistry;
    }

    public void record(String ip, UUID uuid)
    {
        playerRegistry.recordAlt(ip, uuid);
    }

    public Set<UUID> getAlts(String ip)
    {
        return playerRegistry.getAlts(ip);
    }

    public Set<UUID> getAccounts(String ip)
    {
        return getAlts(ip);
    }

    public Optional<String> getLastIP(UUID uuid)
    {
        return playerRegistry.getLastIp(uuid);
    }
}