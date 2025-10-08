package betterquesting.loaders.dsl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.item.Item;

public class DslValidator {

    private static final Set<String> VALID_TASK_TYPES = new HashSet<>(
        Arrays.asList(
            "craft",
            "collect",
            "kill",
            "visit",
            "meeting",
            "checkbox",
            "interact",
            "scoreboard",
            "hunt",
            "fluid",
            "location",
            "blockbreak",
            "xp",
            "detection"));

    private static final Set<String> VALID_REWARD_TYPES = new HashSet<>(
        Arrays.asList("item", "choice", "xp", "command", "scoreboard", "quest"));

    private static final Set<String> VALID_LOGIC_TYPES = new HashSet<>(
        Arrays.asList("AND", "OR", "NAND", "NOR", "XOR", "XNOR"));

    private static final Set<String> VALID_METADATA_KEYS = new HashSet<>(
        Arrays.asList(
            "mod",
            "filename",
            "size",
            "category",
            "quest_line",
            "status",
            "layout",
            "spacing_x",
            "spacing_y",
            "base_x",
            "base_y"));

    private static final Set<String> VALID_QUEST_PROPERTIES = new HashSet<>(
        Arrays.asList(
            "x",
            "y",
            "title",
            "desc",
            "requires",
            "logic",
            "task",
            "reward",
            "repeatable",
            "auto_claim",
            "visibility",
            "simultaneous",
            "main"));

    private DslErrorCollector errorCollector;
    private String currentFile;

    private Map<String, java.util.List<PrerequisiteReference>> prerequisiteReferences = new HashMap<>();

    private static class PrerequisiteReference {

        String referenceString;
        UUID prereqId;

        PrerequisiteReference(String referenceString, UUID prereqId) {
            this.referenceString = referenceString;
            this.prereqId = prereqId;
        }
    }

    public DslValidator(DslErrorCollector errorCollector, String fileName) {
        this.errorCollector = errorCollector;
        this.currentFile = fileName;
    }

    public void addError(DslError.Severity severity, int lineNumber, String message, String context) {
        errorCollector.addError(severity, currentFile, lineNumber, message, context);

        if (severity == DslError.Severity.ERROR) {
            sendChatError(message + " in " + currentFile + " at line " + lineNumber);
        }
    }

