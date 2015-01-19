package com.comphenix.proximity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Invoked when a player is no longer recieving information about a given entity.
 *
 * @author Kristian
 */
public class PlayerUnloadEntityEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private Entity unloadedEntity;

    public PlayerUnloadEntityEvent(Player who, Entity unloadedEntity) {
        super(who);
        this.unloadedEntity = unloadedEntity;
    }

    /**
     * Retrieve the unloaded entity.
     *
     * @return Entity nearby.
     */
    public Entity getUnloadedEntity() {
        return unloadedEntity;
    }

    /**
     * This is a Bukkit method. Don't touch me.
     *
     * @return registered handlers to Bukkit
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        // TODO Auto-generated method stub
        return handlers;
    }
}