package bq_standard.tasks.base;

import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.utils.DirtyPlayerMarker;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public abstract class TaskBase implements ITask {
    protected final Set<UUID> completeUsers = new TreeSet<>();

    @Override
    public boolean isComplete(UUID uuid) {
        return completeUsers.contains(uuid);
    }

    @Override
    public void setComplete(UUID uuid) {
        if (!completeUsers.contains(uuid)) {
            completeUsers.add(uuid);
            DirtyPlayerMarker.markDirty(uuid);
        }
    }

    @Override
    public void resetUser(@Nullable UUID uuid) {
        HashSet<UUID> dirtyPlayers = new HashSet<>();
        if (uuid == null) {
            dirtyPlayers.addAll(completeUsers);
            completeUsers.clear();
        } else {
            if (completeUsers.remove(uuid)) {
                dirtyPlayers.add(uuid);
            }
        }
        DirtyPlayerMarker.markDirty(dirtyPlayers);
    }
}
