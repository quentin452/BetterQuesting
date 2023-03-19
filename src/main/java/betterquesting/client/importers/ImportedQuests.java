package betterquesting.client.importers;

import betterquesting.questing.QuestDatabase;
import net.minecraft.nbt.NBTTagList;

import java.util.List;
import java.util.UUID;

public class ImportedQuests extends QuestDatabase
{

	@Override
    public NBTTagList writeProgressToNBT(NBTTagList nbt, List<UUID> users)
    {
        return nbt;
    }
    
    @Override
    public void readProgressFromNBT(NBTTagList nbt, boolean merge)
    {
    }
}
