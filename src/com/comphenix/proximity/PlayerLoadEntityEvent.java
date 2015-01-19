package com.comphenix.proximity;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Invoked when a player is informed about the existence of a nearby entity.
 *
 * @author Kristian
 */
public class PlayerLoadEntityEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private Entity loadedEntity;

    public PlayerLoadEntityEvent(Player who, Entity loadedEntity) {
        super(who);
        this.loadedEntity = loadedEntity;
    }

    /**
     * Retrieve the loaded nearby entity.
     *
     * @return Nearby entity.
     */
    public Entity getLoadedEntity() {
        return loadedEntity;
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