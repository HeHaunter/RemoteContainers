# Changelog

## 1.0.1
- Fixed remote inventories closing when their container was very far from every player.
- Remote container chunks now receive a plugin chunk ticket while the remote inventory is open.
- Chunk tickets are reference-counted so multiple players can safely use containers in the same chunk.
- Tickets are released when the inventory closes, the player disconnects, or the plugin disables.

## 1.0.0
- Initial public release.
- Added explicit CraftBukkit, Spigot, and Paper support across 1.21.x, 26.1, and 26.2.
- Default compilation now targets Spigot API 1.21.0 to protect full 1.21.x compatibility.
- Added Maven API compile-check profiles for Spigot API 26.1 and 26.2.
- Added startup compatibility diagnostics for advertised/untested versions.
- Added named remote container registration.
- Added remote opening.
- Added delete, rename, list and info commands.
- Added admin access to other players' links.
- Added multi-world support.
- Added configurable container limits and supported block types.
- Added Copper Chest support for 1.21.9+ including every oxidation and waxed variant.
- Copper Chest links survive oxidation, waxing, scraping, and wax removal.
- Added optional chunk loading.
- Added stale-link cleanup.
- Added cleanup for broken/exploded containers.
- Added tab completion and configurable messages.
