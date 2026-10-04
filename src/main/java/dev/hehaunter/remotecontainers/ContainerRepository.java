package dev.hehaunter.remotecontainers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

final class ContainerRepository {

    private final RemoteContainersPlugin plugin;
    private final File file;

    private final Map<UUID, LinkedHashMap<String, RemoteContainer>>
            byOwner = new LinkedHashMap<>();

    private final Map<String, List<RemoteContainer>>
            byLocation = new HashMap<>();

    ContainerRepository(RemoteContainersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(
                plugin.getDataFolder(),
                "containers.yml"
        );
    }

    synchronized void load() {
        byOwner.clear();
        byLocation.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection root =
                yaml.getConfigurationSection(
                        "containers"
                );

        if (root == null) return;

        for (String rawOwner :
                root.getKeys(false)) {
            UUID ownerId;

            try {
                ownerId = UUID.fromString(rawOwner);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning(
                        "Skipping invalid owner UUID in containers.yml: "
                                + rawOwner
                );
                continue;
            }

            ConfigurationSection ownerSection = root.getConfigurationSection(rawOwner);

            if (ownerSection == null) continue;

            for (String normalizedName :
                    ownerSection.getKeys(false)) {
                ConfigurationSection section =
                        ownerSection.getConfigurationSection(
                                normalizedName
                        );

                if (section == null) continue;

                try {
                    UUID worldId = UUID.fromString(
                            section.getString(
                                    "world-uuid",
                                    ""
                            )
                    );

                    RemoteContainer record =
                            new RemoteContainer(
                                    ownerId,
                                    section.getString(
                                            "owner-name",
                                            "Unknown"
                                    ),
                                    normalizedName,
                                    section.getString(
                                            "display-name",
                                            normalizedName
                                    ),
                                    worldId,
                                    section.getString(
                                            "world-name",
                                            ""
                                    ),
                                    section.getInt("x"),
                                    section.getInt("y"),
                                    section.getInt("z"),
                                    section.getString(
                                            "material",
                                            "CHEST"
                                    ),
                                    section.getLong(
                                            "created",
                                            Instant.now()
                                                    .getEpochSecond()
                                    )
                            );

                    putInternal(record);

                } catch (Exception exception) {
                    plugin.getLogger().log(
                            Level.WARNING,
                            "Could not load container "
                                    + rawOwner
                                    + "/"
                                    + normalizedName,
                            exception
                    );
                }
            }
        }
    }

    synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("data-version", 1);

