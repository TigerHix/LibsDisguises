package com.comphenix.proximity;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.Maps;
import me.libraryaddict.disguise.LibsDisguises;
import net.minecraft.server.v1_8_R1.EntityPlayer;
import net.minecraft.server.v1_8_R1.EntityTracker;
import net.minecraft.server.v1_8_R1.EntityTrackerEntry;
import net.minecraft.server.v1_8_R1.WorldServer;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityProximityDetector implements Listener {
    // Number of ticks to wait
    private static final int TASK_DELAY = 10;

    // Used to revert the previous set
    private Map<EntityTrackerEntry, Set<EntityPlayer>> notchSets = Maps.newHashMap();
    private Map<Integer, EntityTrackerEntry> lookup = Maps.newHashMap();

    // Entities to inject
    private Deque<Entity> toInject = new ArrayDeque<Entity>();

    private Plugin plugin;
    private BukkitScheduler scheduler;
    private PluginManager manager;
    private int taskID = -1;

    public EntityProximityDetector(Plugin plugin) {
        this(plugin,
                plugin.getServer().getScheduler(),
                plugin.getServer().getPluginManager()
        );
    }

    public EntityProximityDetector(Plugin plugin, BukkitScheduler scheduler, PluginManager manager) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.manager = manager;
    }

    /**
     * Initialize the proximity detector.
     *
     * @param worlds - list of already loaded worlds.
     */
    public void initialize(List<World> worlds) {
        scheduleTask();
        register(manager);
        initializeWorlds(worlds);
    }

    /**
     * Initialize the task that is responsible for injecting into the entity tracker.
     */
    private void scheduleTask() {
        if (taskID >= 0)
            throw new IllegalStateException("Cannot schedule multiple tasks.");

        // Schedule using the default delay (once per tick)
        taskID = scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (toInject.size() > 0) {
                    // Inject every scheduled entity
                    for (Iterator<Entity> it = toInject.descendingIterator(); it.hasNext(); ) {
                        // Remove successful injections
                        if (injectEntity(it.next()))
                            it.remove();
                    }
                }
            }
        }, TASK_DELAY, TASK_DELAY);

        // Might as well check this
        if (taskID < 0) {
            throw new IllegalStateException("Cannot schedule repeating task.");
        }
    }

    /**
     * Cancel a previously scheduled task.
     */
    private void cancelTask() {
        if (taskID >= 0) {
            scheduler.cancelTask(taskID);
            taskID = -1;
        }
    }

    /**
     * Register this proximity detector as an event listener.
     *
     * @param manager - plugin manager.
     */
    private void register(PluginManager manager) {
        manager.registerEvents(this, plugin);
    }

    /**
     * Initialize based on already loaded chunks.
     */
    private void initializeWorlds(List<World> worlds) {
        for (World world : worlds) {
            for (Chunk chunk : world.getLoadedChunks()) {
                initializeChunk(chunk);
            }
        }
    }

    /**
     * Initialize loaded chunks.
     *
     * @param chunk - the chunk with every loaded entity.
     */
    private void initializeChunk(Chunk chunk) {
        if (chunk == null)
            return;

        // Inject into every existing entity
        for (Entity entity : chunk.getEntities()) {
            if (entity != null)
                toInject.addLast(entity);
        }
    }

    /**
     * Unload entities from chunk, if they have been injected before.
     *
     * @param chunk - the chunk with injected entities.
     */
    private void unloadChunk(Chunk chunk) {
        if (chunk == null)
            return;

        // Remove loaded entities
        for (Entity entity : chunk.getEntities()) {
            if (entity != null)
                uninjectEntity(entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        initializeChunk(event.getChunk());
    }

    public void onChunkUnloadedEvent(ChunkUnloadEvent event) {
        unloadChunk(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoinedEvent(PlayerJoinEvent event) {
        if (event.getPlayer() != null)
            toInject.addLast(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        if (event.getEntity() != null)
            toInject.addLast(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeathEvent(CreatureSpawnEvent event) {
        uninjectEntity(event.getEntity());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        uninjectEntity(event.getPlayer());
    }

    private Map<Player, List<Entity>> map = new ConcurrentHashMap<Player, List<Entity>>();

    // Invoked the first time an entity is nearby another player
    protected void notifyAdding(final Player observer, final Entity visible) {
        if (map.containsKey(observer) && map.get(observer).contains(visible)) {
            return;
        }
        List<Entity> list = map.get(observer);
        if (list == null) {
            list = new ArrayList<Entity>();
        }
        list.add(visible);
        map.put(observer, list);
        PlayerLoadEntityEvent loaded = new PlayerLoadEntityEvent(observer, visible);
        manager.callEvent(loaded);
        new BukkitRunnable() {
            @Override
            public void run() {
                map.get(observer).remove(visible);
            }
        }.runTaskLater(LibsDisguises.instance(), 5L);
    }

    // Invoked the first time an entity leaves the vicinity of another player
    protected void notifyRemoving(Player observer, Entity visible) {
        PlayerUnloadEntityEvent unloaded = new PlayerUnloadEntityEvent(observer, visible);
        manager.callEvent(unloaded);
    }

    protected void notifyAdding(Entity visible, Collection<?> source, Collection<?> target) {
        for (Object element : source) {
            if (target == null || (element instanceof EntityPlayer && !target.contains(element)))
                notifyAdding(getBukkitPlayer(element), visible);
        }
    }

    protected void notifyRemoving(Entity visible, Collection<?> source, Collection<?> target) {
        for (Object element : source) {
            if (target == null || (element instanceof EntityPlayer && target.contains(element)))
                notifyRemoving(getBukkitPlayer(element), visible);
        }
    }

    private Player getBukkitPlayer(Object entityPlayer) {
        return ((EntityPlayer) entityPlayer).getBukkitEntity();
    }

    protected void uninjectEntity(Entity entity) {
        int entityID = entity.getEntityId();
        EntityTrackerEntry entry = lookup.get(entityID);

        if (entry != null) {
            uninjectEntity(entry);

            // Clean up
            lookup.remove(entityID);
            notchSets.remove(entry);
        }
    }

    private void uninjectEntity(EntityTrackerEntry entry) {
        // Revert to the old tracker
        if (entry != null) {
            entry.trackedPlayers = notchSets.get(entry);
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean injectEntity(final Entity visible) {

        final World world = visible.getWorld();
        final WorldServer worldServer = ((CraftWorld) world).getHandle();

        final EntityTracker tracker = worldServer.tracker;
        final EntityTrackerEntry entry = (EntityTrackerEntry) tracker.trackedEntities.
                get(visible.getEntityId());

        // Wait for the next tick if the entity isn't tracked yet
        if (entry == null)
            return false;
            // Stop if another plugin has already injected into this set
        else if (entry.trackedPlayers instanceof ForwardingSet)
            return true;

        final Set<EntityPlayer> notchSet = entry.trackedPlayers;
        lookup.put(visible.getEntityId(), entry);
        notchSets.put(entry, notchSet);

        // Notify the already existing players
        notifyAdding(visible, entry.trackedPlayers, null);

        entry.trackedPlayers = new ForwardingSet<EntityPlayer>() {
            @Override
            protected Set<EntityPlayer> delegate() {
                return notchSet;
            }

            @Override
            public boolean add(EntityPlayer element) {
                boolean success = super.add(element);

                // Notify if this player was actually added
                if (success)
                    notifyAdding(element.getBukkitEntity(), visible);
                return success;
            }

            @Override
            public boolean addAll(Collection<? extends EntityPlayer> collection) {
                notifyAdding(visible, collection, this);
                return super.addAll(collection);
            }

            @Override
            public boolean remove(Object object) {
                boolean success = super.remove(object);

                if (object instanceof EntityPlayer && success)
                    notifyRemoving(getBukkitPlayer(object), visible);
                return success;
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                notifyRemoving(visible, collection, this);
                return super.removeAll(collection);
            }

            @Override
            public void clear() {
                notifyRemoving(visible, this, this);
                super.clear();
            }
        };

        // Successful injection
        return true;
    }

    public void close() {
        // Revert every set
        for (EntityTrackerEntry entry : notchSets.keySet()) {
            uninjectEntity(entry);
        }

        notchSets.clear();
        lookup.clear();
        cancelTask();
    }
}