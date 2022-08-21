package bq_standard.client.gui.tasks;

import betterquesting.api.utils.BigItemStack;
import codechicken.nei.ItemPanels;
import codechicken.nei.recipe.GuiCraftingRecipe;
import cpw.mods.fml.common.Optional;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

abstract class NEIUtils {

    @Optional.Method(modid = "NotEnoughItems")
    protected static void processItemSlotCallback(BigItemStack bigItemStack) {
        ItemStack itemStack = bigItemStack.getBaseStack();
        itemStack.stackSize = bigItemStack.stackSize;

        if (GuiScreen.isShiftKeyDown()) {
            ItemPanels.bookmarkPanel.addOrRemoveItem(itemStack, StringUtils.EMPTY, null, false, true);
        } else {
            GuiCraftingRecipe.openRecipeGui("item", itemStack);
        }
    }
}