        for (Map.Entry<UUID, LinkedHashMap<String, RemoteContainer>> ownerEntry :
                byOwner.entrySet()) {

            for (RemoteContainer record :
                    ownerEntry.getValue().values()) {

                String path =
                        "containers."
                                + ownerEntry.getKey()
                                + "."
                                + record.normalizedName();

                yaml.set(
                        path + ".display-name",
                        record.displayName()
                );
                yaml.set(
                        path + ".owner-name",
                        record.ownerName()
                );
                yaml.set(
                        path + ".world-uuid",
                        record.worldId().toString()
                );
                yaml.set(
                        path + ".world-name",
                        record.worldName()
                );
                yaml.set(
                        path + ".x",
                        record.x()
                );
                yaml.set(
                        path + ".y",
                        record.y()
                );
                yaml.set(
                        path + ".z",
                        record.z()
                );
                yaml.set(
                        path + ".material",
                        record.material()
                );
                yaml.set(
                        path + ".created",
                        record.createdEpochSeconds()
                );
            }
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not save containers.yml",
                    exception
            );
        }
    }

    synchronized int size() {
        return byOwner.values()
                .stream()
                .mapToInt(Map::size)
                .sum();
    }

    synchronized int count(UUID ownerId) {
        return byOwner
                .getOrDefault(
                        ownerId,
                        new LinkedHashMap<>()
                )
                .size();
    }

    synchronized RemoteContainer get(
            UUID ownerId,
            String normalizedName
    ) {
        return byOwner
                .getOrDefault(
                        ownerId,
                        new LinkedHashMap<>()
                )
                .get(normalizedName);
    }

    synchronized List<RemoteContainer> list(
            UUID ownerId
    ) {
        return List.copyOf(
                byOwner
                        .getOrDefault(
                                ownerId,
                                new LinkedHashMap<>()
                        )
                        .values()
        );
    }

    synchronized Collection<RemoteContainer> all() {
        List<RemoteContainer> result = new ArrayList<>();

        for (Map<String, RemoteContainer> entries :
                byOwner.values()) {
            result.addAll(entries.values());
        }

        return List.copyOf(result);
    }

    synchronized List<RemoteContainer> byLocation(
            String locationKey
    ) {
        return List.copyOf(
                byLocation.getOrDefault(
                        locationKey,
                        List.of()
                )
        );
    }

    synchronized void put(RemoteContainer record) {
        putInternal(record);
        save();
    }

    private void putInternal(RemoteContainer record) {
        byOwner.computeIfAbsent(
                record.ownerId(),
                ignored -> new LinkedHashMap<>()
        ).put(
                record.normalizedName(),
                record
        );

        byLocation.computeIfAbsent(
                record.locationKey(),
                ignored -> new ArrayList<>()
        ).add(record);
    }

    synchronized RemoteContainer remove(
            UUID ownerId,
            String normalizedName
    ) {
        LinkedHashMap<String, RemoteContainer> entries = byOwner.get(ownerId);

        if (entries == null) return null;

        RemoteContainer removed = entries.remove(normalizedName);

        if (removed != null) {
            List<RemoteContainer> locationEntries =
                    byLocation.get(
                            removed.locationKey()
                    );

            if (locationEntries != null) {
                locationEntries.removeIf(
                        entry ->
                                entry.ownerId().equals(ownerId)
                                        && entry.normalizedName()
                                        .equals(normalizedName)
                );

                if (locationEntries.isEmpty()) {
                    byLocation.remove(
                            removed.locationKey()
                    );
                }
            }
        }

        if (entries.isEmpty()) {
            byOwner.remove(ownerId);
        }

        save();
        return removed;
    }

    synchronized List<RemoteContainer> removeAllByLocation(
            String locationKey
    ) {
        List<RemoteContainer> removed =
                new ArrayList<>(
                        byLocation.getOrDefault(
                                locationKey,
                                List.of()
                        )
                );

        if (removed.isEmpty()) {
            return List.of();
        }

        byLocation.remove(locationKey);

        for (RemoteContainer record : removed) {
            LinkedHashMap<String, RemoteContainer> entries = byOwner.get(record.ownerId());

            if (entries == null) continue;

            entries.remove(
                    record.normalizedName()
            );

            if (entries.isEmpty()) {
                byOwner.remove(record.ownerId());
            }
        }

        save();
        return List.copyOf(removed);
    }

    synchronized boolean rename(
            UUID ownerId,
            String oldNormalizedName,
            String newNormalizedName,
            String newDisplayName
    ) {
        LinkedHashMap<String, RemoteContainer> entries = byOwner.get(ownerId);

        if (entries == null
                || !entries.containsKey(oldNormalizedName)
                || entries.containsKey(newNormalizedName)) {
            return false;
        }

        RemoteContainer old = entries.remove(oldNormalizedName);

        RemoteContainer renamed =
                new RemoteContainer(
                        old.ownerId(),
                        old.ownerName(),
                        newNormalizedName,
                        newDisplayName,
                        old.worldId(),
                        old.worldName(),
                        old.x(),
                        old.y(),
                        old.z(),
                        old.material(),
                        old.createdEpochSeconds()
                );

        entries.put(
                newNormalizedName,
                renamed
        );

        List<RemoteContainer> locationEntries =
                byLocation.computeIfAbsent(
                        renamed.locationKey(),
                        ignored -> new ArrayList<>()
                );

        locationEntries.removeIf(
                entry ->
                        entry.ownerId().equals(ownerId)
                                && entry.normalizedName()
                                .equals(oldNormalizedName)
        );

        locationEntries.add(renamed);

        save();
        return true;
    }
}
