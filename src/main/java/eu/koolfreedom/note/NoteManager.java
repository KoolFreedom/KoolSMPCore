package eu.koolfreedom.note;

import eu.koolfreedom.player.PlayerRegistry;

import java.util.List;
import java.util.UUID;

/**
 * Thin facade over PlayerRegistry's note persistence.
 * Kept so existing call sites (commands, ChatListener, etc.) don't need changing.
 */
public class NoteManager
{
    private final PlayerRegistry playerRegistry;

    public NoteManager(PlayerRegistry playerRegistry)
    {
        this.playerRegistry = playerRegistry;
    }

    public void addNote(UUID uuid, PlayerNote note)
    {
        playerRegistry.addNote(uuid, note);
    }

    public void removeNote(UUID uuid, PlayerNote note)
    {
        playerRegistry.removeNote(uuid, note);
    }

    public List<PlayerNote> getNotes(UUID uuid)
    {
        return playerRegistry.getNotes(uuid);
    }

    public boolean hasNotes(UUID uuid)
    {
        return playerRegistry.hasNotes(uuid);
    }
}