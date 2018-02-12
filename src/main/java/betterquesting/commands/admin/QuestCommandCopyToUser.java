package betterquesting.commands.admin;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import betterquesting.commands.QuestCommandBase;
import betterquesting.party.PartyManager;
import betterquesting.quests.QuestDatabase;
import betterquesting.quests.QuestInstance;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class QuestCommandCopyToUser extends QuestCommandBase {
	@Override
	public String getUsageSuffix() {
		return "[from username|uuid] [to username|uuid]";
	}

	@Override
	public boolean validArgs(String[] args) {
		return args.length == 3;
	}

	@Override
	public String getCommand() {
		return "copyprogress";
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<String> autoComplete(ICommandSender sender, String[] args) {
		if (args.length == 3 || args.length == 2) {
			return CommandBase.getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
		}
		return Collections.emptyList();
	}

	@Override
	public void runCommand(CommandBase command, ICommandSender sender, String[] args) {
		try {
			copyQuestProgress(sender, args[1], args[2]);
		} catch (Exception e) {
			throw getException(command);
		}
	}

	private void copyQuestProgress(ICommandSender sender, String from, String to) {
		UUID uFrom = PartyManager.GetUUID(from);
		UUID uTo = PartyManager.GetUUID(to);

		if (uFrom == null) {
			try {
				uFrom = UUID.fromString(from);
			} catch (IllegalArgumentException ex) {
				sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "'From' user could not be found. Try entering their UUID."));
				return;
			}
		}
		if (uTo == null) {
			try {
				uTo = UUID.fromString(to);
			} catch (IllegalArgumentException ex) {
				sender.addChatMessage(new ChatComponentText(EnumChatFormatting.RED + "'To' user could not be found. Try entering their UUID."));
				return;
			}
		}
		long current = System.currentTimeMillis();
		int questsCompleted = 0;
		for (QuestInstance questInstance : QuestDatabase.questDB.values()) {
			if (questInstance.isComplete(uFrom) && !questInstance.isComplete(uTo)) {
				questInstance.setComplete(uTo, current);
				questsCompleted++;
			}
		}
		if (questsCompleted > 0) {
			QuestDatabase.sendDBToParty(uTo);
		}
		sender.addChatMessage(new ChatComponentText("Completed " + questsCompleted + " quests for " + uTo));
	}
}
