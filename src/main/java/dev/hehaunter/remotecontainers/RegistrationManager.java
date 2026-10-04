package dev.hehaunter.remotecontainers;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class RegistrationManager implements Listener {

    private record Pending(
            String displayName,
            String normalizedName,
            String locationKey,
            long expiresAtMillis
    ) {}

    private final RemoteContainersPlugin plugin;
    private final ContainerRepository repository;
    private final ContainerService service;
    private final MessageManager messages;

    private final Map<UUID, Pending> pending = new LinkedHashMap<>();

    RegistrationManager(
            RemoteContainersPlugin plugin,
            ContainerRepository repository,
            ContainerService service,
            MessageManager messages
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.service = service;
        this.messages = messages;
    }

    void begin(
            Player player,
            String displayName,
            Block block
    ) {
        if (!plugin.getConfig().getBoolean("require-physical-confirmation", true)) {
            register(
                    player,
                    displayName,
                    block
            );
            return;
        }

        int timeoutSeconds = Math.max(
                5,
                plugin.getConfig().getInt("confirmation-timeout-seconds", 30)
        );

        pending.put(
                player.getUniqueId(),
                new Pending(
                        displayName.trim(),
                        service.normalizeName(displayName),
                        service.locationKey(block),
                        System.currentTimeMillis()
                                + timeoutSeconds * 1000L
                )
        );

        player.sendMessage(messages.message(
                "create.confirm",
                "{prefix}&eRight-click the selected container within &f{seconds}s &eto confirm registration.",
                Map.of(
                        "name",
                        displayName.trim(),
                        "seconds",
                        String.valueOf(timeoutSeconds)
                )
        ));
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onInteract(
            PlayerInteractEvent event
    ) {
        if (event.getAction()
                != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Pending registration =
                pending.get(
                        player.getUniqueId()
                );

        if (registration == null) return;

        if (System.currentTimeMillis()
                > registration.expiresAtMillis()) {
            pending.remove(
                    player.getUniqueId()
            );

            player.sendMessage(messages.message(
                    "create.confirmation-expired",
                    "{prefix}&cContainer registration expired. Run /rc create <name> again.",
                    Map.of()
            ));
            return;
        }

        Block block = event.getClickedBlock();

        if (!service.locationKey(block)
                .equals(registration.locationKey())) {
            return;
        }

        pending.remove(
                player.getUniqueId()
        );

        register(
                player,
                registration.displayName(),
                block
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(
                event.getPlayer().getUniqueId()
        );
    }

    private void register(
            Player player,
            String displayName,
            Block block
    ) {
        String normalized =
                service.normalizeName(
                        displayName
                );

        if (!service.isSupported(block)) {
            player.sendMessage(messages.message(
                    "create.unsupported",
                    "{prefix}&cThat block is not a supported container.",
                    Map.of()
            ));
            return;
        }

        if (repository.get(
                player.getUniqueId(),
                normalized
        ) != null) {
            player.sendMessage(messages.message(
                    "create.duplicate-name",
                    "{prefix}&cYou already have a remote container named &f{name}&c.",
                    Map.of(
                            "name",
                            displayName
                    )
            ));
            return;
        }

        int limit =
                plugin.getConfig().getInt("max-containers-per-player", 10);

        if (limit > 0
                && !player.hasPermission("remotecontainers.limit.bypass")
                && repository.count(
                        player.getUniqueId()
                ) >= limit) {
            player.sendMessage(messages.message(
                    "create.limit-reached",
                    "{prefix}&cYou reached your remote container limit of &f{limit}&c.",
                    Map.of(
                            "limit",
                            String.valueOf(limit)
                    )
            ));
            return;
        }

        if (!plugin.getConfig().getBoolean("allow-shared-links", false) && !repository.byLocation(
                service.locationKey(block)
        ).isEmpty()) {
            player.sendMessage(messages.message(
                    "create.already-linked",
                    "{prefix}&cThat physical container is already registered.",
                    Map.of()
            ));
            return;
        }

        RemoteContainer record =
                service.createRecord(
                        player,
                        displayName,
                        block
                );

        repository.put(record);

        Map<String, String> placeholders = new LinkedHashMap<>();

        placeholders.put(
                "name",
                record.displayName()
        );
        placeholders.put(
                "world",
                record.worldName()
        );
        placeholders.put(
                "x",
                String.valueOf(record.x())
        );
        placeholders.put(
                "y",
                String.valueOf(record.y())
        );
        placeholders.put(
                "z",
                String.valueOf(record.z())
        );

        player.sendMessage(messages.message(
                "create.success",
                "{prefix}&aRegistered &f{name}&a.",
                placeholders
        ));
    }
}
