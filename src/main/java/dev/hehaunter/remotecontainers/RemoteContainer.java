package dev.hehaunter.remotecontainers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Instant;
import java.util.UUID;

record RemoteContainer(
        UUID ownerId,
        String ownerName,
        String normalizedName,
        String displayName,
        UUID worldId,
        String worldName,
        int x,
        int y,
        int z,
        String material,
        long createdEpochSeconds
) {

    Location location() {
        World world = Bukkit.getWorld(worldId);
        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }
        return world == null ? null : new Location(world, x, y, z);
    }

    String createdDisplay() {
        return Instant.ofEpochSecond(createdEpochSeconds).toString();
    }

    String locationKey() {
        return worldId + ":" + x + ":" + y + ":" + z;
    }
}
