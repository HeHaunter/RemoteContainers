package dev.hehaunter.remotecontainers;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class RemoteContainersPlugin extends JavaPlugin {

    private MessageManager messages;
    private ContainerRepository repository;
    private RemoteInventorySessionManager remoteSessions;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messages = new MessageManager(this);
        repository = new ContainerRepository(this);
        repository.load();

        remoteSessions = new RemoteInventorySessionManager(
                this
        );

        ContainerService service = new ContainerService(
                this,
                repository,
                messages,
                remoteSessions
        );

        RegistrationManager registrations = new RegistrationManager(
                this,
                repository,
                service,
                messages
        );

        RemoteContainersCommand command = new RemoteContainersCommand(
                this,
                repository,
                service,
                registrations,
                messages
        );

        PluginCommand pluginCommand = getCommand("remotecontainers");

        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(
                remoteSessions,
                this
        );

        getServer().getPluginManager().registerEvents(
                registrations,
                this
        );

        getServer().getPluginManager().registerEvents(
                new ContainerCleanupListener(
                        this,
                        repository,
                        messages
                ),
                this
        );

        String minecraftVersion = Compatibility.minecraftVersion();

        getLogger().info(
                "RemoteContainers v1.0.1 enabled with "
                        + repository.size()
                        + " registered links."
        );

        if (Compatibility.isAdvertisedSupportedVersion()) {
            getLogger().info(
                    "Detected Minecraft "
                            + minecraftVersion
                            + " - supported target."
            );
        } else if (getConfig().getBoolean(
                "warn-on-untested-version",
                true
        )) {
            getLogger().warning(
                    "Detected Minecraft "
                            + minecraftVersion
                            + ". Official targets are CraftBukkit/Spigot/Paper "
                            + "1.21.x through 26.2. "
                            + "The plugin will still attempt to run."
            );
        }
    }

    @Override
    public void onDisable() {
        if (remoteSessions != null) {
            remoteSessions.shutdown();
        }

        if (repository != null) {
            repository.save();
        }
    }

    void reloadEverything() {
        reloadConfig();
        messages.reload();
        repository.load();
    }

    String color(String value) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                value == null ? "" : value
        );
    }


    String replace(
            String input,
            Map<String, String> values
    ) {
        String result = input == null ? "" : input;

        for (Map.Entry<String, String> entry :
                values.entrySet()) {
            result = result.replace(
                    "{" + entry.getKey() + "}",
                    entry.getValue()
            );
        }

        return result;
    }
}
