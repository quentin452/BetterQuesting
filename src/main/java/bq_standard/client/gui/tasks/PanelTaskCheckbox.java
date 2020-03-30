package bq_standard.client.gui.tasks;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasMinimum;
import betterquesting.api2.client.gui.resources.colors.GuiColorStatic;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import bq_standard.network.StandardPacketType;
import bq_standard.tasks.TaskCheckbox;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;

public class PanelTaskCheckbox extends CanvasMinimum
{
    private final IQuest quest;
    private final TaskCheckbox task;
    private final IGuiRect initialRect;
    
    public PanelTaskCheckbox(IGuiRect rect, IQuest quest, TaskCheckbox task)
    {
        super(rect);
        this.quest = quest;
        this.task = task;
        initialRect = rect;
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        
        boolean isComplete = task.isComplete(QuestingAPI.getQuestingUUID(Minecraft.getMinecraft().thePlayer));
    
        PanelButton btnCheck = new PanelButton(new GuiTransform(GuiAlign.TOP_LEFT, (initialRect.getWidth() - 32) / 2, 0, 32, 32, 0), -1, "")
        {
            @Override
            public void onButtonClick()
            {
                setIcon(PresetIcon.ICON_TICK.getTexture(), new GuiColorStatic(0xFF00FF00), 4);
                setActive(false);
                
                NBTTagCompound tags = new NBTTagCompound();
                tags.setInteger("ID", 2);
                tags.setInteger("qId", QuestingAPI.getAPI(ApiReference.QUEST_DB).getID(quest));
                tags.setInteger("tId", quest.getTasks().getID(task));
                QuestingAPI.getAPI(ApiReference.PACKET_SENDER).sendToServer(new QuestingPacket(StandardPacketType.CHECKBOX.GetLocation(), tags));
            }
        };
        btnCheck.setIcon(isComplete ? PresetIcon.ICON_TICK.getTexture() : PresetIcon.ICON_CROSS.getTexture(), new GuiColorStatic(isComplete ? 0xFF00FF00 : 0xFFFF0000), 4);
        btnCheck.setActive(!isComplete);
        this.addPanel(btnCheck);
        recalcSizes();
    }
}
