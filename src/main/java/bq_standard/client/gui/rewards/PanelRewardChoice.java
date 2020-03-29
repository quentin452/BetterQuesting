package bq_standard.client.gui.rewards;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasMinimum;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import bq_standard.network.StandardPacketType;
import bq_standard.rewards.RewardChoice;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.util.vector.Vector4f;

import java.util.UUID;

public class PanelRewardChoice extends CanvasMinimum
{
    private final IQuest quest;
    private final RewardChoice reward;
    private final IGuiRect initialRect;
    
    public PanelRewardChoice(IGuiRect rect, IQuest quest, RewardChoice reward)
    {
        super(rect);
        initialRect = rect;
        this.quest = quest;
        this.reward = reward;
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();

        UUID uuid = QuestingAPI.getQuestingUUID(Minecraft.getMinecraft().thePlayer);
        int sel = reward.getSelecton(uuid);
        PanelItemSlot slot = new PanelItemSlot(new GuiTransform(new Vector4f(0F, 0F, 0F, 0F), 0, 0, 32, 32, 0), -1, sel < 0 ? null : reward.choices.get(sel));
        this.addPanel(slot);
        
        int listWidth = initialRect.getWidth();
        for(int i = 0; i < reward.choices.size(); i++)
        {
            BigItemStack stack = reward.choices.get(i);
            PanelItemSlot is = new PanelItemSlot(new GuiRectangle(40, i * 18, 18, 18, 0), -1, stack, true);
            this.addPanel(is);
            
            this.addPanel(new PanelTextBox(new GuiRectangle(62, i * 18 + 4, listWidth - 22, 14, 0), stack.stackSize + " " + stack.getBaseStack().getDisplayName()).setColor(PresetColor.TEXT_MAIN.getColor()));
            
            final int sID = i;
            is.setCallback(value -> {
                slot.setStoredValue(value);
                
                NBTTagCompound retTags = new NBTTagCompound();
                retTags.setInteger("questID", QuestingAPI.getAPI(ApiReference.QUEST_DB).getID(quest));
                retTags.setInteger("rewardID", quest.getRewards().getID(reward));
                retTags.setInteger("selection", sID);
                QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToServer(new QuestingPacket(StandardPacketType.CHOICE.GetLocation(), retTags));
            });
        }

        recalcSizes();
    }
}
