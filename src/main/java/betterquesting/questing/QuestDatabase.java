package betterquesting.questing;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestDatabase;
import betterquesting.api.utils.NBTConverter;
import betterquesting.api.utils.UuidConverter;
import betterquesting.api2.storage.UuidDatabase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class QuestDatabase extends UuidDatabase<IQuest> implements IQuestDatabase
{
	public static final QuestDatabase INSTANCE = new QuestDatabase();

	@Override
	public synchronized IQuest createNew(UUID questID)
	{
	    IQuest quest = new QuestInstance();
	    put(questID, quest);
		return quest;
	}



    @Override
    public IQuest remove(Object key)
    {
        if (!(key instanceof UUID))
        {
            return null;
        }
        UUID questID = (UUID) key;

        IQuest removed = super.remove(questID);
        if (removed != null)
        {
            for (IQuest quest : values())
            {
                removeReq(quest, questID);
            }
        }
        return removed;
    }
    
    @Override
    public UUID removeValue(IQuest value)
    {
        UUID questID = super.removeValue(value);
        if (questID != null)
        {
            for (IQuest quest : values())
            {
                    removeReq(quest, questID);

            }
        }
        return questID;
    }
    
    private void removeReq(IQuest quest, UUID questID)
    {
        quest.getRequirements().remove(questID);
    }

    @Override
	public synchronized NBTTagList writeToNBT(NBTTagList nbt, @Nullable List<UUID> subset)
	{
		orderedEntries().forEach(entry ->
		{
            if (subset != null && !subset.contains(entry.getKey()))
            {
                return;
            }
			NBTTagCompound jq = new NBTTagCompound();
			entry.getValue().writeToNBT(jq);
            NBTConverter.UuidValueType.QUEST.writeId(entry.getKey(), jq);
			nbt.appendTag(jq);
		});
		
		return nbt;
	}
	
	@Override
	public synchronized void readFromNBT(NBTTagList nbt, boolean merge)
	{
		if (!merge)
        {
            clear();
        }

		for (int i = 0; i < nbt.tagCount(); i++)
		{
			NBTTagCompound qTag = nbt.getCompoundTagAt(i);

            Optional<UUID> questIDOptional = NBTConverter.UuidValueType.QUEST.tryReadId(qTag);
            UUID questID;
            if (questIDOptional.isPresent())
            {
                questID = questIDOptional.get();
            }
            else if (qTag.hasKey("questID", 99))
            {
                // This block is needed for old questbook data.
                questID = UuidConverter.convertLegacyId(qTag.getInteger("questID"));
            }
            else
            {
                continue;
            }

			IQuest quest = get(questID);
			quest = quest != null ? quest : createNew(questID);
			quest.readFromNBT(qTag);
		}
	}
	
	@Override
	public synchronized NBTTagList writeProgressToNBT(NBTTagList json, List<UUID> users)
	{
		for (Map.Entry<UUID, IQuest> entry : entrySet())
		{
			NBTTagCompound jq = entry.getValue().writeProgressToNBT(new NBTTagCompound(), users);
            NBTConverter.UuidValueType.QUEST.writeId(entry.getKey(), jq);
			json.appendTag(jq);
		}
		
		return json;
	}
	
	@Override
	public synchronized void readProgressFromNBT(NBTTagList json, boolean merge)
	{
		for (int i = 0; i < json.tagCount(); i++)
		{
			NBTTagCompound qTag = json.getCompoundTagAt(i);

            Optional<UUID> questIDOptional = NBTConverter.UuidValueType.QUEST.tryReadId(qTag);
            UUID questID = null;
            if (questIDOptional.isPresent())
            {
                questID = questIDOptional.get();
            }
            else if (qTag.hasKey("questID", 99))
            {
                // This block is needed for old player progress data.
                questID = UuidConverter.convertLegacyId(qTag.getInteger("questID"));
            }

            if (questID == null)
            {
                // Quest was deleted
                continue;
            }

			IQuest quest = get(questID);
			if (quest != null)
            {
                quest.readProgressFromNBT(qTag, merge);
            }
		}
	}

    public void readBookmarksFromNBT(NBTTagCompound nbt) {
        UUID uuid;
        try{
            uuid = UUID.fromString(nbt.getString("uuid"));
        }catch (IllegalArgumentException ignored){
            return;
        }
        NBTTagList json = nbt.getTagList("quests", 10);


        for (int i = 0; i < json.tagCount(); i++)
        {
            NBTTagCompound qTag = json.getCompoundTagAt(i);

            Optional<UUID> questIDOptional = NBTConverter.UuidValueType.QUEST.tryReadId(qTag);
            UUID questID = null;
            if (questIDOptional.isPresent())
            {
                questID = questIDOptional.get();
            }
            else if (qTag.hasKey("questID", 99))
            {
                // This block is needed for old player progress data.
                questID = UuidConverter.convertLegacyId(qTag.getInteger("questID"));
            }

            if (questID == null)
            {
                // Quest was deleted
                continue;
            }

            IQuest quest = get(questID);
            if (quest != null)
            {
                quest.setBookmarked(uuid, true);
            }
        }
    }


    public NBTTagCompound writeBookmarksToNBT(UUID user) {
        NBTTagCompound json = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, IQuest> entry : entrySet())
        {
            if (entry.getValue().isBookmarked(user)){
                NBTTagCompound jq = new NBTTagCompound();
                NBTConverter.UuidValueType.QUEST.writeId(entry.getKey(), jq);
                list.appendTag(jq);
            }
        }
        json.setString("uuid", user.toString());
        json.setTag("quests", list);

        return json;
    }
}
