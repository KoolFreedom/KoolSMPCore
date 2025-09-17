package eu.koolfreedom.api;

import java.util.*;

public class AltManager
{
    private final Map<String, Set<UUID>> ipMap = new HashMap<>();
    private final Map<UUID, String> lastIpMap  = new HashMap<>();

    public void record(String ip, UUID uuid)
    {
        ipMap.computeIfAbsent(ip, k -> new HashSet<>()).add(uuid);
        lastIpMap.put(uuid, ip); // overwrite old mapping
    }

    /** All accounts seen from this IP */
    public Set<UUID> getAlts(String ip)
    {
        return ipMap.getOrDefault(ip, Collections.emptySet());
    }

    /** Alias */
    public Set<UUID> getAccounts(String ip)
    {
        return getAlts(ip);
    }

    /** Most recent IP for this account */
    public Optional<String> getLastIP(UUID uuid)
    {
        return Optional.ofNullable(lastIpMap.get(uuid));
    }
}
