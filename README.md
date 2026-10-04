# RemoteContainers

A lightweight Minecraft plugin for **Bukkit, Spigot, and Paper** that lets players register **real in-world containers** and open them remotely from anywhere.

## Features

- Register real containers with custom names
- Open registered containers remotely from anywhere
- Protection-friendly physical confirmation before registration
- Supports chests, barrels, shulker boxes, furnaces, hoppers, Copper Chests, and more
- Rename and delete remote links
- List registered containers
- Admin access to other players' links
- Per-player registration limits
- Configurable supported container types
- Multi-world support
- Optional loading of unloaded chunks
- Keeps distant container chunks loaded only while their remote inventory is open
- Automatic stale-link cleanup
- Automatic cleanup when registered containers are broken or destroyed
- Full tab completion
- Fully configurable messages
- No database required
- No external dependencies

## Commands

```text
/rc create <name>
/rc open <name>
/rc delete <name>
/rc rename <old> <new>
/rc list
/rc info <name>
/rc help
```

### Admin commands

```text
/rc open <player> <name>
/rc delete <player> <name>
/rc info <player> <name>
/rc list <player>
/rc reload
```

### Aliases

```text
/rc
/remotecontainers
/remotecontainer
/rcontainer
```

## How to Use

Look directly at a supported container and run:

```text
/rc create storage
```

By default, RemoteContainers will ask you to right-click that same container once to confirm the registration.

After that, you can open it remotely from anywhere:

```text
/rc open storage
```

RemoteContainers opens the **actual inventory of the real block**. It does not create a virtual copy.

If the container is in a distant or unloaded chunk, RemoteContainers can load the chunk and keep it loaded only while the remote inventory is open. The chunk ticket is released again when the inventory closes.

## Default Supported Containers

- Chest
- Trapped Chest
- Copper Chest
- Exposed Copper Chest
- Weathered Copper Chest
- Oxidized Copper Chest
- All waxed Copper Chest variants
- Barrel
- Hopper
- Dispenser
- Dropper
- Furnace
- Blast Furnace
- Smoker
- Brewing Stand
- Crafter
- All Shulker Box colors

The list is configurable in `config.yml`.

### Copper Chests

Copper Chests are supported on Minecraft versions that include them.

RemoteContainers resolves container materials by name, so Copper Chest entries in the default config are safely ignored on older versions where those blocks do not exist.

A registered Copper Chest also remains linked if it:

- oxidizes naturally
- is waxed
- is scraped with an axe
- has its wax removed

All Copper Chest oxidation and wax states are treated as the same physical container family.

## Permissions

```text
remotecontainers.use
remotecontainers.create
remotecontainers.open
remotecontainers.delete
remotecontainers.rename
remotecontainers.list
remotecontainers.info
remotecontainers.others
remotecontainers.reload
remotecontainers.limit.bypass
```

Basic permissions default to everyone.

Administrative permissions default to operators.

## Configuration

Important defaults:

```yaml
max-containers-per-player: 10
create-target-distance: 6

require-physical-confirmation: true
confirmation-timeout-seconds: 30

load-unloaded-chunks: true
allow-shared-links: false
open-cooldown-seconds: 1
```

Use:

```yaml
max-containers-per-player: 0
```

for unlimited registered containers.

Messages can be customized in `messages.yml`.

## Data

Remote container links are stored in:

```text
plugins/RemoteContainers/containers.yml
```

Each record stores:

- owner UUID
- owner name
- link name
- world UUID and name
- X / Y / Z coordinates
- registered block material
- creation timestamp

RemoteContainers stores the **location of the real container**, not a copy of its inventory.

## Compatibility

One JAR is designed for the following range:

| Minecraft | CraftBukkit | Spigot | Paper | Java |
|---|---:|---:|---:|---:|
| **1.21.x** | Yes | Yes | Yes | 21+ |
| **26.1 / 26.1.x** | Yes | Yes | Yes | 25+ |
| **26.2 / 26.2.x** | Yes | Yes | Yes | 25+ |

### Runtime Tested

- ✅ Spigot 1.21.1
- ✅ Paper 1.21
- ✅ Spigot 26.2
- ✅ Paper 26.2

Other versions in the supported range are designed to work but have not all been individually runtime-tested.

See [`COMPATIBILITY.md`](COMPATIBILITY.md) for technical compatibility details.

## Build

```bash
mvn clean package
```

Output:

```text
target/remotecontainers-1.0.1.jar
```

## License

RemoteContainers is licensed under the **MIT License**.
