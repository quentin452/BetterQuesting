package betterquesting.misc;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import betterquesting.api2.storage.DBEntry;

import java.util.Map;
import java.util.UUID;

public class QuestSearchEntry {
    public QuestSearchEntry(Map.Entry<UUID, IQuest> quest, DBEntry<IQuestLine> questLineEntry) {
        this.quest = quest;
        this.questLineEntry = questLineEntry;
    }

    private Map.Entry<UUID, IQuest> quest;

    public Map.Entry<UUID, IQuest> getQuest() {
        return quest;
    }

    public void setQuest(Map.Entry<UUID, IQuest> quest) {
        this.quest = quest;
    }

    public DBEntry<IQuestLine> getQuestLineEntry() {
        return questLineEntry;
    }

    public void setQuestLineEntry(DBEntry<IQuestLine> questLineEntry) {
        this.questLineEntry = questLineEntry;
    }

    private DBEntry<IQuestLine> questLineEntry;
}
