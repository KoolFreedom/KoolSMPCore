package eu.koolfreedom.player;

import eu.koolfreedom.freeze.FreezeData;
import eu.koolfreedom.note.PlayerNote;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * In-memory representation of a player's persistent data row.
 * Loaded from the database on join, written through to the database on every mutation.
 * Session-only fields (tasks, join time) are never persisted.
 */
@Getter
public class PlayerData
{
    // Identity
    private final UUID uuid;
    private final Player player;

    // Session-only (never persisted)
    private final long joinTime = System.currentTimeMillis();
    @Setter private BukkitRunnable lockupTask = null;
    @Setter private FreezeData freezeData = null;

    // Persistent fields — mutated via PlayerRegistry which writes through to the DB
    private boolean muted;
    private boolean commandsBlocked;
    private boolean frozen;
    private boolean lockedUp;
    @Setter private String lastKnownIp;

    // Notes — source of truth is the player_notes table; this is the session cache
    private final List<PlayerNote> notes = new ArrayList<>();

    /**
     * Constructor used when loading from the database on join.
     */
    public PlayerData(Player player, boolean muted, boolean commandsBlocked,
                      boolean frozen, boolean lockedUp, String lastKnownIp)
    {
        this.uuid = player.getUniqueId();
        this.player = player;
        this.muted = muted;
        this.commandsBlocked = commandsBlocked;
        this.frozen = frozen;
        this.lockedUp = lockedUp;
        this.lastKnownIp = lastKnownIp != null ? lastKnownIp
                : (player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null);
    }

    /**
     * Constructor used for brand new players with no existing DB row.
     */
    public PlayerData(Player player)
    {
        this(player, false, false, false, false,
                player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null);
    }

    // --- Persistent state setters (called by PlayerRegistry which handles the DB write) ---

    void applyMuted(boolean value)
    {
        this.muted = value;
    }
    void applyCommandsBlocked(boolean value)
    {
        this.commandsBlocked = value;
    }
    void applyFrozen(boolean value)
    {
        this.frozen = value;
    }
    void applyLockedUp(boolean value)
    {
        this.lockedUp = value;
    }

    // --- Derived state ---

    public boolean isFrozenInSession()
    {
        return freezeData != null && freezeData.isFrozen();
    }

    public long getPlaytimeMillis()
    {
        return System.currentTimeMillis() - joinTime;
    }

    // --- Notes (session cache) ---

    public void cacheNote(PlayerNote note)
    {
        notes.add(note);
    }
    public void uncacheNote(PlayerNote note)
    {
        notes.remove(note);
    }
    public List<PlayerNote> getNotes()
    {
        return Collections.unmodifiableList(notes);
    }
    public boolean hasNotes()
    {
        return !notes.isEmpty();
    }
}