package betterquesting.commands.admin;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.MinecraftForge;

import betterquesting.api.events.DatabaseEvent;
import betterquesting.api.events.DatabaseEvent.DBType;
import betterquesting.commands.QuestCommandBase;
import betterquesting.handlers.SaveLoadHandler;
import betterquesting.loaders.dsl.DslQuestLoader;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class QuestCommandDslReload extends QuestCommandBase {

    @Override
    public String getCommand() {
        return "dsl_reload";
    }

    @Override
    public String getUsageSuffix() {
        return "";
    }

    @Override
    public void runCommand(MinecraftServer server, CommandBase command, ICommandSender sender, String[] args) {
        try {
            DslQuestLoader.loadDslQuests();
            SaveLoadHandler.INSTANCE.markDirty();
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                MinecraftForge.EVENT_BUS.post(new DatabaseEvent.Update(DBType.QUEST));
                MinecraftForge.EVENT_BUS.post(new DatabaseEvent.Update(DBType.CHAPTER));
            }
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("§c[BQ] Error reloading DSL quests: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public List<String> autoComplete(MinecraftServer server, ICommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
