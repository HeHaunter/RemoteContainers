package dev.hehaunter.remotecontainers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RemoteInventorySessionManager implements Listener {

    private record ChunkKey(UUID worldId, int x, int z) {}

    private final RemoteContainersPlugin plugin;
    private final Map<UUID, ChunkKey> playerSessions = new HashMap<>();
    private final Map<ChunkKey, Integer> chunkReferences = new HashMap<>();

    RemoteInventorySessionManager(RemoteContainersPlugin plugin) {
        this.plugin = plugin;
    }

    boolean acquire(Player player, Chunk chunk) {
        if (player == null || chunk == null) return false;

        release(player.getUniqueId());

        ChunkKey key = new ChunkKey(
                chunk.getWorld().getUID(),
                chunk.getX(),
                chunk.getZ()
        );

        int references = chunkReferences.getOrDefault(key, 0);

        if (references == 0) {
            try {
                boolean added = chunk.addPluginChunkTicket(plugin);
                if (!added && !chunk.isLoaded()) return false;
            } catch (Throwable throwable) {
                plugin.getLogger().warning(
                        "Could not retain remote container chunk "
                                + chunk.getWorld().getName() + " "
                                + chunk.getX() + "," + chunk.getZ()
                                + ": " + throwable.getMessage()
                );
                return false;
            }
        }

        chunkReferences.put(key, references + 1);
        playerSessions.put(player.getUniqueId(), key);
        return true;
    }

    void release(UUID playerId) {
        ChunkKey key = playerSessions.remove(playerId);
        if (key == null) return;

        int remaining = chunkReferences.getOrDefault(key, 0) - 1;
        if (remaining > 0) {
            chunkReferences.put(key, remaining);
            return;
        }

        chunkReferences.remove(key);
        World world = Bukkit.getWorld(key.worldId());
        if (world == null || !world.isChunkLoaded(key.x(), key.z())) return;

        try {
            world.getChunkAt(key.x(), key.z()).removePluginChunkTicket(plugin);
        } catch (Throwable throwable) {
            plugin.getLogger().warning(
                    "Could not release remote container chunk ticket in "
                            + world.getName() + " " + key.x() + "," + key.z()
                            + ": " + throwable.getMessage()
            );
        }
    }

    void shutdown() {
        UUID[] players = playerSessions.keySet().toArray(UUID[]::new);
        for (UUID playerId : players) release(playerId);
        playerSessions.clear();
        chunkReferences.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        release(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        release(event.getPlayer().getUniqueId());
    }
}
