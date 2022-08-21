package bq_standard.client.gui.panels.content;

import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import codechicken.nei.api.ShortcutInputHandler;
import net.minecraft.item.ItemStack;

/**
 * NEI compatible ItemSlot
 * <p>
 * Process any input while hovering this panel and pass the ItemStack to NEI allowing it to process
 * it's internal keybindings.
 */
public class PanelInteractiveItemSlot extends PanelItemSlot {

    private boolean isMouseHovered;

    public PanelInteractiveItemSlot(IGuiRect rect, int id, BigItemStack value, boolean showCount, boolean oreDict) {
        super(rect, id, value, showCount, oreDict);
    }

    @Override
    public boolean onKeyTyped(char c, int keycode) {
        if (isMouseHovered) {
            return ShortcutInputHandler.handleKeyEvent(getBaseStackOfSameSize());
        }

        return false;
    }

    @Override
    public boolean onMouseClick(int mx, int my, int click) {
        if (isMouseHovered) {
            return ShortcutInputHandler.handleMouseClick(getBaseStackOfSameSize());
        }

        return false;
    }

    @Override
    public void drawPanel(int mx, int my, float partialTick) {
        isMouseHovered = isActive() && getTransform().contains(mx, my);
        super.drawPanel(mx, my, partialTick);
    }

    private ItemStack getBaseStackOfSameSize() {
        BigItemStack bigItemStack = getStoredValue();

        ItemStack itemStack = bigItemStack.getBaseStack();
        itemStack.stackSize = bigItemStack.stackSize;

        return itemStack;
    }
}
