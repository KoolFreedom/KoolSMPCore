package eu.koolfreedom.stats;

import eu.koolfreedom.KoolSMPCore;
import net.ess3.api.IEssentials;
import net.ess3.api.IUser;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeManager {
    private final File file = new File(KoolSMPCore.getInstance().getDataFolder(), "playtime.yml");
    private final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
    private final IEssentials essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

    public static class PlaytimeData {
        public long firstJoin = 0;         // millis
        public long lastSeen = 0;          // millis
        public long totalPlaySeconds = 0;  // stored seconds
        public Long sessionStart = null;   // millis
    }

    private final Map<UUID, PlaytimeData> data = new HashMap<>();

    public void load()
    {
        if (!file.exists())
        {
            try
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        for (String key : config.getKeys(false)) {
            UUID id = UUID.fromString(key);
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            PlaytimeData d = new PlaytimeData();
            d.firstJoin = section.getLong("firstJoin", 0);
            d.lastSeen = section.getLong("lastSeen", 0);
            d.totalPlaySeconds = section.getLong("totalPlaySeconds", 0);
            if (section.contains("sessionStart"))
                d.sessionStart = section.getLong("sessionStart");

            // crash recovery
            if (d.sessionStart != null) {
                long now = Instant.now().toEpochMilli();
                long delta = (now - d.sessionStart) / 1000L;
                d.totalPlaySeconds += Math.max(delta, 0);
                d.sessionStart = null;
                System.out.println("[Playtime] Recovered " + delta + "s for " + key);
            }

            data.put(id, d);
        }
    }

    public void save()
    {
        for (Map.Entry<UUID, PlaytimeData> entry : data.entrySet())
        {
            UUID id = entry.getKey();
            PlaytimeData d = entry.getValue();
            String path = id.toString();

            config.set(path + ".firstJoin", d.firstJoin);
            config.set(path + ".lastSeen", d.lastSeen);
            config.set(path + ".totalPlaySeconds", d.totalPlaySeconds);
            config.set(path + ".sessionStart", d.sessionStart);
        }

        try
        {
            config.save(file);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public PlaytimeData get(UUID id)
    {
        return data.computeIfAbsent(id, k -> new PlaytimeData());
    }

    public void set(UUID id, PlaytimeData pd)
    {
        data.put(id, pd);
    }

    public long getPlaytime(UUID id)
    {
        return get(id).totalPlaySeconds;
    }

    /** Includes active session time if online */
    public long getEffectivePlaytime(UUID id)
    {
        PlaytimeData d = get(id);
        long base = d.totalPlaySeconds;
        if (d.sessionStart != null) {
            long now = Instant.now().toEpochMilli();
            long delta = (now - d.sessionStart) / 1000L;
            return base + Math.max(delta, 0);
        }
        return base;
    }

    /** Call from PlayerJoinEvent */
    public void handleJoin(UUID id)
    {
        PlaytimeData d = get(id);
        long now = Instant.now().toEpochMilli();

        if (d.firstJoin == 0)
            d.firstJoin = now;

        d.lastSeen = now;
        d.sessionStart = now; // store millis
    }

    /** Call from PlayerQuitEvent */
    public void handleQuit(UUID id)
    {
        PlaytimeData d = get(id);
        if (d.sessionStart != null)
        {
            long now = Instant.now().toEpochMilli();
            long delta = (now - d.sessionStart) / 1000L;
            d.totalPlaySeconds += Math.max(delta, 0);
            d.lastSeen = now;
            System.out.println("[Playtime] + " + delta + "s for " + id);
            d.sessionStart = null;
        }
    }

    /** Optional: resync from Essentials/Bukkit stats */
    @SuppressWarnings("deprecation")
    public void importFromEssentials(UUID id)
    {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(id);
        if (offlinePlayer == null) return;

        try
        {
            int ticks = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);
            long playtimeSeconds = ticks / 20L;

            PlaytimeData d = get(id);

            // safer: only overwrite if ours is empty or theirs is bigger
            if (d.totalPlaySeconds == 0 || playtimeSeconds > d.totalPlaySeconds)
            {
                d.totalPlaySeconds = playtimeSeconds;
            }

            if (d.firstJoin == 0) d.firstJoin = offlinePlayer.getFirstPlayed();
            if (offlinePlayer.getLastPlayed() > 0) d.lastSeen = offlinePlayer.getLastPlayed();

            d.sessionStart = null;
            set(id, d);

            save();
            System.out.println("[Playtime] Imported " + playtimeSeconds + "s from Bukkit stats for " + id);
        }
        catch (Exception e)
        {
            System.out.println("[Playtime] Failed to import from Bukkit stats for " + id);
            e.printStackTrace();
        }
    }
}
