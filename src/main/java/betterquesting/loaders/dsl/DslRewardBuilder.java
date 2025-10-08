package betterquesting.loaders.dsl;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.rewards.IReward;
import betterquesting.api.utils.BigItemStack;
import bq_standard.rewards.*;

public class DslRewardBuilder {

    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private DslValidator validator;
    private int currentLine;

    public DslRewardBuilder() {
        this.validator = null; 
    }

    public DslRewardBuilder(DslValidator validator) {
        this.validator = validator;
    }

    public void setContext(int lineNumber) {
        this.currentLine = lineNumber;
    }

    public void buildReward(String rewardDef, IQuest quest) {
        try {
            String[] parts = rewardDef.split("\\s+");
            if (parts.length == 0) {
                return;
            }

            String rewardType = parts[0].toLowerCase();
            IReward reward = null;

            switch (rewardType) {
                case "xp":
                    reward = buildXPReward(rewardDef);
                    break;
                case "item":
                    reward = buildItemReward(rewardDef);
                    break;
                case "command":
                    reward = buildCommandReward(rewardDef);
                    break;
                case "scoreboard":
                    reward = buildScoreboardReward(rewardDef);
                    break;
                case "questcompletion":
                    reward = buildQuestCompletionReward(rewardDef);
                    break;
                default:
                    System.out.println("[BQ] WARNING: Unknown reward type: " + rewardType);
                    return;
            }

            if (reward != null) {
                int rewardId = quest.getRewards()
                    .nextID();
                quest.getRewards()
                    .add(rewardId, reward);
            }
        } catch (Exception e) {
            System.err.println("[BQ] Error building reward: " + rewardDef);
            e.printStackTrace();
        }
    }

    public void buildChoiceReward(List<String> choices, IQuest quest) {
        try {
            RewardChoice reward = new RewardChoice();

            for (String choice : choices) {
                choice = choice.trim();
                String[] parts = choice.split("\\s+");

                if (parts.length < 2) {
                    if (validator != null) {
                        validator.addError(
                            DslError.Severity.ERROR,
                            currentLine,
                            "Choice reward item requires item ID and count: \"item_id\" count",
                            choice);
                    }
                    continue;
                }

                String itemId;
                if (choice.contains("\"")) {
                    itemId = extractQuoted(choice);
                } else {
                    itemId = parts[0];
                }

                if (validator != null && !validator.validateItemId(itemId, currentLine, choice)) {
                    continue;
                }

                int count = Integer.parseInt(parts[parts.length - 1]);

                ItemStack stack = getItemStack(itemId, count);
                if (stack != null) {
                    reward.choices.add(new BigItemStack(stack));
                }
            }

            if (!reward.choices.isEmpty()) {
                int rewardId = quest.getRewards()
                    .nextID();
                quest.getRewards()
                    .add(rewardId, reward);
            }
        } catch (Exception e) {
            System.err.println("[BQ] Error building choice reward");
            e.printStackTrace();
        }
    }

    private IReward buildXPReward(String rewardDef) {
        String[] parts = rewardDef.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        int amount = Integer.parseInt(parts[1]);

        RewardXP reward = new RewardXP();
        reward.amount = amount;
        reward.levels = false;

        return reward;
    }

    private IReward buildItemReward(String rewardDef) {
        String[] parts = rewardDef.split("\\s+");
        if (parts.length < 3) {
            if (validator != null) {
                validator.addError(
                    DslError.Severity.ERROR,
                    currentLine,
                    "Item reward requires item ID and count: item \"item_id\" count",
                    rewardDef);
            }
            return null;
        }

        String itemId = parts[1];
        if (itemId.startsWith("\"") && itemId.endsWith("\"")) {
            itemId = itemId.substring(1, itemId.length() - 1);
        }

        if (validator != null && !validator.validateItemId(itemId, currentLine, rewardDef)) {
            return null;
        }

        int count = 1;

        if (parts.length > 2) {
            try {
                count = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                if (validator != null) {
                    validator
                        .addError(DslError.Severity.ERROR, currentLine, "Invalid count value: " + parts[2], rewardDef);
                }
                return null;
            }
        }

        RewardItem reward = new RewardItem();
        ItemStack stack = getItemStack(itemId, count);
        if (stack != null) {
            reward.items.add(new BigItemStack(stack));
        }

        return reward;
    }

    private IReward buildCommandReward(String rewardDef) {
        String command = extractQuoted(rewardDef);

        RewardCommand reward = new RewardCommand();
        reward.command = command;

        return reward;
    }

    private IReward buildScoreboardReward(String rewardDef) {
        String[] parts = rewardDef.split("\\s+");
        if (parts.length < 3) {
            return null;
        }

        String objective = parts[1];
        int score = Integer.parseInt(parts[2]);

        RewardScoreboard reward = new RewardScoreboard();
        reward.score = objective;
        reward.value = score;

        return reward;
    }

    private IReward buildQuestCompletionReward(String rewardDef) {
        String questId = extractQuoted(rewardDef);

        RewardQuestCompletion reward = new RewardQuestCompletion();
        reward.questNum = UUID.nameUUIDFromBytes(questId.getBytes());

        return reward;
    }

    private String extractQuoted(String text) {
        Matcher matcher = QUOTED_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private ItemStack getItemStack(String itemId, int count) {
        try {
            Item item = (Item) Item.itemRegistry.getObject(itemId);
            if (item != null) {
                return new ItemStack(item, count, 0);
            }
        } catch (Exception e) {
            System.out.println("[BQ] WARNING: Could not find item: " + itemId);
        }
        return null;
    }
}
