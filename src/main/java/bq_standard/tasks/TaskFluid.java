package bq_standard.tasks;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.IFluidTask;
import betterquesting.api.questing.tasks.IItemTask;
import betterquesting.api.utils.JsonHelper;
import betterquesting.api.utils.NBTConverter;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.ParticipantInfo;
import betterquesting.api2.utils.Tuple2;
import bq_standard.client.gui.tasks.PanelTaskFluid;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.base.TaskProgressableBase;
import bq_standard.tasks.factory.FactoryTaskFluid;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTBase.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;
import org.apache.logging.log4j.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

public class TaskFluid extends TaskProgressableBase<int[]> implements ITaskInventory, IFluidTask, IItemTask
{
	public final List<FluidStack> requiredFluids = new ArrayList<>();
	//public boolean partialMatch = true; // Not many ideal ways of implementing this with fluid handlers
	public boolean ignoreNbt = true;
	public boolean consume = true;
	public boolean groupDetect = false;
	public boolean autoConsume = false;
	
	@Override
	public ResourceLocation getFactoryID()
	{
		return FactoryTaskFluid.INSTANCE.getRegistryName();
	}
	
	@Override
	public String getUnlocalisedName()
	{
		return "bq_standard.task.fluid";
	}
	
	@Override
	public void onInventoryChange(@Nonnull DBEntry<IQuest> quest, @Nonnull ParticipantInfo pInfo)
	{
        if(!consume || autoConsume)
        {
            detect(pInfo, quest);
        }
	}

	@Override
    public void detect(ParticipantInfo pInfo, DBEntry<IQuest> quest) {
        if (isComplete(pInfo.UUID)) return;

        Detector detector = new Detector(this, consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);

        final List<InventoryPlayer> invoList;
        if (consume) {
            // We do not support consuming resources from other member's invetories.
            // This could otherwise be abused to siphon items/fluids unknowingly
            invoList = Collections.singletonList(pInfo.PLAYER.inventory);
        } else {
            invoList = new ArrayList<>();
            pInfo.ACTIVE_PLAYERS.forEach((p) -> invoList.add(p.inventory));
        }

        for (InventoryPlayer invo : invoList) {
            IntStream.range(0, invo.getSizeInventory()).forEachOrdered(i -> {
                ItemStack stack = invo.getStackInSlot(i);
                detector.run(stack, (drain, drainAmount) -> getFluid(invo, i, drain, drainAmount));
            });
        }

        if (detector.updated) setBulkProgress(detector.progress);
        checkAndComplete(pInfo, quest, detector.updated);
    }
	
	private void checkAndComplete(ParticipantInfo pInfo, DBEntry<IQuest> quest, boolean resync)
    {
        final List<Tuple2<UUID, int[]>> progress = getBulkProgress(consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);
        boolean updated = resync;
        
        topLoop:
        for(Tuple2<UUID, int[]> value : progress)
        {
            for(int j = 0; j < requiredFluids.size(); j++)
            {
                if(value.getSecond()[j] >= requiredFluids.get(j).amount) continue;
                continue topLoop;
            }
            
            updated = true;
            
            if(consume)
            {
                setComplete(value.getFirst());
            } else
            {
                progress.forEach((pair) -> setComplete(pair.getFirst()));
                break;
            }
        }
		
		if(updated)
        {
            if(consume)
            {
                pInfo.markDirty(Collections.singletonList(quest.getID()));
            } else
            {
                pInfo.markDirtyParty(Collections.singletonList(quest.getID()));
            }
        }
    }
	
