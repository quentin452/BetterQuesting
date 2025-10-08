package betterquesting.loaders.dsl;

import java.util.UUID;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import betterquesting.api.enums.EnumLogic;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api.utils.BigItemStack;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestInstance;
import betterquesting.questing.QuestLine;
import betterquesting.questing.QuestLineDatabase;
import betterquesting.questing.QuestLineEntry;

public class DslQuestBuilder {

    private final DslValidator validator;
    private final DslTaskBuilder taskBuilder;
    private final DslRewardBuilder rewardBuilder;
    private final DslLayoutCalculator layoutCalculator;

    public DslQuestBuilder(DslValidator validator, DslLayoutCalculator layoutCalculator) {
        this.validator = validator;
        this.taskBuilder = new DslTaskBuilder(validator);
        this.rewardBuilder = new DslRewardBuilder(validator);
        this.layoutCalculator = layoutCalculator;
    }

    public void buildQuests(DslQuestData dslData, QuestDatabase questDB, QuestLineDatabase lineDB, String questLineName,
        boolean addToQuestLine) {
        try {
            if (addToQuestLine) {
                UUID lineId = getOrCreateQuestLine(questLineName, dslData, lineDB);
                QuestLine questLine = (QuestLine) lineDB.get(lineId);

                if (questLine == null) {
                    System.err.println("[BQ] Failed to create quest line: " + questLineName);
                    return;
                }

                int totalQuests = dslData.quests.size();
                for (int i = 0; i < totalQuests; i++) {
                    DslQuest dslQuest = dslData.quests.get(i);
                    try {
                        buildQuest(dslQuest, questDB, questLine, lineId, i, totalQuests, questLineName);
                    } catch (Exception e) {
                        System.err.println("[BQ] Error building quest: " + dslQuest.id);
                        e.printStackTrace();
                    }
                }
            } else {
                for (DslQuest dslQuest : dslData.quests) {
                    try {
                        buildQuestWithoutLine(dslQuest, questDB);
                    } catch (Exception e) {
                        System.err.println("[BQ] Error building quest: " + dslQuest.id);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[BQ] Error building quests from DSL");
            e.printStackTrace();
        }
    }

    private void buildQuest(DslQuest dslQuest, QuestDatabase questDB, QuestLine questLine, UUID lineId, int questIndex,
        int totalQuests, String questLineName) {
        int lineNumber = dslQuest.lineNumber > 0 ? dslQuest.lineNumber : 0;

        String title = dslQuest.getProperty("title");
        if (!validator.validateRequiredParameter(title, "title", lineNumber, "Quest: " + dslQuest.id)) {
            title = "Untitled Quest (" + dslQuest.id + ")";
        }

        String taskDef = dslQuest.getProperty("task");
        if (!validator.validateRequiredParameter(taskDef, "task", lineNumber, "Quest: " + dslQuest.id)) {
            return;
        }

        UUID questId = generateQuestId(dslQuest.id, lineId);

        IQuest quest = questDB.get(questId);
        boolean isNewQuest = (quest == null);
        
        if (isNewQuest) {
            quest = new QuestInstance();
        }

        // Always update quest properties for both new and existing quests
        buildQuestProperties(dslQuest, quest);

        quest.setProperty(DslProps.DSL_SOURCE, true);

        // Clear existing tasks and rewards, then rebuild them from DSL
        quest.getTasks().reset();
        quest.getRewards().reset();
        
        if (taskDef != null) {
            taskBuilder.buildTask(taskDef, quest, lineNumber);
        }

        String rewardDef = dslQuest.getProperty("reward");
        rewardBuilder.setContext(lineNumber);
        if (rewardDef != null) {
            if ("choice".equals(rewardDef)) {
                rewardBuilder.buildChoiceReward(dslQuest.choiceRewards, quest);
            } else {
                rewardBuilder.buildReward(rewardDef, quest);
            }
        } else {
            validator.addError(
                DslError.Severity.WARNING,
                lineNumber,
                "Quest has no reward defined",
                "Quest: " + dslQuest.id);
        }

        String requires = dslQuest.getProperty("requires");
        if (requires != null && !"none".equals(requires)) {
            if (requires.trim()
                .isEmpty()) {
                validator.addError(
                    DslError.Severity.ERROR,
                    lineNumber,
                    "Empty 'requires' value - use 'none' if no prerequisites needed",
                    "Quest: " + dslQuest.id);
            } else {
                buildRequirements(requires, quest, lineId, questDB, questLine, dslQuest);
            }
        }

        DslQuestLoader.registerDslQuest(questId);

        if (isNewQuest) {
            questDB.put(questId, quest);
        }

        Integer manualX = dslQuest.hasProperty("x") ? dslQuest.getIntProperty("x", 0) : null;
        Integer manualY = dslQuest.hasProperty("y") ? dslQuest.getIntProperty("y", 0) : null;
        int[] pos = layoutCalculator.calculatePosition(questIndex, totalQuests, manualX, manualY, questLineName);
        questLine.put(questId, new QuestLineEntry(pos[0], pos[1]));
    }

    private void buildQuestWithoutLine(DslQuest dslQuest, QuestDatabase questDB) {
        int lineNumber = dslQuest.lineNumber > 0 ? dslQuest.lineNumber : 0;

        String title = dslQuest.getProperty("title");
        if (!validator.validateRequiredParameter(title, "title", lineNumber, "Quest: " + dslQuest.id)) {
            title = "Untitled Quest (" + dslQuest.id + ")";
        }

        String taskDef = dslQuest.getProperty("task");
        if (!validator.validateRequiredParameter(taskDef, "task", lineNumber, "Quest: " + dslQuest.id)) {
            return;
        }

        UUID questId = generateQuestId(dslQuest.id, UUID.nameUUIDFromBytes("NO_QUESTLINE".getBytes()));

        IQuest quest = questDB.get(questId);
        boolean isNewQuest = (quest == null);
        
        if (isNewQuest) {
            quest = new QuestInstance();
            System.out.println("[BQ DSL] Creating new quest: " + dslQuest.id + " (UUID: " + questId + ")");
        }

        // Always update quest properties for both new and existing quests
        buildQuestProperties(dslQuest, quest);

        quest.setProperty(DslProps.DSL_SOURCE, true);

        // Clear existing tasks and rewards, then rebuild them from DSL
        quest.getTasks().reset();
        quest.getRewards().reset();

        if (taskDef != null) {
            taskBuilder.buildTask(taskDef, quest, lineNumber);
        }

        String rewardDef = dslQuest.getProperty("reward");
        rewardBuilder.setContext(lineNumber);
        if (rewardDef != null) {
            if ("choice".equals(rewardDef)) {
                rewardBuilder.buildChoiceReward(dslQuest.choiceRewards, quest);
            } else {
                rewardBuilder.buildReward(rewardDef, quest);
            }
        } else {
            validator.addError(
                DslError.Severity.WARNING,
                lineNumber,
                "Quest has no reward defined",
                "Quest: " + dslQuest.id);
        }

        String requires = dslQuest.getProperty("requires");
        if (requires != null && !"none".equals(requires)) {
            validator.addError(
                DslError.Severity.WARNING,
                lineNumber,
                "Quest has prerequisites but is not in a quest line - prerequisites may not work correctly",
                "Quest: " + dslQuest.id);
        }

        DslQuestLoader.registerDslQuest(questId);

        if (isNewQuest) {
            questDB.put(questId, quest);
        }
    }

    private void buildQuestProperties(DslQuest dslQuest, IQuest quest) {
        String title = dslQuest.getProperty("title");
        if (title != null) {
            quest.setProperty(NativeProps.NAME, title);
        }

        String desc = dslQuest.getProperty("desc");
        if (desc != null) {
            quest.setProperty(NativeProps.DESC, desc);
        }

        String icon = dslQuest.getProperty("icon");
        if (icon != null && !icon.trim().isEmpty()) {
            parseAndSetIcon(icon, quest, dslQuest.lineNumber);
        }

        String bgImage = dslQuest.getProperty("bg_image");
        if (bgImage != null && !bgImage.trim().isEmpty()) {
            quest.setProperty(NativeProps.BG_IMAGE, bgImage);
        }

        if (dslQuest.hasProperty("bg_size")) {
            int bgSize = dslQuest.getIntProperty("bg_size", 256);
            quest.setProperty(NativeProps.BG_SIZE, bgSize);
        }

        boolean repeatable = dslQuest.getBooleanProperty("repeatable", false);
        if (repeatable) {
            quest.setProperty(NativeProps.REPEAT_TIME, 0);
        }

        boolean autoClaim = dslQuest.getBooleanProperty("auto_claim", false);
        quest.setProperty(NativeProps.AUTO_CLAIM, autoClaim);

        // Parse visibility/show property
        String show = dslQuest.getProperty("show");
        if (show != null && !show.trim().isEmpty()) {
            parseAndSetVisibility(show.trim().toUpperCase(), quest, dslQuest.lineNumber);
        }
    }

    private void parseAndSetVisibility(String visibility, IQuest quest, int lineNumber) {
        try {
            betterquesting.api.enums.EnumQuestVisibility vis = betterquesting.api.enums.EnumQuestVisibility
                .valueOf(visibility);
            quest.setProperty(NativeProps.VISIBILITY, vis);
        } catch (IllegalArgumentException e) {
            validator.addError(
                DslError.Severity.ERROR,
                lineNumber,
                "Invalid visibility value '" + visibility
                    + "'. Valid values: NORMAL, COMPLETED, CHAIN, ALWAYS, HIDDEN, SECRET, UNLOCKED",
                "show: " + visibility);
        }
    }

    private void parseAndSetIcon(String iconDef, IQuest quest, int lineNumber) {
        String[] parts = iconDef.trim().split("\\s+");
        if (parts.length == 0) return;

        String itemId = parts[0];
        int count = 1;
        int meta = 0;

        if (parts.length >= 2) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                validator.addError(
                    DslError.Severity.WARNING,
                    lineNumber,
                    "Invalid icon count '" + parts[1] + "', using 1",
                    "icon: " + iconDef);
            }
        }

        if (parts.length >= 3) {
            try {
                meta = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                validator.addError(
                    DslError.Severity.WARNING,
                    lineNumber,
                    "Invalid icon metadata '" + parts[2] + "', using 0",
                    "icon: " + iconDef);
            }
        }

        try {
            Item item = (Item) Item.itemRegistry.getObject(itemId);
            if (item != null) {
                ItemStack stack = new ItemStack(item, count, meta);
                quest.setProperty(NativeProps.ICON, new BigItemStack(stack));
                System.out.println("[BQ DSL] Successfully set icon: " + itemId + " count=" + count + " meta=" + meta);
            } else {
                System.err.println("[BQ DSL] Failed to find item: " + itemId);
                validator.addError(
                    DslError.Severity.ERROR,
                    lineNumber,
                    "Unknown item ID: " + itemId,
                    "icon: " + iconDef);
            }
        } catch (Exception e) {
            System.err.println("[BQ DSL] Exception while parsing icon: " + iconDef);
            e.printStackTrace();
            validator.addError(
                DslError.Severity.ERROR,
                lineNumber,
                "Failed to parse icon: " + e.getMessage(),
                "icon: " + iconDef);
        }
    }


    private void setQuestLogic(DslQuest dslQuest, IQuest quest) {
        String logic = dslQuest.getProperty("logic", "AND");
        try {
            EnumLogic logicType = EnumLogic.valueOf(logic.toUpperCase());
            quest.setProperty(NativeProps.LOGIC_QUEST, logicType);
        } catch (IllegalArgumentException e) {
            System.out.println("[BQ] WARNING: Invalid logic type: " + logic + ", using AND");
            quest.setProperty(NativeProps.LOGIC_QUEST, EnumLogic.AND);
        }
    }

    private void buildRequirements(String requires, IQuest quest, UUID currentLineId, QuestDatabase questDB,
        QuestLine questLine, DslQuest dslQuest) {
        setQuestLogic(dslQuest, quest);

        String[] reqs = requires.split(",");
        for (String req : reqs) {
            req = req.trim();

            UUID prereqId = null;
            String originalReq = req;
            if (req.contains(":")) {
                String[] parts = req.split(":", 2);
                String questLineName = parts[0];
                String questName = parts[1];

                prereqId = findQuestByName(questLineName, questName, questDB);
            } else {
                prereqId = generateQuestId(req, currentLineId);
            }

            if (prereqId != null) {
                quest.getRequirements().add(prereqId);

                String questName = quest.getProperty(betterquesting.api.properties.NativeProps.NAME, dslQuest.id);
                validator.addPrerequisiteReference(questName, originalReq, prereqId);
            } else {
                validator.addError(
                    DslError.Severity.ERROR,
                    dslQuest.lineNumber,
                    "Could not parse prerequisite quest reference: " + req,
                    "Quest ID: " + dslQuest.id);
            }
        }
    }

    private void validatePrerequisiteReferences(String requires, UUID currentLineId, DslQuest dslQuest) {
        String[] reqs = requires.split(",");
        for (String req : reqs) {
            req = req.trim();

            UUID prereqId = null;
            String originalReq = req;
            if (req.contains(":")) {
                String[] parts = req.split(":", 2);
                String questLineName = parts[0];
                String questName = parts[1];

                prereqId = findQuestByName(questLineName, questName, null);
            } else {
                prereqId = generateQuestId(req, currentLineId);
            }

            if (prereqId != null) {
                validator.addPrerequisiteReference(dslQuest.id, originalReq, prereqId);
            } else {
                validator.addError(
                    DslError.Severity.ERROR,
                    dslQuest.lineNumber,
                    "Could not parse prerequisite quest reference: " + req,
                    "Quest ID: " + dslQuest.id);
            }
        }
    }

    private UUID findQuestByName(String questLineName, String questName, QuestDatabase questDB) {
        return generateQuestId(questName, UUID.nameUUIDFromBytes(questLineName.getBytes()));
    }

    private UUID generateQuestId(String questName, UUID lineId) {
        String combined = lineId.toString() + ":" + questName;
        return UUID.nameUUIDFromBytes(combined.getBytes());
    }

    private UUID getOrCreateQuestLine(String name, DslQuestData dslData, QuestLineDatabase lineDB) {
        UUID lineId = UUID.nameUUIDFromBytes(name.getBytes());

        IQuestLine existingLine = lineDB.get(lineId);
        if (existingLine != null) {
            existingLine.setProperty(DslProps.DSL_SOURCE, true);
            DslQuestLoader.registerDslQuestLine(lineId);
            return lineId;
        }

        QuestLine newLine = new QuestLine();
        newLine.setProperty(NativeProps.NAME, name);

        newLine.setProperty(DslProps.DSL_SOURCE, true);

        String desc = dslData.getMetadata("mod", "");
        if (!desc.isEmpty()) {
            newLine.setProperty(NativeProps.DESC, "Quests for " + desc);
        }

        lineDB.put(lineId, newLine);

        DslQuestLoader.registerDslQuestLine(lineId);

        return lineId;
    }

    @Deprecated
    @SuppressWarnings("unused")
    private void preserveQuestProgress(IQuest oldQuest, IQuest newQuest) {
        if (oldQuest == null || newQuest == null) {
            return;
        }

        if (oldQuest instanceof QuestInstance && newQuest instanceof QuestInstance) {
            QuestInstance oldQI = (QuestInstance) oldQuest;
            QuestInstance newQI = (QuestInstance) newQuest;

            java.util.Set<UUID> usersWithData = new java.util.HashSet<>();
            oldQI.getUsersWithCompletionData(usersWithData);

            for (UUID userId : usersWithData) {
                net.minecraft.nbt.NBTTagCompound completionInfo = oldQI.getCompletionInfo(userId);
                if (completionInfo != null) {
                    newQI.setCompletionInfo(userId, completionInfo);
                }
            }

            net.minecraft.nbt.NBTTagCompound progressNBT = new net.minecraft.nbt.NBTTagCompound();
            oldQuest.writeProgressToNBT(progressNBT, null); // null = all users

            newQuest.readProgressFromNBT(progressNBT, true);
        }
    }
}
