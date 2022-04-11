package bq_standard.tasks.base;

import betterquesting.api2.utils.DirtyPlayerMarker;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.UUID;

public abstract class TaskProgressableBase<T> extends TaskBase {
    protected final TreeMap<UUID, T> userProgress = new TreeMap<>();

    protected void setUserProgress(UUID uuid, T progress) {
        userProgress.put(uuid, progress);
        DirtyPlayerMarker.markDirty(uuid);
    }

    public abstract T getUsersProgress(UUID uuid);

    @Override
    public void resetUser(@Nullable UUID uuid) {
        HashSet<UUID> dirtyPlayers = new HashSet<>();
        if (uuid == null) {
            dirtyPlayers.addAll(completeUsers);
            dirtyPlayers.addAll(userProgress.keySet());
            completeUsers.clear();
            userProgress.clear();
        } else {
            if (completeUsers.remove(uuid)) {
                dirtyPlayers.add(uuid);
            }
            if (userProgress.containsKey(uuid)) {
                userProgress.remove(uuid);
                dirtyPlayers.add(uuid);
            }
        }
        DirtyPlayerMarker.markDirty(dirtyPlayers);
    }
}
