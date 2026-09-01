package eu.koolfreedom.player;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.banning.Ban;
import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.note.PlayerNote;
import eu.koolfreedom.punishment.Punishment;
import eu.koolfreedom.reporting.Report;
import eu.koolfreedom.util.FLog;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single source of truth for all persistent plugin data.
 *
 * Owns:
 *  - koolsmpcore.db (SQLite)
 *  - UUID → PlayerData in-memory cache
 *  - All read/write operations for player_data, player_notes, player_alts,
 *    bans, punishments, and reports tables
 */
public class PlayerRegistry extends KoolListener
{
    private static final SimpleDateFormat LEGACY_PUNISHMENT_DATE =
            new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    private long legacyIdSeed = System.currentTimeMillis();
    private Connection connection;

    public PlayerRegistry()
    {
        initDatabase();
    }

    private void initDatabase()
    {
        try
        {
            KoolSMPCore.getInstance().getDataFolder().mkdirs();
            String path = KoolSMPCore.getInstance().getDataFolder().getAbsolutePath() + "/koolsmpcore.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement s = connection.createStatement()) { s.execute("PRAGMA journal_mode=WAL"); }
            createTables();
            migrateLegacyFiles();
            FLog.info("SQLite database initialised at {}", path);
        }
        catch (SQLException e)
        {
            FLog.error("Failed to initialise SQLite database: " + e.getMessage());
        }
    }

    private synchronized void createTables() throws SQLException
    {
        try (Statement s = connection.createStatement())
        {
            // Player state
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_data (
                    uuid TEXT PRIMARY KEY,
                    last_known_ip TEXT,
                    muted INTEGER NOT NULL DEFAULT 0,
                    commands_blocked INTEGER NOT NULL DEFAULT 0,
                    frozen INTEGER NOT NULL DEFAULT 0,
                    locked_up INTEGER NOT NULL DEFAULT 0
                )""");

            // Notes
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_notes (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid      TEXT    NOT NULL,
                    author    TEXT    NOT NULL,
                    message   TEXT    NOT NULL,
                    timestamp TEXT    NOT NULL
                )""");

            // Alt tracking
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_alts (
                    ip TEXT NOT NULL,
                    uuid TEXT NOT NULL,
                    PRIMARY KEY (ip, uuid)
                )""");

            // Bans
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bans (
                    id INTEGER PRIMARY KEY,
                    uuid TEXT,
                    name TEXT,
                    by TEXT,
                    reason TEXT,
                    expires INTEGER NOT NULL DEFAULT 9223372036854775807,
                    ips TEXT NOT NULL DEFAULT ''
                )""");

