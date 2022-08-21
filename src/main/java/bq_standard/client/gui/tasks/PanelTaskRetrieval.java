package bq_standard.client.gui.tasks;

import betterquesting.api.api.QuestingAPI;
import betterquesting.api.properties.NativeProps;
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
import betterquesting.api2.utils.QuestTranslation;
import bq_standard.core.BQ_Standard;
import bq_standard.tasks.TaskRetrieval;
import codechicken.nei.ItemPanels;
import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.Optional.Method;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class PanelTaskRetrieval extends CanvasMinimum
{
    private final TaskRetrieval task;
    private final IGuiRect initialRect;
    
    public PanelTaskRetrieval(IGuiRect rect, TaskRetrieval task)
    {
        super(rect);
        this.task = task;
        initialRect = rect;
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
        int listW = initialRect.getWidth();
        
        UUID uuid = QuestingAPI.getQuestingUUID(Minecraft.getMinecraft().thePlayer);
        int[] progress = task.getUsersProgress(uuid);
        boolean isComplete = task.isComplete(uuid);
        
        String sCon = (task.consume ? EnumChatFormatting.RED : EnumChatFormatting.GREEN) + QuestTranslation.translate(task.consume ? "gui.yes" : "gui.no");
        this.addPanel(new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, 0, 0, listW, 16, 0), QuestTranslation.translate("bq_standard.btn.consume", sCon)).setColor(PresetColor.TEXT_MAIN.getColor()));

        for(int i = 0; i < task.requiredItems.size(); i++)
        {
            BigItemStack stack = task.requiredItems.get(i);
            
            if(stack == null)
            {
                continue;
            }
    
            PanelItemSlot slot = new PanelItemSlot(new GuiRectangle(0, i * 32 + 16, 28, 28, 0), -1, stack, false, true);
            if(BQ_Standard.hasNEI) slot.setCallback(value -> lookupRecipe(value.getBaseStack()));
            this.addPanel(slot);
            
            StringBuilder sb = new StringBuilder();
            
            sb.append(stack.getBaseStack().getDisplayName());
            
			if(stack.hasOreDict()) sb.append(" (").append(stack.getOreDict()).append(")");
			
			sb.append("\n").append(progress[i]).append("/").append(stack.stackSize).append("\n");
			
			if(isComplete || progress[i] >= stack.stackSize)
			{
				sb.append(EnumChatFormatting.GREEN).append(QuestTranslation.translate("betterquesting.tooltip.complete"));
			} else
			{
				sb.append(EnumChatFormatting.RED).append(QuestTranslation.translate("betterquesting.tooltip.incomplete"));
			}
            
            PanelTextBox text = new PanelTextBox(new GuiRectangle(32, i * 32 + 16, listW - 28, 28, 0), sb.toString());
			text.setColor(PresetColor.TEXT_MAIN.getColor());
			this.addPanel(text);
        }

        recalcSizes();
    }
    
    @Method(modid = "NotEnoughItems")
    private void lookupRecipe(ItemStack stack)
    {
        if (GuiScreen.isShiftKeyDown()) {
            ItemPanels.bookmarkPanel.addOrRemoveItem(stack, StringUtils.EMPTY, null, false, true);
        } else {
            GuiCraftingRecipe.openRecipeGui("item", stack);
        }
    }
}