    public boolean validateDimensionId(int dimId, int lineNumber, String context) {
        if (dimId == 0 && context != null && context.contains("failed")) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Invalid dimension ID - must be a number (0=Overworld, -1=Nether, 1=End, etc.)",
                context);
            return false;
        }
        return true;
    }

    public boolean validateTaskType(String taskType, int lineNumber, String context) {
        if (taskType == null || taskType.isEmpty()) {
            errorCollector.addError(DslError.Severity.ERROR, currentFile, lineNumber, "Empty task type", context);
            sendChatError("Empty task type in " + currentFile + " at line " + lineNumber);
            return false;
        }

        if (!VALID_TASK_TYPES.contains(taskType.toLowerCase())) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Unknown task type: '" + taskType + "'. Valid types: " + VALID_TASK_TYPES,
                context);
            sendChatError("Unknown task type '" + taskType + "' in " + currentFile + " at line " + lineNumber);
            return false;
        }

        return true;
    }

    public boolean validateRewardType(String rewardType, int lineNumber, String context) {
        if (rewardType == null || rewardType.isEmpty()) {
            errorCollector.addError(DslError.Severity.ERROR, currentFile, lineNumber, "Empty reward type", context);
            return false;
        }

        if (!VALID_REWARD_TYPES.contains(rewardType.toLowerCase())) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Unknown reward type: '" + rewardType + "'. Valid types: " + VALID_REWARD_TYPES,
                context);
            return false;
        }

        return true;
    }

    public boolean validateLogicType(String logic, int lineNumber, String context) {
        if (logic != null && !VALID_LOGIC_TYPES.contains(logic.toUpperCase())) {
            errorCollector.addError(
                DslError.Severity.WARNING,
                currentFile,
                lineNumber,
                "Unknown logic type: '" + logic + "'. Valid types: " + VALID_LOGIC_TYPES + ". Defaulting to AND.",
                context);
            return false;
        }
        return true;
    }

    public boolean validateMetadataKey(String key, int lineNumber) {
        if (!VALID_METADATA_KEYS.contains(key.toLowerCase())) {
            errorCollector.addError(
                DslError.Severity.WARNING,
                currentFile,
                lineNumber,
                "Unknown metadata key: '@" + key + "'. Valid keys: " + VALID_METADATA_KEYS,
                null);
            return false;
        }
        return true;
    }

    public boolean validateQuestProperty(String property, int lineNumber, String context) {
        if (!VALID_QUEST_PROPERTIES.contains(property.toLowerCase())) {
            errorCollector.addError(
                DslError.Severity.WARNING,
                currentFile,
                lineNumber,
                "Unknown quest property: '" + property + "'. Valid properties: " + VALID_QUEST_PROPERTIES,
                context);
            return false;
        }
        return true;
    }

    public boolean validateItemId(String itemId, int lineNumber, String context) {
        if (itemId == null || itemId.isEmpty()) {
            errorCollector.addError(DslError.Severity.ERROR, currentFile, lineNumber, "Empty item ID", context);
            sendChatError("Empty item ID in " + currentFile + " at line " + lineNumber);
            return false;
        }

        Item item = (Item) Item.itemRegistry.getObject(itemId);
        if (item == null) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Item not found: '" + itemId + "'. Use /bq_export items to get valid item IDs.",
                context);
            sendChatError("Invalid item ID '" + itemId + "' in " + currentFile + " at line " + lineNumber);
            return false;
        }

        return true;
    }

    private void sendChatError(String message) {
        try {
            net.minecraft.server.MinecraftServer server = net.minecraft.server.MinecraftServer.getServer();
            if (server != null) {
                String[] ops = server.getConfigurationManager()
                    .func_152606_n();
                for (String opName : ops) {
                    net.minecraft.command.ICommandSender sender = server.getConfigurationManager()
                        .func_152612_a(opName);
                    if (sender != null) {
                        sender.addChatMessage(new net.minecraft.util.ChatComponentText("§c[BQ DSL] " + message));
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public boolean validateQuestId(String questId, int lineNumber) {
        if (questId == null || questId.isEmpty()) {
            errorCollector.addError(DslError.Severity.ERROR, currentFile, lineNumber, "Empty quest ID", null);
            return false;
        }

        if (!questId.matches("[a-zA-Z0-9_]+")) {
            errorCollector.addError(
                DslError.Severity.WARNING,
                currentFile,
                lineNumber,
                "Quest ID contains special characters: '" + questId
                    + "'. Recommended format: lowercase_with_underscores",
                null);
            return false;
        }

        return true;
    }

    public boolean validateNumberParameter(String value, String paramName, int lineNumber, String context) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Invalid number for " + paramName + ": '" + value + "'",
                context);
            return false;
        }
    }

    public boolean validateRequiredParameter(String value, String paramName, int lineNumber, String context) {
        if (value == null || value.trim()
            .isEmpty()) {
            errorCollector.addError(
                DslError.Severity.ERROR,
                currentFile,
                lineNumber,
                "Missing required parameter: " + paramName,
                context);
            sendChatError("Missing " + paramName + " in " + currentFile + " at line " + lineNumber);
            return false;
        }
        return true;
    }

    public void addPrerequisiteReference(String questName, String referenceString, UUID prereqId) {
        if (!prerequisiteReferences.containsKey(questName)) {
            prerequisiteReferences.put(questName, new java.util.ArrayList<>());
        }
        prerequisiteReferences.get(questName)
            .add(new PrerequisiteReference(referenceString, prereqId));
    }

    public void validateAllPrerequisites(betterquesting.questing.QuestDatabase questDB,
        java.util.Set<java.util.UUID> validDslQuestIds) {
        for (Map.Entry<String, java.util.List<PrerequisiteReference>> entry : prerequisiteReferences.entrySet()) {
            String questName = entry.getKey();

            for (PrerequisiteReference ref : entry.getValue()) {
                betterquesting.api.questing.IQuest prereq = questDB.get(ref.prereqId);

                if (prereq == null) {
                    errorCollector.addError(
                        DslError.Severity.ERROR,
                        currentFile,
                        0,
                        "Quest '" + questName + "' references non-existent prerequisite: '" + ref.referenceString
                            + "'",
                        "Check that the prerequisite quest ID is correct and the quest exists");
                    sendChatError("Quest '" + questName + "' has invalid prerequisite: '" + ref.referenceString + "'");
                } else if (validDslQuestIds != null && !validDslQuestIds.contains(ref.prereqId)) {
                    errorCollector.addError(
                        DslError.Severity.WARNING,
                        currentFile,
                        0,
                        "Quest '" + questName + "' references prerequisite '" + ref.referenceString
                            + "' which exists in database but not in current DSL files",
                        "This prerequisite may have been removed from DSL files. Consider updating the dependency or restoring the quest.");
                    sendChatError(
                        "Quest '" + questName + "' references removed DSL quest: '" + ref.referenceString + "'");
                }
            }
        }
    }

    public void validateAllPrerequisites(betterquesting.questing.QuestDatabase questDB) {
        validateAllPrerequisites(questDB, null);
    }
}
