package bq_standard.tasks;

import bq_standard.DirtyPlayerMarker;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ProgressUtil {

    public static void setComplete(UUID uuid, Set<UUID> completeUsers) {
        if (!completeUsers.contains(uuid)) {
            completeUsers.add(uuid);
            DirtyPlayerMarker.markDirty(uuid);
        }
    }

    public static <P> void setUserProgress(UUID uuid, Map<UUID, P> userProgress, P progress) {
        userProgress.put(uuid, progress);
        DirtyPlayerMarker.markDirty(uuid);
    }

    public static <P> void resetUser(@Nullable UUID uuid, Set<UUID> completeUsers, Map<UUID, P> userProgress) {
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

    public static void resetUser(@Nullable UUID uuid, Set<UUID> completeUsers) {
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

