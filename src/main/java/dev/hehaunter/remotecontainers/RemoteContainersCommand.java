package dev.hehaunter.remotecontainers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

final class RemoteContainersCommand
        implements CommandExecutor, TabCompleter {

    private final RemoteContainersPlugin plugin;
    private final ContainerRepository repository;
    private final ContainerService service;
    private final RegistrationManager registrations;
    private final MessageManager messages;

    RemoteContainersCommand(
            RemoteContainersPlugin plugin,
            ContainerRepository repository,
            ContainerService service,
            RegistrationManager registrations,
            MessageManager messages
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.service = service;
        this.registrations = registrations;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);

        switch (action) {
            case "create" -> create(sender, args);
            case "open" -> open(sender, args);
            case "delete", "remove" -> delete(sender, args);
            case "rename" -> rename(sender, args);
            case "list" -> list(sender, args);
            case "info" -> info(sender, args);
            case "reload" -> reload(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void create(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.create")) {
            noPermission(player);
            return;
        }

        if (args.length < 2) {
            player.sendMessage(messages.message(
                    "create.usage",
                    "{prefix}&cUsage: /rc create <name>",
                    Map.of()
            ));
            return;
        }

        String displayName = args[1];

        if (!service.validName(displayName)) {
            int min = Math.max(
                    1,
                    plugin.getConfig().getInt("names.min-length", 1)
            );

            int max = Math.max(
                    min,
                    plugin.getConfig().getInt("names.max-length", 32)
            );

            player.sendMessage(messages.message(
                    "create.invalid-name",
                    "{prefix}&cInvalid name.",
                    Map.of(
                            "min",
                            String.valueOf(min),
                            "max",
                            String.valueOf(max)
                    )
            ));
            return;
        }

        String normalized = service.normalizeName(displayName);

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

        Block block = service.targetContainer(player);

        if (block == null) {
            player.sendMessage(messages.message(
                    "create.no-target",
                    "{prefix}&cLook directly at a supported container first.",
                    Map.of()
            ));
            return;
        }

        String locationKey = service.locationKey(block);

        if (!plugin.getConfig().getBoolean("allow-shared-links", false) && !repository.byLocation(
                locationKey
        ).isEmpty()) {
            player.sendMessage(messages.message(
                    "create.already-linked",
                    "{prefix}&cThat physical container is already registered.",
                    Map.of()
            ));
            return;
        }

        registrations.begin(
                player,
                displayName,
                block
        );
    }

    private void open(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.open")) {
            noPermission(player);
            return;
        }

        Target target =
                resolveTarget(
                        player,
                        args,
                        "open.usage",
                        "{prefix}&cUsage: /rc open <name>"
                );

        if (target == null) return;

        RemoteContainer record =
                repository.get(
                        target.ownerId(),
                        target.normalizedName()
                );

        if (record == null) {
            player.sendMessage(messages.message(
                    "open.not-found",
                    "{prefix}&cNo remote container named &f{name}&c was found.",
                    Map.of(
                            "name",
                            target.displayName()
                    )
            ));
            return;
        }

        ContainerService.OpenResult result =
                service.open(
                        player,
                        record
                );

        if (!result.success()) {
            player.sendMessage(messages.message(
                    result.errorKey(),
                    result.fallback(),
                    result.placeholders()
            ));
        }
    }

    private void delete(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.delete")) {
            noPermission(player);
            return;
        }

        Target target =
                resolveTarget(
                        player,
                        args,
                        "delete.usage",
                        "{prefix}&cUsage: /rc delete <name>"
                );

        if (target == null) return;

        RemoteContainer removed =
                repository.remove(
                        target.ownerId(),
                        target.normalizedName()
                );

        if (removed == null) {
            player.sendMessage(messages.message(
                    "delete.not-found",
                    "{prefix}&cNo remote container named &f{name}&c was found.",
                    Map.of(
                            "name",
                            target.displayName()
                    )
            ));
            return;
        }

        player.sendMessage(messages.message(
                "delete.success",
                "{prefix}&aDeleted remote link &f{name}&a.",
                Map.of(
                        "name",
                        removed.displayName()
                )
        ));
    }

    private void rename(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.rename")) {
            noPermission(player);
            return;
        }

        if (args.length < 3) {
            player.sendMessage(messages.message(
                    "rename.usage",
                    "{prefix}&cUsage: /rc rename <old> <new>",
                    Map.of()
            ));
            return;
        }

        String oldName = args[1];
        String newName = args[2];

        if (!service.validName(newName)) {
            int min = Math.max(
                    1,
                    plugin.getConfig().getInt("names.min-length", 1)
            );

            int max = Math.max(
                    min,
                    plugin.getConfig().getInt("names.max-length", 32)
            );

            player.sendMessage(messages.message(
                    "create.invalid-name",
                    "{prefix}&cInvalid name.",
                    Map.of(
                            "min",
                            String.valueOf(min),
                            "max",
                            String.valueOf(max)
                    )
            ));
            return;
        }

        String oldNormalized = service.normalizeName(oldName);

        String newNormalized = service.normalizeName(newName);

        if (repository.get(
                player.getUniqueId(),
                oldNormalized
        ) == null) {
            player.sendMessage(messages.message(
                    "rename.not-found",
                    "{prefix}&cNo remote container named &f{name}&c was found.",
                    Map.of(
                            "name",
                            oldName
                    )
            ));
            return;
        }

        if (repository.get(
                player.getUniqueId(),
                newNormalized
        ) != null) {
            player.sendMessage(messages.message(
                    "rename.duplicate-name",
                    "{prefix}&cYou already have a remote container named &f{name}&c.",
                    Map.of(
                            "name",
                            newName
                    )
            ));
            return;
        }

        repository.rename(
                player.getUniqueId(),
                oldNormalized,
                newNormalized,
                newName.trim()
        );

        player.sendMessage(messages.message(
                "rename.success",
                "{prefix}&aRenamed &f{old}&a to &f{new}&a.",
                Map.of(
                        "old",
                        oldName,
                        "new",
                        newName
                )
        ));
    }

    private void list(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.list")) {
            noPermission(player);
            return;
        }

        UUID ownerId = player.getUniqueId();
        String ownerName = player.getName();

        if (args.length >= 2) {
            if (!player.hasPermission("remotecontainers.others")) {
                noPermission(player);
                return;
            }

            OfflinePlayer owner = findPlayer(args[1]);

            if (owner == null) {
                player.sendMessage(messages.message(
                        "player-not-found",
                        "{prefix}&cPlayer not found: &f{player}",
                        Map.of(
                                "player",
                                args[1]
                        )
                ));
                return;
            }

            ownerId = owner.getUniqueId();
            ownerName = owner.getName() == null
                    ? args[1]
                    : owner.getName();
        }

        List<RemoteContainer> entries = repository.list(ownerId);

        if (entries.isEmpty()) {
            player.sendMessage(messages.message(
                    "list.empty",
                    "{prefix}&7No remote containers found.",
                    Map.of()
            ));
            return;
        }

        player.sendMessage(messages.message(
                "list.header",
                "{prefix}&6Remote containers for &f{player}&6: &f{count}",
                Map.of(
                        "player",
                        ownerName,
                        "count",
                        String.valueOf(entries.size())
                )
        ));

        for (RemoteContainer record : entries) {
            player.sendMessage(messages.message(
                    "list.entry",
                    "&7- &e{name}",
                    placeholders(record)
            ));
        }
    }

    private void info(
            CommandSender sender,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.message(
                    "player-only",
                    "{prefix}&cThis command can only be used in-game.",
                    Map.of()
            ));
            return;
        }

        if (!player.hasPermission("remotecontainers.info")) {
            noPermission(player);
            return;
        }

        Target target =
                resolveTarget(
                        player,
                        args,
                        "info.not-found",
                        "{prefix}&cUsage: /rc info <name>"
                );

        if (target == null) return;

        RemoteContainer record =
                repository.get(
                        target.ownerId(),
                        target.normalizedName()
                );

        if (record == null) {
            player.sendMessage(messages.message(
                    "info.not-found",
                    "{prefix}&cNo remote container named &f{name}&c was found.",
                    Map.of(
                            "name",
                            target.displayName()
                    )
            ));
            return;
        }

        for (String line :
                messages.lines(
                        "info.lines",
                        placeholders(record)
                )) {
            player.sendMessage(line);
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("remotecontainers.reload")) {
            noPermission(sender);
            return;
        }

        plugin.reloadEverything();

        sender.sendMessage(messages.message(
                "reloaded",
                "{prefix}&aRemoteContainers reloaded.",
                Map.of()
        ));
    }

    private void sendHelp(CommandSender sender) {
        if (!sender.hasPermission("remotecontainers.use")) {
            noPermission(sender);
            return;
        }

        for (String line :
                messages.lines(
                        "help",
                        Map.of()
                )) {
            sender.sendMessage(line);
        }
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(messages.message(
                "no-permission",
                "{prefix}&cYou do not have permission.",
                Map.of()
        ));
    }

    private record Target(
            UUID ownerId,
            String normalizedName,
            String displayName
    ) {}

    private Target resolveTarget(
            Player actor,
            String[] args,
            String usageKey,
            String usageFallback
    ) {
        if (args.length < 2) {
            actor.sendMessage(messages.message(
                    usageKey,
                    usageFallback,
                    Map.of()
            ));
            return null;
        }

        if (args.length >= 3
                && actor.hasPermission("remotecontainers.others")) {
            OfflinePlayer owner = findPlayer(args[1]);

            if (owner == null) {
                actor.sendMessage(messages.message(
                        "player-not-found",
                        "{prefix}&cPlayer not found: &f{player}",
                        Map.of(
                                "player",
                                args[1]
                        )
                ));
                return null;
            }

            return new Target(
                    owner.getUniqueId(),
                    service.normalizeName(args[2]),
                    args[2]
            );
        }

        return new Target(
                actor.getUniqueId(),
                service.normalizeName(args[1]),
                args[1]
        );
    }

    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);

        if (online != null) {
            return online;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);

        if (offline.hasPlayedBefore()
                || offline.isOnline()) {
            return offline;
        }

        return null;
    }

    private Map<String, String> placeholders(
            RemoteContainer record
    ) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("name", record.displayName());
        map.put("owner", record.ownerName());
        map.put("material", record.material());
        map.put("world", record.worldName());
        map.put("x", String.valueOf(record.x()));
        map.put("y", String.valueOf(record.y()));
        map.put("z", String.valueOf(record.z()));
        map.put(
                "created",
                record.createdDisplay()
        );

        return map;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            if (args.length == 1
                    && sender.hasPermission("remotecontainers.reload")) {
                return complete(
                        List.of(
                                "reload",
                                "help"
                        ),
                        args[0]
                );
            }

            return List.of();
        }

        if (args.length == 1) {
            List<String> options =
                    new ArrayList<>(
                            List.of(
                                    "create",
                                    "open",
                                    "delete",
                                    "rename",
                                    "list",
                                    "info",
                                    "help"
                            )
                    );

            if (player.hasPermission("remotecontainers.reload")) {
                options.add("reload");
            }

            return complete(
                    options,
                    args[0]
            );
        }

        String action =
                args[0].toLowerCase(
                        Locale.ROOT
                );

        if (args.length == 2) {
            if (action.equals("open")
                    || action.equals("delete")
                    || action.equals("rename")
                    || action.equals("info")) {
                List<String> names =
                        repository.list(
                                player.getUniqueId()
                        )
                        .stream()
                        .map(RemoteContainer::displayName)
                        .toList();

                return complete(
                        names,
                        args[1]
                );
            }

            if (action.equals("list")
                    && player.hasPermission("remotecontainers.others")) {
                return complete(
                        Bukkit.getOnlinePlayers()
                                .stream()
                                .map(Player::getName)
                                .toList(),
                        args[1]
                );
            }
        }

        if (args.length == 3
                && player.hasPermission("remotecontainers.others")
                && (action.equals("open")
                || action.equals("delete")
                || action.equals("info"))) {

            OfflinePlayer owner = findPlayer(args[1]);

            if (owner == null) {
                return List.of();
            }

            return complete(
                    repository.list(
                            owner.getUniqueId()
                    )
                    .stream()
                    .map(RemoteContainer::displayName)
                    .toList(),
                    args[2]
            );
        }

        return List.of();
    }

    private List<String> complete(
            Collection<String> options,
            String current
    ) {
        String needle =
                current.toLowerCase(
                        Locale.ROOT
                );

        return options.stream()
                .filter(Objects::nonNull)
                .filter(value ->
                        value.toLowerCase(
                                Locale.ROOT
                        ).startsWith(needle))
                .sorted(
                        String.CASE_INSENSITIVE_ORDER
                )
                .toList();
    }
}
