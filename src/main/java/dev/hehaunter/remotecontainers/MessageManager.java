package dev.hehaunter.remotecontainers;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MessageManager {

    private static final String DEFAULT_PREFIX = "&8[&6RemoteContainers&8] ";

    private final RemoteContainersPlugin plugin;
    private final File file;
    private YamlConfiguration messages;

    MessageManager(RemoteContainersPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    void reload() {
        messages = YamlConfiguration.loadConfiguration(file);
    }

    String message(String path, String fallback, Map<String, String> placeholders) {
        return format(messages.getString(path, fallback), placeholders);
    }

    List<String> lines(String path, Map<String, String> placeholders) {
        return messages.getStringList(path).stream()
                .map(line -> format(line, placeholders))
                .toList();
    }

    private String format(String text, Map<String, String> placeholders) {
        Map<String, String> values = new LinkedHashMap<>(placeholders);
        values.putIfAbsent("prefix", messages.getString("prefix", DEFAULT_PREFIX));
        return plugin.color(plugin.replace(text, values));
    }
}
