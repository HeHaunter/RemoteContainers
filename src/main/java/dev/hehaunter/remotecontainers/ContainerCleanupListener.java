package dev.hehaunter.remotecontainers;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Map;

final class ContainerCleanupListener implements Listener {

    private final ContainerRepository repository;
    private final MessageManager messages;

    ContainerCleanupListener(
            RemoteContainersPlugin plugin,
            ContainerRepository repository,
            MessageManager messages
    ) {
        this.repository = repository;
        this.messages = messages;
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onBreak(BlockBreakEvent event) {
        removeForBlock(
                event.getBlock(),
                "cleanup.broken",
                "{prefix}&eRemote link &f{name} &ewas removed because its container was broken."
        );
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onBlockExplosion(
            BlockExplodeEvent event
    ) {
        for (Block block :
                List.copyOf(event.blockList())) {
            removeForBlock(
                    block,
                    "cleanup.exploded",
                    "{prefix}&eRemote link &f{name} &ewas removed because its container was destroyed."
            );
        }
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void onEntityExplosion(
            EntityExplodeEvent event
    ) {
        for (Block block :
                List.copyOf(event.blockList())) {
            removeForBlock(
                    block,
                    "cleanup.exploded",
                    "{prefix}&eRemote link &f{name} &ewas removed because its container was destroyed."
            );
        }
    }

    private void removeForBlock(
            Block block,
            String messagePath,
            String fallback
    ) {
        String key =
                block.getWorld()
                        .getUID()
                        + ":"
                        + block.getX()
                        + ":"
                        + block.getY()
                        + ":"
                        + block.getZ();

        List<RemoteContainer> removed = repository.removeAllByLocation(key);

        if (removed.isEmpty()) return;

        for (RemoteContainer record : removed) {
            Player owner =
                    Bukkit.getPlayer(
                            record.ownerId()
                    );

            if (owner != null) {
                owner.sendMessage(messages.message(
                        messagePath,
                        fallback,
                        Map.of(
                                "name",
                                record.displayName()
                        )
                ));
            }
        }
    }
}
