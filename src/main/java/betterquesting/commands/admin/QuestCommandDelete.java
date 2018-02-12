package betterquesting.commands.admin;

import java.util.ArrayList;
import java.util.List;

import betterquesting.commands.QuestCommandBase;
import betterquesting.quests.QuestDatabase;
import betterquesting.quests.QuestInstance;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentTranslation;

public class QuestCommandDelete extends QuestCommandBase {
	@Override
	public String getUsageSuffix() {
		return "[all|<quest_id>]";
	}

	@Override
	public boolean validArgs(String[] args) {
		return args.length == 2;
	}

	@Override
	public List<String> autoComplete(ICommandSender sender, String[] args) {
		ArrayList<String> list = new ArrayList<>();

		if (args.length == 2) {
			list.add("all");

			for (int i : QuestDatabase.questDB.keySet()) {
				list.add("" + i);
			}
		}

		return list;
	}

	@Override
	public String getCommand() {
		return "delete";
	}

	@Override
	public void runCommand(CommandBase command, ICommandSender sender, String[] args) {
		if (args[1].equalsIgnoreCase("all")) {
			QuestDatabase.questDB.clear();
			QuestDatabase.questLines.clear();

			sender.addChatMessage(new ChatComponentTranslation("betterquesting.cmd.delete.all"));
		} else {
			try {
				int id = Integer.parseInt(args[1].trim());
				QuestInstance quest = QuestDatabase.getQuestByID(id);
				QuestDatabase.DeleteQuest(id);

				sender.addChatMessage(new ChatComponentTranslation("betterquesting.cmd.delete.single", new ChatComponentTranslation(quest.name)));
			} catch (Exception e) {
				throw getException(command);
			}
		}

		QuestDatabase.UpdateClients();
	}

}
