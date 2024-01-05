package betterquesting.client.gui2;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiPadding;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestBookmarks;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.misc.QuestSearchEntry;
import net.minecraft.client.gui.GuiScreen;

import java.util.function.Consumer;

public class GuiBookmarks extends GuiScreenCanvas {
    private Consumer<QuestSearchEntry> callback;

    public GuiBookmarks(GuiScreen parent) {
        super(parent);
    }

    @Override
    public void initPanel() {
        super.initPanel();
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);

        CanvasEmpty cvInner = new CanvasEmpty(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(8, 8, 8, 8), 0));
        cvBackground.addPanel(cvInner);

        createExitButton(cvInner);

        PanelTextBox txtDb = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 0, 0, -16), 0), QuestTranslation.translate("betterquesting.gui.bookmarks")).setAlignment(1).setColor(PresetColor.TEXT_MAIN.getColor());
        cvInner.addPanel(txtDb);

        CanvasQuestBookmarks pins = createMainCanvas();
        cvInner.addPanel(pins);

        PanelVScrollBar scDb = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 32, 0, 24), 0));
        cvInner.addPanel(scDb);
        pins.setScrollDriverY(scDb);
    }

    private CanvasQuestBookmarks createMainCanvas() {
        CanvasQuestBookmarks pins = new CanvasQuestBookmarks(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 32, 8, 24), 0), mc.thePlayer);
        pins.setQuestOpenCallback(pinEntry -> {
            acceptCallback(pinEntry);
            GuiHome.bookmark = new GuiQuest(parent, pinEntry.getQuest().getKey());
            mc.displayGuiScreen(GuiHome.bookmark);
        });
        pins.setQuestHighlightCallback(pinEntry -> {
            mc.displayGuiScreen(parent);
            acceptCallback(pinEntry);
        });
        return pins;
    }

    private void createExitButton(CanvasEmpty cvInner) {
        PanelButton btnExit = new PanelButton(
                new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0),
                0,
                QuestTranslation.translate("gui.back")
        );
        btnExit.setClickAction((b) -> mc.displayGuiScreen(parent));
        cvInner.addPanel(btnExit);
    }

    public void setCallback(Consumer<QuestSearchEntry> callback) {
        this.callback = callback;
    }

    private void acceptCallback(QuestSearchEntry questSearchEntry){
        if (callback != null) callback.accept(questSearchEntry);
    }
}
