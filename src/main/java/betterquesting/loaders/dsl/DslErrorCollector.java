package betterquesting.loaders.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;

import cpw.mods.fml.common.FMLLog;

public class DslErrorCollector {

    private final ConcurrentLinkedQueue<DslError> errors = new ConcurrentLinkedQueue<>();

    public void addError(DslError error) {
        errors.add(error);
        FMLLog.severe("[BetterQuesting DSL] " + error.toString());
    }

    public void addError(DslError.Severity severity, String fileName, int lineNumber, String message, String context) {
        addError(new DslError(severity, fileName, lineNumber, message, context));
    }

    public void addError(DslError.Severity severity, String fileName, int lineNumber, String message) {
        addError(new DslError(severity, fileName, lineNumber, message, null));
    }

    public List<DslError> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<DslError> getErrorsBySeverity(DslError.Severity severity) {
        List<DslError> result = new ArrayList<>();
        for (DslError error : errors) {
            if (error.getSeverity() == severity) {
                result.add(error);
            }
        }
        return result;
    }

    public boolean hasErrors() {
        for (DslError error : errors) {
            if (error.getSeverity() == DslError.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }

    public boolean hasWarnings() {
        for (DslError error : errors) {
            if (error.getSeverity() == DslError.Severity.WARNING) {
                return true;
            }
        }
        return false;
    }

    public int getErrorCount() {
        return getErrorsBySeverity(DslError.Severity.ERROR).size();
    }

    public int getWarningCount() {
        return getErrorsBySeverity(DslError.Severity.WARNING).size();
    }

    public void clear() {
        errors.clear();
    }

    public void reportToOperators() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            FMLLog.info("[BetterQuesting DSL] Cannot send chat messages - server not available");
            return;
        }

        int errorCount = getErrorCount();
        int warningCount = getWarningCount();

        String[] ops = server.getConfigurationManager()
            .func_152606_n();

        List<EntityPlayerMP> players = ops.length > 0 ? getOnlineOps(server, ops)
            : new ArrayList<EntityPlayerMP>(server.getConfigurationManager().playerEntityList);

        if (players.isEmpty()) {
            FMLLog.info("[BetterQuesting DSL] No online players to receive error report");
            return;
        }

        if (errorCount == 0 && warningCount == 0) {
            for (EntityPlayerMP player : players) {
                try {
                    player.addChatMessage(new ChatComponentText("§a═══════════════════════════════════════"));
                    player.addChatMessage(new ChatComponentText("§a§lDSL Quest Loading Report"));
                    player.addChatMessage(new ChatComponentText("§a═══════════════════════════════════════"));
                    player.addChatMessage(new ChatComponentText("§a✔ No errors or warnings found!"));
                    player.addChatMessage(new ChatComponentText("§a═══════════════════════════════════════"));
                } catch (Exception e) {
                    FMLLog.warning(
                        "[BetterQuesting DSL] Failed to send success report to player: "
                            + player.getCommandSenderName());
                }
            }
            return;
        }

        for (EntityPlayerMP player : players) {
            try {
                player.addChatMessage(new ChatComponentText("§c═══════════════════════════════════════"));
                player.addChatMessage(new ChatComponentText("§c§lDSL Quest Loading Report"));
                player.addChatMessage(new ChatComponentText("§c═══════════════════════════════════════"));

                if (errorCount > 0) {
                    player.addChatMessage(new ChatComponentText("§c✖ " + errorCount + " Error(s) found"));
                }
                if (warningCount > 0) {
                    player.addChatMessage(new ChatComponentText("§e⚠ " + warningCount + " Warning(s) found"));
                }

                player.addChatMessage(new ChatComponentText(""));

                List<DslError> sortedErrors = new ArrayList<>(errors);
                int shown = 0;
                for (DslError error : sortedErrors) {
                    if (shown >= 10) {
                        int remaining = errors.size() - shown;
                        player.addChatMessage(new ChatComponentText("§7... and " + remaining + " more issues"));
                        break;
                    }

                    player.addChatMessage(new ChatComponentText(error.toColoredString()));
                    shown++;
                }

                player.addChatMessage(new ChatComponentText("§c═══════════════════════════════════════"));
                player.addChatMessage(new ChatComponentText("§7Check server console for full details"));
            } catch (Exception e) {
                FMLLog.warning(
                    "[BetterQuesting DSL] Failed to send error report to player: " + player.getCommandSenderName());
            }
        }
    }

    private List<EntityPlayerMP> getOnlineOps(MinecraftServer server, String[] ops) {
        List<EntityPlayerMP> onlineOps = new ArrayList<>();
        for (String opName : ops) {
            EntityPlayerMP player = server.getConfigurationManager()
                .func_152612_a(opName);
            if (player != null) {
                onlineOps.add(player);
            }
        }
        return onlineOps;
    }

    public void printToConsole() {
        if (errors.isEmpty()) {
            FMLLog.info("[BetterQuesting DSL] Loading completed with no errors or warnings");
            return;
        }

        FMLLog.warning("[BetterQuesting DSL] ═══════════════════════════════════════");
        FMLLog.warning("[BetterQuesting DSL] DSL Quest Loading Report");
        FMLLog.warning("[BetterQuesting DSL] ═══════════════════════════════════════");
        FMLLog.warning("[BetterQuesting DSL] Total Issues: " + errors.size());
        FMLLog.warning("[BetterQuesting DSL] Errors: " + getErrorCount());
        FMLLog.warning("[BetterQuesting DSL] Warnings: " + getWarningCount());
        FMLLog.warning("[BetterQuesting DSL] ═══════════════════════════════════════");

        for (DslError error : errors) {
            switch (error.getSeverity()) {
                case ERROR:
                    FMLLog.severe("[BetterQuesting DSL] " + error.toString());
                    break;
                case WARNING:
                    FMLLog.warning("[BetterQuesting DSL] " + error.toString());
                    break;
                case INFO:
                    FMLLog.info("[BetterQuesting DSL] " + error.toString());
                    break;
            }
        }

        FMLLog.warning("[BetterQuesting DSL] ═══════════════════════════════════════");
    }
}
