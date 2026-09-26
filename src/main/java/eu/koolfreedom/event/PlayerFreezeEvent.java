package eu.koolfreedom.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import eu.koolfreedom.freeze.FreezeData;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PlayerFreezeEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final FreezeState state;
    private final FreezeData data; // null on UNFREEZE if you don't keep it around

    @Setter
    private boolean cancelled;

    public PlayerFreezeEvent(Player player, FreezeState state, FreezeData data)
    {
        this.player = player;
        this.state = state;
        this.data = data;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS;
    }

    public enum FreezeState
    {
        FROZEN,
        UNFROZEN
    }
}