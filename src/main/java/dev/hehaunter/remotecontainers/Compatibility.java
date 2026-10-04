package dev.hehaunter.remotecontainers;

import org.bukkit.Bukkit;

final class Compatibility {

    private Compatibility() {
    }

    static String minecraftVersion() {
        String version = Bukkit.getBukkitVersion();
        if (version == null || version.isBlank()) {
            return "unknown";
        }

        int separator = version.indexOf('-');
        return separator > 0 ? version.substring(0, separator) : version;
    }

    static boolean isAdvertisedSupportedVersion() {
        String version = minecraftVersion();
        return version.equals("1.21")
                || version.startsWith("1.21.")
                || version.equals("26.1")
                || version.startsWith("26.1.")
                || version.equals("26.2")
                || version.startsWith("26.2.");
    }
}
