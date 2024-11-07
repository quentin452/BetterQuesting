package betterquesting.core.proxies;

import net.minecraftforge.common.MinecraftForge;

import betterquesting.core.BetterQuesting;
import betterquesting.core.ExpansionLoader;
import betterquesting.handlers.EventHandler;
import betterquesting.handlers.GuiHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;

public class CommonProxy {

    public boolean isClient() {
        return false;
    }

    public void registerHandlers() {
        ExpansionLoader.INSTANCE.initCommonAPIs();

        MinecraftForge.EVENT_BUS.register(EventHandler.INSTANCE);
        MinecraftForge.TERRAIN_GEN_BUS.register(EventHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(EventHandler.INSTANCE);

        NetworkRegistry.INSTANCE.registerGuiHandler(BetterQuesting.instance, new GuiHandler());
    }

    public void registerRenderers() {}
}