	/**
	 * Returns the fluid drained (or can be drained) up to the specified amount
	 */
    private FluidStack getFluid(InventoryPlayer invo, int slot, boolean drain, int amount) {
        ItemStack stack = invo.getStackInSlot(slot);
        if (stack == null || stack.stackSize <= 0 || amount <= 0) return null;
        stack = stack.copy();

        if (stack.getItem() instanceof IFluidContainerItem) {
            final IFluidContainerItem fluidContainerItem = (IFluidContainerItem) stack.getItem();
            if (fluidContainerItem.getFluid(stack) == null) return null;
            FluidStack fluid = fluidContainerItem.getFluid(stack).copy();
            fluid.amount = 0;
            while (0 < stack.stackSize && 0 < amount) {
                ItemStack oneSizedStack = stack.copy();
                oneSizedStack.stackSize = 1;
                FluidStack removed = fluidContainerItem.drain(oneSizedStack, amount, drain);
                if (removed != null && 0 < removed.amount) {
                    fluid.amount += removed.amount;
                    amount -= removed.amount;
                    stack.stackSize--;
                    if (drain) {
                        invo.decrStackSize(slot, 1);
                        if (!invo.addItemStackToInventory(oneSizedStack)) {
                            invo.player.dropPlayerItemWithRandomChoice(oneSizedStack, false);
                        }
                    }
                } else break;
            }
            return fluid;
        } else {
            FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
            int unitFluidAmount = fluid.amount;
            int emptyContainerCount = 1;
            while (fluid.amount < amount && emptyContainerCount < stack.stackSize) {
                emptyContainerCount++;
                fluid.amount += unitFluidAmount;
            }

            if (drain) {
                for (; emptyContainerCount > 0; emptyContainerCount--) {
                    ItemStack emptyContainer = FluidContainerRegistry.drainFluidContainer(stack);
                    invo.decrStackSize(slot, 1);

                    if (!invo.addItemStackToInventory(emptyContainer)) {
                        invo.player.dropPlayerItemWithRandomChoice(emptyContainer, false);
                    }
                }
            }

            return fluid;
        }
    }
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
	    //json.setBoolean("partialMatch", partialMatch);
		nbt.setBoolean("ignoreNBT", ignoreNbt);
		nbt.setBoolean("consume", consume);
		nbt.setBoolean("groupDetect", groupDetect);
		nbt.setBoolean("autoConsume", autoConsume);
		
		NBTTagList itemArray = new NBTTagList();
		for(FluidStack stack : this.requiredFluids)
		{
			itemArray.appendTag(stack.writeToNBT(new NBTTagCompound()));
		}
		nbt.setTag("requiredFluids", itemArray);
		
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
	    //partialMatch = json.getBoolean("partialMatch");
		ignoreNbt = nbt.getBoolean("ignoreNBT");
		consume = nbt.getBoolean("consume");
		groupDetect = nbt.getBoolean("groupDetect");
		autoConsume = nbt.getBoolean("autoConsume");
		
