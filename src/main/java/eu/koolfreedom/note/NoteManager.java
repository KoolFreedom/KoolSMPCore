package eu.koolfreedom.note;

import java.util.*;

public class NoteManager
{
    private final Map<UUID, List<PlayerNote>> notes = new HashMap<>();

    public void addNote(UUID playerUUID, PlayerNote note)
    {
        notes.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(note);
    }

    public void removeNote(UUID playerUUID, PlayerNote note)
    {
        List<PlayerNote> playerNotes = notes.get(playerUUID);
        if (playerNotes == null)
        {
            return;
        }

        playerNotes.remove(note);
    }

    public List<PlayerNote> getNotes(UUID playerUUID)
    {
        return notes.getOrDefault(playerUUID, List.of());
    }

    public boolean hasNotes(UUID playerUUID)
    {
        return notes.containsKey(playerUUID);
    }
}
