# Compatibility

RemoteContainers v1.0.0 is designed as **one JAR** for Bukkit-based servers
from Minecraft 1.21 through 26.2.

## Supported server software

| Minecraft version | CraftBukkit | Spigot | Paper | Required Java |
|---|---:|---:|---:|---:|
| 1.21.x | Yes | Yes | Yes | Java 21+ |
| 26.1 / 26.1.x | Yes | Yes | Yes | Java 25+ |
| 26.2 / 26.2.x | Yes | Yes | Yes | Java 25+ |

### About "Bukkit"

Bukkit is primarily the API/project. The server implementation produced by
Spigot BuildTools is **CraftBukkit**. Therefore when RemoteContainers is listed
as supporting "Bukkit", that means CraftBukkit / Bukkit-compatible servers.

## Why one JAR can support all three

RemoteContainers deliberately:

- uses the Bukkit/Spigot public API;
- does not use NMS;
- does not import `org.bukkit.craftbukkit.*`;
- does not depend on Paper-only APIs;
- resolves configurable block materials by name;
- targets Java 21 bytecode;
- keeps `api-version: '1.21'`.

Paper and Spigot implement the Bukkit API, while CraftBukkit is the reference
Bukkit-based server implementation.

Java 25 can run Java 21 class files, so the same plugin JAR can run on 26.1 and
26.2 servers even though those server versions themselves require Java 25.

## API compile strategy

The default Maven build intentionally compiles against:

```text
org.spigotmc:spigot-api:1.21-R0.1-SNAPSHOT
```

That is the **oldest advertised Minecraft API version** in the supported
range. This helps guarantee that ordinary builds do not accidentally use a
method/class introduced only in a later 1.21.x update.

The project also includes Maven profiles for newer API compile checks:

```text
mvn -Papi-26.1 clean package
mvn -Papi-26.2 clean package
```

Use JDK 25 for the 26.x checks.

These profiles are compile-time compatibility checks; they do not replace
actual runtime testing on each server implementation.

## plugin.yml API version

RemoteContainers intentionally declares:

```yaml
api-version: '1.21'
```

Do not change this to `26.2` for the shared release JAR. Declaring a newer API
version would prevent older servers in the supported range from loading the
same artifact.

## Recommended release testing

Before publishing a release as fully tested, run the same JAR on at least:

- CraftBukkit 1.21.x
- Spigot 1.21.x
- Paper 1.21.x
- CraftBukkit 26.2
- Spigot 26.2
- Paper 26.2

26.1 should also be smoke-tested if practical, especially because the Java
runtime requirement changed to Java 25 in that generation.

At minimum test:

```text
/rc create <name>
physical registration confirmation
/rc open <name>
/rc list
/rc info <name>
/rc rename <old> <new>
/rc delete <name>
container break cleanup
explosion cleanup
unloaded-chunk opening
restart persistence
```

## Runtime diagnostics

At startup RemoteContainers reports the detected Minecraft version.

Advertised Minecraft lines:
- `1.21.x`
- `26.1.x`
- `26.2.x`

Unknown future versions are not blocked; the plugin only logs a warning by
default.

Disable that warning with:

```yaml
warn-on-untested-version: false
```


## Copper Chest compatibility

Copper Chests are supported on Minecraft versions that provide them.

RemoteContainers includes all Copper Chest material names in its default
configuration, but resolves them dynamically. Older servers where those
materials do not exist simply ignore those entries.

Supported Copper Chest states:

```text
COPPER_CHEST
EXPOSED_COPPER_CHEST
WEATHERED_COPPER_CHEST
OXIDIZED_COPPER_CHEST
WAXED_COPPER_CHEST
WAXED_EXPOSED_COPPER_CHEST
WAXED_WEATHERED_COPPER_CHEST
WAXED_OXIDIZED_COPPER_CHEST
```

The stale-link check treats all of these as one family so normal oxidation,
waxing, scraping, or unwaxing does not delete a valid remote link.