		requiredFluids.clear();
		NBTTagList fList = nbt.getTagList("requiredFluids", 10);
		for(int i = 0; i < fList.tagCount(); i++)
		{
			requiredFluids.add(JsonHelper.JsonToFluidStack(fList.getCompoundTagAt(i)));
		}
	}
	
	@Override
	public void readProgressFromNBT(NBTTagCompound nbt, boolean merge)
	{
		if(!merge)
        {
            completeUsers.clear();
            userProgress.clear();
        }
		
		NBTTagList cList = nbt.getTagList("completeUsers", 8);
		for(int i = 0; i < cList.tagCount(); i++)
		{
			try
			{
				completeUsers.add(UUID.fromString(cList.getStringTagAt(i)));
			} catch(Exception e)
			{
				BQ_Standard.logger.log(Level.ERROR, "Unable to load UUID for task", e);
			}
		}
		
		NBTTagList pList = nbt.getTagList("userProgress", 10);
		for(int n = 0; n < pList.tagCount(); n++)
		{
			try
			{
                NBTTagCompound pTag = pList.getCompoundTagAt(n);
                UUID uuid = UUID.fromString(pTag.getString("uuid"));
                
                int[] data = new int[requiredFluids.size()];
			    List<NBTBase> dNbt = NBTConverter.getTagList(pTag.getTagList("data", 3));
                for(int i = 0; i < data.length && i < dNbt.size(); i++) // TODO: Change this to an int array. This is dumb...
                {
					data[i] = ((NBTPrimitive)dNbt.get(i)).func_150287_d();
                }
                
			    userProgress.put(uuid, data);
			} catch(Exception e)
			{
				BQ_Standard.logger.log(Level.ERROR, "Unable to load user progress for task", e);
			}
		}
	}
	
	@Override
	public NBTTagCompound writeProgressToNBT(NBTTagCompound nbt, @Nullable List<UUID> users)
	{
		NBTTagList jArray = new NBTTagList();
		NBTTagList progArray = new NBTTagList();
		
		if(users != null)
        {
            users.forEach((uuid) -> {
                if(completeUsers.contains(uuid)) jArray.appendTag(new NBTTagString(uuid.toString()));
                
                int[] data = userProgress.get(uuid);
                if(data != null)
                {
                    NBTTagCompound pJson = new NBTTagCompound();
                    pJson.setString("uuid", uuid.toString());
                    NBTTagList pArray = new NBTTagList(); // TODO: Why the heck isn't this just an int array?!
                    for(int i : data) pArray.appendTag(new NBTTagInt(i));
                    pJson.setTag("data", pArray);
                    progArray.appendTag(pJson);
                }
            });
        } else
        {
            completeUsers.forEach((uuid) -> jArray.appendTag(new NBTTagString(uuid.toString())));
            
            userProgress.forEach((uuid, data) -> {
                NBTTagCompound pJson = new NBTTagCompound();
			    pJson.setString("uuid", uuid.toString());
                NBTTagList pArray = new NBTTagList(); // TODO: Why the heck isn't this just an int array?!
                for(int i : data) pArray.appendTag(new NBTTagInt(i));
                pJson.setTag("data", pArray);
                progArray.appendTag(pJson);
            });
        }
		
		nbt.setTag("completeUsers", jArray);
		nbt.setTag("userProgress", progArray);
		
		return nbt;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IGuiPanel getTaskGui(IGuiRect rect, DBEntry<IQuest> quest)
	{
	    return new PanelTaskFluid(rect, this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getTaskEditor(GuiScreen screen, DBEntry<IQuest> quest)
	{
		return null;
	}

	@Override
	public boolean canAcceptFluid(UUID owner, DBEntry<IQuest> quest, FluidStack fluid)
	{
		if(owner == null || fluid == null || fluid.getFluid() == null || !consume || isComplete(owner) || requiredFluids.size() <= 0)
		{
			return false;
		}
		
		int[] progress = getUsersProgress(owner);
		
		for(int j = 0; j < requiredFluids.size(); j++)
		{
			FluidStack rStack = requiredFluids.get(j).copy();
			if(ignoreNbt) rStack.tag = null;
			if(progress[j] < rStack.amount && rStack.equals(fluid)) return true;
		}
		
		return false;
	}

	@Override
	public boolean canAcceptItem(UUID owner, DBEntry<IQuest> quest, ItemStack item)
	{
		if(owner == null || item == null || !consume || isComplete(owner) || requiredFluids.size() <= 0)
		{
			return false;
		}
		
		if(item.getItem() instanceof IFluidContainerItem)
        {
            return canAcceptFluid(owner, quest, ((IFluidContainerItem)item.getItem()).getFluid(item));
        } else if(FluidContainerRegistry.isFilledContainer(item))
        {
            return canAcceptFluid(owner, quest, FluidContainerRegistry.getFluidForFilledItem(item));
        }
		
		return false;
	}

    @Override
    public FluidStack submitFluid(UUID owner, DBEntry<IQuest> quest, FluidStack input) {
        if (owner == null || input == null || input.amount <= 0 || !consume || isComplete(owner) || requiredFluids.size() <= 0) {
            return input;
        }

        Detector detector = new Detector(this, Collections.singletonList(owner));
        
        final FluidStack fluid = input.copy();

        detector.run(fluid, (remaining) -> {
            int removed = Math.min(fluid.amount, remaining);
            FluidStack removedFluid = fluid.copy();
            removedFluid.amount = removed;
            fluid.amount -= removed;
            return removedFluid;
        });

        if (detector.updated) {
            setBulkProgress(detector.progress);
        }

        return 0 < fluid.amount ? fluid : null;
    }

    @Override
    public void retrieveFluids(ParticipantInfo pInfo, DBEntry<IQuest> quest, FluidStack[] fluids) {
        if (consume || isComplete(pInfo.UUID)) return;

        Detector detector = new Detector(this, consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);

        for (FluidStack fluid : fluids) {
            detector.run(fluid, (remaining) -> null); // Never execute consumer
        }

        if (detector.updated) {
            setBulkProgress(detector.progress);
        }
        checkAndComplete(pInfo, quest, detector.updated);
    }


    @Override
    public ItemStack submitItem(UUID owner, DBEntry<IQuest> quest, ItemStack input) {
        if (owner == null || input == null || input.stackSize != 1 || !consume || isComplete(owner)) return input;

        Detector detector = new Detector(this, Collections.singletonList(owner));
        
        final ItemStack[] wrapper =new ItemStack[] {input.copy()};
        
        detector.run(wrapper[0], (drain, drainAmount) -> {
            if (wrapper[0].getItem() instanceof IFluidContainerItem) {
                return ((IFluidContainerItem) wrapper[0].getItem()).drain(wrapper[0], drainAmount, drain);
            } else  {
                FluidStack fluid = FluidContainerRegistry.getFluidForFilledItem(wrapper[0]);
                if (drain && fluid != null) {
                    wrapper[0] = FluidContainerRegistry.drainFluidContainer(wrapper[0]);
                }
                return fluid;
            }
        });

        if (detector.updated) {
            setBulkProgress(detector.progress);
        }

        return wrapper[0];
    }

    @Override
    public void retrieveItems(ParticipantInfo pInfo, DBEntry<IQuest> quest, ItemStack[] stacks) {
        if (consume || isComplete(pInfo.UUID)) return;

        Detector detector = new Detector(this, consume ? Collections.singletonList(pInfo.UUID) : pInfo.ALL_UUIDS);

        IntStream.range(0, stacks.length).forEachOrdered(i -> {
            final ItemStack stack = stacks[i];
            detector.run(stack, (drain, drainAmount) -> {
                final FluidStack fluid;
                if (stack.getItem() instanceof IFluidContainerItem) {
                    final ItemStack oneSizedStack = stack.copy();
                    fluid = ((IFluidContainerItem) stack.getItem()).getFluid(oneSizedStack);
                } else {
                    fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
                }
                if (fluid == null) return null;
                int unitFluidAmount = fluid.amount;
                int stackSize = stack.stackSize;
                for (int j = 0; j < stackSize; j++) {
                    if (drainAmount <= fluid.amount) break;
                    fluid.amount += Math.min(drainAmount - fluid.amount, unitFluidAmount);
                }
                return fluid;
            });
        });

        if (detector.updated) {
            setBulkProgress(detector.progress);
        }
        checkAndComplete(pInfo, quest, detector.updated);
    }

    @Override
    public int[] getUsersProgress(UUID uuid)
    {
        int[] progress = userProgress.get(uuid);
        return progress == null || progress.length != requiredFluids.size()? new int[requiredFluids.size()] : progress;
    }
	
	private List<Tuple2<UUID, int[]>> getBulkProgress(@Nonnull List<UUID> uuids)
    {
        if(uuids.size() <= 0) return Collections.emptyList();
        List<Tuple2<UUID, int[]>> list = new ArrayList<>();
        uuids.forEach((key) -> list.add(new Tuple2<>(key, getUsersProgress(key))));
        return list;
    }
    
    private void setBulkProgress(@Nonnull List<Tuple2<UUID, int[]>> list)
    {
        list.forEach((entry) -> setUserProgress(entry.getFirst(), entry.getSecond()));
    }

	@Override
	public List<String> getTextsForSearch() {
		List<String> texts = new ArrayList<>();
		for (FluidStack fluid : requiredFluids) {
			texts.add(fluid.getLocalizedName());
			texts.add(fluid.getUnlocalizedName());
		}
		return texts;
	}

    static class Detector {
        public boolean updated = false;
        public final TaskFluid task;
        /**
         * List of (player uuid, [progress per required item])
         */
        public final List<Tuple2<UUID, int[]>> progress;

        public Detector(TaskFluid task, @Nonnull List<UUID> uuids) {
            this.task = task;
            // Removing the consume check here would make the task cheaper on groups and for that reason sharing is restricted to detect only
            this.progress = task.getBulkProgress(uuids);
            if (!task.consume) {
                if (task.groupDetect) // Reset all detect progress
                {
                    progress.forEach((value) -> Arrays.fill(value.getSecond(), 0));
                } else {
                    for (int i = 0; i < task.requiredFluids.size(); i++) {
                        final int r = task.requiredFluids.get(i).amount;
                        for (Tuple2<UUID, int[]> value : progress) {
                            int n = value.getSecond()[i];
                            if (n != 0 && n < r) {
                                value.getSecond()[i] = 0;
                                updated = true;
                            }
                        }
                    }
                }
            }
        }

        /**
         * @param fluidGetter
         *     Args: (drain, drainAmount)
         */
        public void run(ItemStack stack, BiFunction<Boolean, Integer, FluidStack> fluidGetter) {
            if (stack == null || stack.stackSize <= 0) return;
            if (!(stack.getItem() instanceof IFluidContainerItem || FluidContainerRegistry.isFilledContainer(stack)))
                return;

            for (int i = 0; i < task.requiredFluids.size(); i++) {
                final FluidStack rStack = task.requiredFluids.get(i);
                FluidStack drainOG = rStack.copy();
                if (task.ignoreNbt) drainOG.tag = null;

                // Pre-check
                FluidStack sample = fluidGetter.apply(false, drainOG.amount);
                if (!drainOG.isFluidEqual(sample)) continue;

                for (Tuple2<UUID, int[]> value : progress) {
                    if (value.getSecond()[i] >= rStack.amount) continue;
                    int remaining = rStack.amount - value.getSecond()[i];

                    FluidStack drain = rStack.copy();
                    drain.amount = remaining; //drain.amount = remaining / stack.stackSize;
                    if (task.ignoreNbt) drain.tag = null;
                    if (drain.amount <= 0) continue;

                    FluidStack fluid = fluidGetter.apply(task.consume, drain.amount);
                    if (fluid == null || fluid.amount <= 0) continue;

                    value.getSecond()[i] += fluid.amount;
                    updated = true;
                }
            }
        }

        /**
         * @param consumer
         *     Args: (remaining)
         */
        public void run(FluidStack fluid, IntFunction<FluidStack> consumer) {
            if (fluid == null || fluid.amount <= 0) return;

            for (int i = 0; i < task.requiredFluids.size(); i++) {
                final FluidStack rStack = task.requiredFluids.get(i);
                FluidStack drainOG = rStack.copy();
                if (task.ignoreNbt) drainOG.tag = null;

                // Pre-check
                if (!drainOG.isFluidEqual(fluid)) continue;

                for (Tuple2<UUID, int[]> value : progress) {
                    if (value.getSecond()[i] >= rStack.amount) continue;
                    int remaining = rStack.amount - value.getSecond()[i];

                    if (task.consume) {
                        FluidStack removed = consumer.apply(remaining);
                        if (removed != null && removed.amount > 0) {
                            value.getSecond()[i] += removed.amount;
                            updated = true;
                        }
                    }else {
                        FluidStack drain = rStack.copy();
                        drain.amount = remaining; //drain.amount = remaining / stack.stackSize;
                        if (task.ignoreNbt) drain.tag = null;
                        if (drain.amount <= 0) continue;

                        FluidStack tFluid = fluid.copy();
                        tFluid.amount = Math.min(tFluid.amount, drain.amount);
                        if (tFluid.amount <= 0) continue;

                        value.getSecond()[i] += tFluid.amount;
                        updated = true;
                    }
                }
            }
        }
    }
} 