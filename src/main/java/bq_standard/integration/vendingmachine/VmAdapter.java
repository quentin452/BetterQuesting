package bq_standard.integration.vendingmachine;

import java.util.List;
import java.util.UUID;

import com.cubefury.vendingmachine.integration.betterquesting.BqAdapter;

public class VmAdapter {

    public static void addBqConditions(UUID player, List<UUID> completedQuests) {
        for (UUID quest : completedQuests) {
            BqAdapter.INSTANCE.setQuestFinished(player, quest);
        }
    }

    public static void resetCompletedQuests(UUID player) {
        BqAdapter.INSTANCE.resetQuests(player);
    }

    public static void sendPlayerSatisfiedCache() {
        BqAdapter.INSTANCE.sendPlayerSatisfiedCache();
    }

}
