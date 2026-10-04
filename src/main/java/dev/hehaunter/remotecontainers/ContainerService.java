package dev.hehaunter.remotecontainers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class ContainerService {

    record OpenResult(
            boolean success,
            String errorKey,
            String fallback,
            Map<String, String> placeholders
    ) {
        static OpenResult ok() {
            return new OpenResult(
                    true,
                    "",
                    "",
                    Map.of()
            );
        }
    }

    private final RemoteContainersPlugin plugin;
    private final ContainerRepository repository;
    private final MessageManager messages;
    private final RemoteInventorySessionManager remoteSessions;

    private final Map<UUID, Long> lastOpenMillis = new HashMap<>();

    ContainerService(
            RemoteContainersPlugin plugin,
            ContainerRepository repository,
            MessageManager messages,
            RemoteInventorySessionManager remoteSessions
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.messages = messages;
        this.remoteSessions = remoteSessions;
    }

    String normalizeName(String input) {
        return input == null
                ? ""
                : input.trim()
                .toLowerCase(Locale.ROOT);
    }

    boolean validName(String input) {
        if (input == null) return false;

        int min = Math.max(
                1,
                plugin.getConfig().getInt("names.min-length", 1)
        );

        int max = Math.max(
                min,
                plugin.getConfig().getInt("names.max-length", 32)
        );

        String trimmed = input.trim();

        if (trimmed.length() < min
                || trimmed.length() > max) {
            return false;
        }

        String regex = plugin.getConfig().getString(
                "names.allowed-regex",
                "^[A-Za-z0-9_-]+$"
        );

        try {
            return Pattern.matches(
                    regex,
                    trimmed
            );
        } catch (PatternSyntaxException exception) {
            plugin.getLogger().warning(
                    "Invalid names.allowed-regex; using safe default."
            );

            return Pattern.matches(
                    "^[A-Za-z0-9_-]+$",
                    trimmed
            );
        }
    }

    Set<Material> supportedMaterials() {
        Set<Material> result = EnumSet.noneOf(Material.class);

        for (String raw :
                plugin.getConfig().getStringList(
                        "supported-materials"
                )) {
            Material material = Material.matchMaterial(raw);

            if (material != null) {
                result.add(material);
            }
        }

        return result;
    }

    boolean isSupported(Block block) {
        if (block == null) return false;

        if (!supportedMaterials().contains(
                block.getType()
        )) {
            return false;
        }

        return block.getState()
                instanceof InventoryHolder;
    }

    Block targetContainer(Player player) {
        int distance = Math.max(
                1,
                plugin.getConfig().getInt("create-target-distance", 6)
        );

        Block block = player.getTargetBlockExact(distance);

        return isSupported(block)
                ? block
                : null;
    }

    String locationKey(Block block) {
        return block.getWorld()
                .getUID()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();
    }

    String locationKey(Location location) {
        if (location == null
                || location.getWorld() == null) {
            return "";
        }

        return location.getWorld()
                .getUID()
                + ":"
                + location.getBlockX()
                + ":"
                + location.getBlockY()
                + ":"
                + location.getBlockZ();
    }

    RemoteContainer createRecord(
            Player owner,
            String displayName,
            Block block
    ) {
        return new RemoteContainer(
                owner.getUniqueId(),
                owner.getName(),
                normalizeName(displayName),
                displayName.trim(),
                block.getWorld().getUID(),
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getType().name(),
                Instant.now().getEpochSecond()
        );
    }

    private boolean sameRegisteredContainerType(
            String registeredMaterial,
            Material currentMaterial
    ) {
        if (registeredMaterial == null
                || currentMaterial == null) {
            return false;
        }

        String currentName = currentMaterial.name();

        if (registeredMaterial.equals(currentName)) {
            return true;
        }

        // Copper Chest states can change without the physical container changing.
        return isCopperChestMaterialName(registeredMaterial)
                && isCopperChestMaterialName(currentName);
    }

    private boolean isCopperChestMaterialName(
            String materialName
    ) {
        return switch (materialName) {
            case "COPPER_CHEST",
                 "EXPOSED_COPPER_CHEST",
                 "WEATHERED_COPPER_CHEST",
                 "OXIDIZED_COPPER_CHEST",
                 "WAXED_COPPER_CHEST",
                 "WAXED_EXPOSED_COPPER_CHEST",
                 "WAXED_WEATHERED_COPPER_CHEST",
                 "WAXED_OXIDIZED_COPPER_CHEST" -> true;
            default -> false;
        };
    }

    OpenResult open(
            Player player,
            RemoteContainer record
    ) {
        int cooldownSeconds = Math.max(
                0,
                plugin.getConfig().getInt("open-cooldown-seconds", 1)
        );

        if (cooldownSeconds > 0) {
            long now = System.currentTimeMillis();

            long previous = lastOpenMillis.getOrDefault(
                    player.getUniqueId(),
                    0L
            );

            long waitMillis =
                    cooldownSeconds * 1000L
                            - (now - previous);

            if (waitMillis > 0L) {
                long seconds =
                        Math.max(
                                1L,
                                (long) Math.ceil(
                                        waitMillis / 1000.0
                                )
                        );

                return new OpenResult(
                        false,
                        "open.cooldown",
                        "{prefix}&cPlease wait &f{seconds}s &cbefore opening another remote container.",
                        Map.of(
                                "seconds",
                                String.valueOf(seconds)
                        )
                );
            }
        }

        Location location = record.location();

        if (location == null
                || location.getWorld() == null) {
            return new OpenResult(
                    false,
                    "open.world-unavailable",
                    "{prefix}&cThe container's world is not currently available.",
                    Map.of()
            );
        }

        Chunk chunk = location.getChunk();

        if (!chunk.isLoaded()) {
            if (!plugin.getConfig().getBoolean("load-unloaded-chunks", true)) {
                return new OpenResult(
                        false,
                        "open.chunk-unloaded",
                        "{prefix}&cThat container's chunk is unloaded.",
                        Map.of()
                );
            }

            if (!chunk.load()) {
                return new OpenResult(
                        false,
                        "open.chunk-load-failed",
                        "{prefix}&cCould not load that container's chunk.",
                        Map.of()
                );
            }
        }

        Block block = location.getBlock();

        if (!sameRegisteredContainerType(
                record.material(),
                block.getType()
        ) || !isSupported(block)) {

            repository.remove(
                    record.ownerId(),
                    record.normalizedName()
            );

            return new OpenResult(
                    false,
                    "open.stale",
                    "{prefix}&cThe registered container no longer exists. The stale link was removed.",
                    Map.of()
            );
        }

        BlockState state = block.getState();

        if (!(state instanceof InventoryHolder holder)) {
            repository.remove(
                    record.ownerId(),
                    record.normalizedName()
            );

            return new OpenResult(
                    false,
                    "open.stale",
                    "{prefix}&cThe registered container no longer exists. The stale link was removed.",
                    Map.of()
            );
        }

        Inventory inventory = holder.getInventory();

        // Keep distant container chunks loaded until the remote inventory closes.
        if (!remoteSessions.acquire(player, chunk)) {
            return new OpenResult(
                    false,
                    "open.chunk-retain-failed",
                    "{prefix}&cCould not keep that container's chunk loaded.",
                    Map.of()
            );
        }

        player.openInventory(inventory);

        lastOpenMillis.put(
                player.getUniqueId(),
                System.currentTimeMillis()
        );

        return OpenResult.ok();
    }
}
