package bq_standard.client.gui.rewards;

import org.lwjgl.util.vector.Vector4f;

import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.CanvasMinimum;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.utils.QuestTranslation;
import bq_standard.rewards.RewardQuestCompletion;
import net.minecraft.util.EnumChatFormatting;

public class PanelRewardQuestCompletion extends CanvasMinimum {
	private final RewardQuestCompletion reward;
    private final IGuiRect initialRect;
    
    public PanelRewardQuestCompletion(IGuiRect rect, RewardQuestCompletion reward) {
        super(rect);
        this.reward = reward;
        initialRect = rect;
    }
    
    @Override
    public void initPanel() {
        super.initPanel();
        int width = initialRect.getWidth();
        
        String txt2 = EnumChatFormatting.BLACK + "" + reward.questNum;
		
        this.addPanel(new PanelTextBox(new GuiTransform(new Vector4f(0F, 0F, 0F, 0F), 36, 2, width - 36, 16, 0), QuestTranslation.translate("bq_standard.gui.questcompletion")).setColor(PresetColor.TEXT_MAIN.getColor()));
        this.addPanel(new PanelTextBox(new GuiTransform(new Vector4f(0F, 0F, 0F, 0F), 40, 16, width - 40, 16, 0), txt2).setColor(PresetColor.TEXT_MAIN.getColor()));
        recalcSizes();
    }
}
