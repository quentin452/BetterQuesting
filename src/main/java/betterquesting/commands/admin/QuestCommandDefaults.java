package betterquesting.commands.admin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import betterquesting.commands.QuestCommandBase;
import betterquesting.core.BQ_Settings;
import betterquesting.quests.QuestDatabase;
import betterquesting.utils.JsonIO;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

public class QuestCommandDefaults extends QuestCommandBase {
	@Override
	public String getUsageSuffix() {
		return "[save|load]";
	}

	@Override
	public boolean validArgs(String[] args) {
		return args.length == 2;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> autoComplete(ICommandSender sender, String[] args) {
		ArrayList<String> list = new ArrayList<>();

		if (args.length == 2) {
			return CommandBase.getListOfStringsMatchingLastWord(args, new String[] { "save", "load" });
		}

		return list;
	}

	@Override
	public String getCommand() {
		return "default";
	}

	@Override
	public void runCommand(CommandBase command, ICommandSender sender, String[] args) {
		if (args[1].equalsIgnoreCase("save")) {
			JsonObject jsonQ = new JsonObject();
			QuestDatabase.writeToJson(jsonQ);
			JsonIO.WriteToFile(new File(MinecraftServer.getServer().getFile("config/betterquesting/"), "DefaultQuests.json"), jsonQ);
			sender.addChatMessage(new ChatComponentTranslation("betterquesting.cmd.default.save"));
		} else if (args[1].equalsIgnoreCase("load")) {
			File f1 = new File(BQ_Settings.defaultDir, "DefaultQuests.json");

			if (f1.exists()) {
				JsonObject j1 = new JsonObject();
				j1 = JsonIO.ReadFromFile(f1);
				QuestDatabase.readFromJson_keepProgression(j1);
				sender.addChatMessage(new ChatComponentTranslation("betterquesting.cmd.default.load"));
				QuestDatabase.UpdateClients();
			} else {
				sender.addChatMessage(new ChatComponentTranslation("betterquesting.cmd.default.none"));
			}
		} else {
			throw getException(command);
		}
	}
}