            // Punishments (audit log)
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS punishments (
                    id INTEGER PRIMARY KEY,
                    uuid TEXT,
                    name TEXT,
                    ip TEXT,
                    by TEXT,
                    reason TEXT,
                    type TEXT
                )""");

            // Reports
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS reports (
                    id TEXT PRIMARY KEY,
                    reporter TEXT NOT NULL,
                    reported TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'UNRESOLVED',
                    last_note TEXT,
                    discord_message_id TEXT,
                    handlers TEXT NOT NULL DEFAULT ''
                )""");

            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS legacy_migrations (
                    name TEXT PRIMARY KEY,
                    migrated_at INTEGER NOT NULL
                )""");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(KoolSMPCore.getInstance(), () ->
        {
            PlayerData data = loadOrCreate(player);
            cache.put(player.getUniqueId(), data);

            String ip = player.getAddress() != null
                    ? player.getAddress().getAddress().getHostAddress() : null;
            if (ip != null)
            {
                data.setLastKnownIp(ip);
                updateFieldSync(player.getUniqueId(), "last_known_ip", ip);
                recordAltSync(ip, player.getUniqueId());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerData data = cache.remove(uuid);
        if (data == null) return;
        if (data.getLockupTask() != null) data.getLockupTask().cancel();
        if (data.getFreezeData() != null) data.getFreezeData().clearTask();
    }

    public Optional<PlayerData> get(UUID uuid)
    {
        return Optional.ofNullable(cache.get(uuid));
    }
    public Optional<PlayerData> get(Player p)
    {
        return get(p.getUniqueId());
    }
    public Collection<PlayerData> getAll()
    {
        return cache.values();
    }

    public PlayerData require(Player player)
    {
        return cache.computeIfAbsent(player.getUniqueId(), k -> loadOrCreate(player));
    }

    public void setMuted(UUID uuid, boolean v)
    {
        get(uuid).ifPresent(d -> d.applyMuted(v));
        updateFieldAsync(uuid, "muted", v ? 1 : 0);
    }

    public boolean isMuted(UUID uuid)
    {
        return get(uuid).map(PlayerData::isMuted).orElseGet(() -> getBooleanFieldSync(uuid, "muted"));
    }

    public int clearMutes()
    {
        int count = countBooleanFieldSync("muted");
        getAll().forEach(data -> data.applyMuted(false));
        updateAllFieldSync("muted", 0);
        return count;
    }

    public void setCommandsBlocked(UUID uuid, boolean v)
    {
        get(uuid).ifPresent(d -> d.applyCommandsBlocked(v));
        updateFieldAsync(uuid, "commands_blocked", v ? 1 : 0);
    }

    public boolean isCommandsBlocked(UUID uuid)
    {
        return get(uuid).map(PlayerData::isCommandsBlocked).orElseGet(() -> getBooleanFieldSync(uuid, "commands_blocked"));
    }

    public int clearCommandsBlocked()
    {
        int count = countBooleanFieldSync("commands_blocked");
        getAll().forEach(data -> data.applyCommandsBlocked(false));
        updateAllFieldSync("commands_blocked", 0);
        return count;
    }

    public void setFrozen(UUID uuid, boolean v)
    {
        get(uuid).ifPresent(d -> d.applyFrozen(v));
        updateFieldAsync(uuid, "frozen", v ? 1 : 0);
    }

    public boolean isFrozen(UUID uuid)
    {
        return get(uuid).map(PlayerData::isFrozen).orElseGet(() -> getBooleanFieldSync(uuid, "frozen"));
    }

    public void setLockedUp(UUID uuid, boolean v)
    {
        get(uuid).ifPresent(d -> d.applyLockedUp(v));
        updateFieldAsync(uuid, "locked_up", v ? 1 : 0);
    }

    public boolean isLockedUp(UUID uuid)
    {
        return get(uuid).map(PlayerData::isLockedUp).orElseGet(() -> getBooleanFieldSync(uuid, "locked_up"));
    }

    public void addNote(UUID uuid, PlayerNote note)
    {
        async(() -> addNoteSync(uuid, note));
        get(uuid).ifPresent(d -> d.cacheNote(note));
    }

    public void removeNote(UUID uuid, PlayerNote note)
    {
        async(() -> removeNoteSync(uuid, note));
        get(uuid).ifPresent(d -> d.uncacheNote(note));
    }

    public List<PlayerNote> getNotes(UUID uuid)
    {
        return get(uuid).map(PlayerData::getNotes).orElseGet(() -> loadNotesSync(uuid));
    }

    public boolean hasNotes(UUID uuid)
    {
        return get(uuid).map(PlayerData::hasNotes).orElseGet(() -> !loadNotesSync(uuid).isEmpty());
    }

    public void recordAlt(String ip, UUID uuid)
    {
        async(() -> recordAltSync(ip, uuid));
    }

    public synchronized Set<UUID> getAlts(String ip)
    {
        Set<UUID> alts = new HashSet<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid FROM player_alts WHERE ip=?"))
        {
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) alts.add(UUID.fromString(rs.getString("uuid")));
        }
        catch (SQLException e)
        {
            FLog.error("getAlts: " + e.getMessage());
        }
        return alts;
    }

    public synchronized Optional<String> getLastIp(UUID uuid)
    {
        return get(uuid).map(d -> Optional.ofNullable(d.getLastKnownIp())).orElseGet(() ->
        {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT last_known_ip FROM player_data WHERE uuid=?"))
            {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.ofNullable(rs.getString("last_known_ip"));
            }
            catch (SQLException e)
            {
                FLog.error("getLastIp: " + e.getMessage());
            }
            return Optional.empty();
        });
    }

    public void saveBan(Ban ban)
    {
        async(() -> saveBanSync(ban));
    }

    public void deleteBan(long id)
    {
        async(() -> deleteBanSync(id));
    }

    public synchronized Map<Long, Ban> loadAllBans()
    {
        Map<Long, Ban> bans = new LinkedHashMap<>();
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM bans"))
        {
            while (rs.next())
            {
                Ban ban = banFromRow(rs);
                if (!ban.isExpired()) bans.put(ban.getId(), ban);
            }
        }
        catch (SQLException e)
        {
            FLog.error("loadAllBans: " + e.getMessage());
        }
        return bans;
    }

    public void savePunishment(Punishment p)
    {
        async(() -> savePunishmentSync(p));
    }

    public synchronized Map<Long, Punishment> loadAllPunishments()
    {
        Map<Long, Punishment> map = new LinkedHashMap<>();
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM punishments ORDER BY id ASC"))
        {
            while (rs.next())
            {
                Punishment p = Punishment.builder()
                        .issued(rs.getLong("id"))
                        .uuid(rs.getString("uuid") != null ? UUID.fromString(rs.getString("uuid")) : null)
                        .name(rs.getString("name"))
                        .ip(rs.getString("ip"))
                        .by(rs.getString("by"))
                        .reason(rs.getString("reason"))
                        .type(rs.getString("type"))
                        .build();
                map.put(p.getIssued(), p);
            }
        }
        catch (SQLException e)
        {
            FLog.error("loadAllPunishments: " + e.getMessage());
        }
        return map;
    }

    public void saveReport(Report report)
    {
        async(() -> saveReportSync(report));
    }

    public void deleteReport(String id)
    {
        async(() -> deleteReportSync(id));
    }

    public synchronized Map<String, Report> loadAllReports()
    {
        Map<String, Report> map = new LinkedHashMap<>();
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM reports ORDER BY timestamp ASC"))
        {
            while (rs.next())
            {
                String handlers = rs.getString("handlers");
                Report report = Report.builder()
                        .id(rs.getString("id"))
                        .reporter(UUID.fromString(rs.getString("reporter")))
                        .reported(UUID.fromString(rs.getString("reported")))
                        .reason(rs.getString("reason"))
                        .timestamp(rs.getLong("timestamp"))
                        .status(Report.ReportStatus.fromNameSafely(rs.getString("status")))
                        .lastNote(rs.getString("last_note"))
                        .handlers(handlers != null && !handlers.isBlank()
                                ? new ArrayList<>(Arrays.asList(handlers.split(",")))
                                : new ArrayList<>())
                        .build();
                map.put(report.getId(), report);
            }
        }
        catch (SQLException e)
        {
            FLog.error("loadAllReports: " + e.getMessage());
        }
        return map;
    }

    private void migrateLegacyFiles()
    {
        migrateOnce("legacy_bans", this::migrateLegacyBans);
        migrateOnce("legacy_punishments", this::migrateLegacyPunishments);
        migrateOnce("legacy_reports", this::migrateLegacyReports);
    }

    private void migrateOnce(String name, Runnable migration)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM legacy_migrations WHERE name=?"))
        {
            ps.setString(1, name);
            if (ps.executeQuery().next()) return;
        }
        catch (SQLException e)
        {
            FLog.error("Unable to check migration " + name + ": " + e.getMessage());
            return;
        }

        migration.run();

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO legacy_migrations (name, migrated_at) VALUES (?, ?)"))
        {
            ps.setString(1, name);
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("Unable to mark migration " + name + ": " + e.getMessage());
        }
    }

    private void migrateLegacyBans()
    {
        File bansFile = legacyFile("bans.yml");
        File permbansFile = legacyFile("permbans.yml");
        int count = 0;

        if (permbansFile.exists())
        {
            YamlConfiguration permbans = YamlConfiguration.loadConfiguration(permbansFile);
            for (String name : permbans.getStringList("names"))
            {
                saveBanSync(Ban.builder()
                        .id(nextLegacyId())
                        .name(name)
                        .expires(Long.MAX_VALUE)
                        .build());
                count++;
            }
            for (String ip : permbans.getStringList("ips"))
            {
                saveBanSync(Ban.builder()
                        .id(nextLegacyId())
                        .ips(List.of(ip))
                        .expires(Long.MAX_VALUE)
                        .build());
                count++;
            }
        }

        if (bansFile.exists())
        {
            YamlConfiguration bans = YamlConfiguration.loadConfiguration(bansFile);
            for (String id : bans.getKeys(false))
            {
                if (!bans.isConfigurationSection(id)) continue;

                Ban ban = banFromLegacySection(id, Objects.requireNonNull(bans.getConfigurationSection(id)));
                if (!ban.isExpired())
                {
                    saveBanSync(ban);
                    count++;
                }
            }
        }

        if (count > 0) FLog.info("Migrated {} legacy ban(s) into SQLite.", count);
    }

    private void migrateLegacyPunishments()
    {
        File punishmentsFile = legacyFile("punishments.yml");
        if (!punishmentsFile.exists()) return;

        YamlConfiguration punishments = YamlConfiguration.loadConfiguration(punishmentsFile);
        int count = 0;

        for (String id : punishments.getKeys(false))
        {
            if (!punishments.isConfigurationSection(id)) continue;

            Punishment punishment = punishmentFromLegacySection(
                    id, Objects.requireNonNull(punishments.getConfigurationSection(id)));
            savePunishmentSync(punishment);
            count++;
        }

        if (count > 0) FLog.info("Migrated {} legacy punishment(s) into SQLite.", count);
    }

    private void migrateLegacyReports()
    {
        File reportsFile = legacyFile("reports.yml");
        if (!reportsFile.exists()) return;

        YamlConfiguration reports = YamlConfiguration.loadConfiguration(reportsFile);
        int count = 0;

        for (String id : reports.getKeys(false))
        {
            if (!reports.isConfigurationSection(id)) continue;

            try
            {
                Report report = Report.fromConfigurationSection(
                        id, Objects.requireNonNull(reports.getConfigurationSection(id)));
                saveReportSync(report);
                count++;
            }
            catch (RuntimeException e)
            {
                FLog.warning("Skipping invalid legacy report {}: {}", id, e.getMessage());
            }
        }

        if (count > 0) FLog.info("Migrated {} legacy report(s) into SQLite.", count);
    }

    private synchronized PlayerData loadOrCreate(Player player)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM player_data WHERE uuid=?"))
        {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                PlayerData data = new PlayerData(
                        player,
                        rs.getInt("muted") == 1,
                        rs.getInt("commands_blocked") == 1,
                        rs.getInt("frozen") == 1,
                        rs.getInt("locked_up") == 1,
                        rs.getString("last_known_ip")
                );
                for (PlayerNote note : loadNotesSync(player.getUniqueId()))
                    data.cacheNote(note);
                return data;
            }
            else
            {
                try (PreparedStatement ins = connection.prepareStatement(
                        "INSERT OR IGNORE INTO player_data (uuid) VALUES (?)"))
                {
                    ins.setString(1, player.getUniqueId().toString());
                    ins.executeUpdate();
                }
                return new PlayerData(player);
            }
        }
        catch (SQLException e)
        {
            FLog.error("loadOrCreate: " + e.getMessage());
            return new PlayerData(player);
        }
    }

    private synchronized void addNoteSync(UUID uuid, PlayerNote note)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_notes (uuid, author, message, timestamp) VALUES (?, ?, ?, ?)"))
        {
            ps.setString(1, uuid.toString());
            ps.setString(2, note.author());
            ps.setString(3, note.message());
            ps.setString(4, note.timestamp().toString());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("addNote: " + e.getMessage());
        }
    }

    private synchronized void removeNoteSync(UUID uuid, PlayerNote note)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_notes WHERE uuid=? AND author=? AND message=? AND timestamp=?"))
        {
            ps.setString(1, uuid.toString());
            ps.setString(2, note.author());
            ps.setString(3, note.message());
            ps.setString(4, note.timestamp().toString());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("removeNote: " + e.getMessage());
        }
    }

    private synchronized void deleteBanSync(long id)
    {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM bans WHERE id=?"))
        {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("deleteBan: " + e.getMessage());
        }
    }

    private synchronized void deleteReportSync(String id)
    {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reports WHERE id=?"))
        {
            ps.setString(1, id);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("deleteReport: " + e.getMessage());
        }
    }

    private synchronized List<PlayerNote> loadNotesSync(UUID uuid)
    {
        List<PlayerNote> notes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM player_notes WHERE uuid=? ORDER BY id ASC"))
        {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                notes.add(new PlayerNote(
                        rs.getString("author"),
                        rs.getString("message"),
                        LocalDateTime.parse(rs.getString("timestamp"))
                ));
        }
        catch (SQLException e)
        {
            FLog.error("loadNotes: " + e.getMessage());
        }
        return notes;
    }

    private synchronized void recordAltSync(String ip, UUID uuid)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO player_alts (ip, uuid) VALUES (?, ?)"))
        {
            ps.setString(1, ip);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("recordAlt: " + e.getMessage());
        }
    }

    private synchronized void updateFieldSync(UUID uuid, String col, Object val)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET " + col + "=? WHERE uuid=?"))
        {
            ps.setObject(1, val);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("updateField " + col + ": " + e.getMessage());
        }
    }

    private synchronized boolean getBooleanFieldSync(UUID uuid, String col)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT " + col + " FROM player_data WHERE uuid=?"))
        {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(col) == 1;
        }
        catch (SQLException e)
        {
            FLog.error("getBooleanField " + col + ": " + e.getMessage());
            return false;
        }
    }

    private synchronized int countBooleanFieldSync(String col)
    {
        try (Statement s = connection.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS count FROM player_data WHERE " + col + "=1"))
        {
            return rs.next() ? rs.getInt("count") : 0;
        }
        catch (SQLException e)
        {
            FLog.error("countBooleanField " + col + ": " + e.getMessage());
            return 0;
        }
    }

    private synchronized void updateAllFieldSync(String col, Object val)
    {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET " + col + "=?"))
        {
            ps.setObject(1, val);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("updateAllField " + col + ": " + e.getMessage());
        }
    }

    private void updateFieldAsync(UUID uuid, String col, Object val)
    {
        async(() -> updateFieldSync(uuid, col, val));
    }

    private synchronized void saveBanSync(Ban ban)
    {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT OR REPLACE INTO bans (id, uuid, name, by, reason, expires, ips)
            VALUES (?, ?, ?, ?, ?, ?, ?)"""))
        {
            ps.setLong(1, ban.getId());
            ps.setString(2, ban.getUuid() != null ? ban.getUuid().toString() : null);
            ps.setString(3, ban.getName());
            ps.setString(4, ban.getBy());
            ps.setString(5, ban.getReason());
            ps.setLong(6, ban.getExpires());
            ps.setString(7, String.join(",", ban.getIps()));
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("saveBan: " + e.getMessage());
        }
    }

    private synchronized void savePunishmentSync(Punishment p)
    {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT OR REPLACE INTO punishments (id, uuid, name, ip, by, reason, type)
            VALUES (?, ?, ?, ?, ?, ?, ?)"""))
        {
            ps.setLong(1, p.getIssued());
            ps.setString(2, p.getUuid() != null ? p.getUuid().toString() : null);
            ps.setString(3, p.getName());
            ps.setString(4, p.getIp());
            ps.setString(5, p.getBy());
            ps.setString(6, p.getReason());
            ps.setString(7, p.getType());
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("savePunishment: " + e.getMessage());
        }
    }

    private synchronized void saveReportSync(Report report)
    {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT OR REPLACE INTO reports (id, reporter, reported, reason, timestamp, status, last_note, handlers)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)"""))
        {
            ps.setString(1, report.getId());
            ps.setString(2, report.getReporter().toString());
            ps.setString(3, report.getReported().toString());
            ps.setString(4, report.getReason());
            ps.setLong(5, report.getTimestamp());
            ps.setString(6, report.getStatus().name());
            ps.setString(7, report.getLastNote());
            ps.setString(8, String.join(",", report.getHandlers()));
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            FLog.error("saveReport: " + e.getMessage());
        }
    }

    private Ban banFromRow(ResultSet rs) throws SQLException
    {
        String uuidStr = rs.getString("uuid");
        String ipsStr  = rs.getString("ips");
        return Ban.builder()
                .id(rs.getLong("id"))
                .uuid(uuidStr != null ? UUID.fromString(uuidStr) : null)
                .name(rs.getString("name"))
                .by(rs.getString("by"))
                .reason(rs.getString("reason"))
                .expires(rs.getLong("expires"))
                .ips(ipsStr != null && !ipsStr.isBlank()
                        ? new ArrayList<>(Arrays.asList(ipsStr.split(",")))
                        : new ArrayList<>())
                .build();
    }

    private Ban banFromLegacySection(String id, ConfigurationSection section)
    {
        Ban.BanBuilder builder = Ban.builder().id(parseLong(id, nextLegacyId()));

        String uuid = section.getString("uuid");
        if (uuid != null && !uuid.isBlank())
        {
            try
            {
                builder.uuid(UUID.fromString(uuid));
            }
            catch (IllegalArgumentException e)
            {
                FLog.warning("Legacy ban {} contains invalid UUID: {}", id, uuid);
            }
        }

        builder.name(blankToNull(section.getString("name")));
        builder.by(blankToNull(section.getString("by", section.getString("punisher"))));
        builder.reason(blankToNull(section.getString("reason")));
        builder.expires(section.getLong("expires", section.getLong("length", Long.MAX_VALUE)));

        if (section.contains("ips"))
            builder.ips(new ArrayList<>(section.getStringList("ips")));
        else
            builder.ips(new ArrayList<>());

        return builder.build();
    }

    private Punishment punishmentFromLegacySection(String id, ConfigurationSection section)
    {
        Punishment.PunishmentBuilder builder = Punishment.builder()
                .issued(parseLong(id, legacyPunishmentIssued(section)));

        String uuid = section.getString("uuid");
        if (uuid != null && !uuid.isBlank())
        {
            try
            {
                builder.uuid(UUID.fromString(uuid));
            }
            catch (IllegalArgumentException ignored)
            {
                builder.uuid(null);
            }
        }

        return builder
                .name(blankToNull(section.getString("name")))
                .ip(blankToNull(section.getString("ip")))
                .by(blankToNull(section.getString("by")))
                .reason(blankToNull(section.getString("reason")))
                .type(blankToNull(section.getString("type")))
                .build();
    }

    private long legacyPunishmentIssued(ConfigurationSection section)
    {
        if (section.isLong("issued")) return section.getLong("issued");

        if (section.isString("issued"))
        {
            try
            {
                return LEGACY_PUNISHMENT_DATE.parse(Objects.requireNonNull(section.getString("issued"))).getTime();
            }
            catch (ParseException e)
            {
                FLog.warning("Invalid legacy punishment issued timestamp: {}", section.getString("issued"));
            }
        }

        return System.currentTimeMillis();
    }

    private File legacyFile(String name)
    {
        return new File(KoolSMPCore.getInstance().getDataFolder(), name);
    }

    private long nextLegacyId()
    {
        return ++legacyIdSeed;
    }

    private long parseLong(String value, long fallback)
    {
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException ignored)
        {
            return fallback;
        }
    }

    private String blankToNull(String value)
    {
        return value == null || value.isBlank() ? null : value;
    }

    private void async(Runnable task)
    {
        Bukkit.getScheduler().runTaskAsynchronously(KoolSMPCore.getInstance(), task);
    }

    public synchronized void close()
    {
        try
        {
            if (connection != null && !connection.isClosed()) connection.close();
        }
        catch (SQLException e)
        {
            FLog.error("Failed to close DB: " + e.getMessage());
        }
    }
}
