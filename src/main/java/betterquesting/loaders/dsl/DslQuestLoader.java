package betterquesting.loaders.dsl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import betterquesting.api.questing.IQuest;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.core.BetterQuesting;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;

public class DslQuestLoader {

    private static final String DSL_LOADER_DIR = "questsloader";
    private static final String DSL_GAMEMODE_DIR = "questsloader/gamemode";
    private static final int THREAD_POOL_SIZE = Math.max(
        2,
        Runtime.getRuntime()
            .availableProcessors() / 2);
    private static final int LOAD_TIMEOUT_SECONDS = 60;

    public static final String DSL_SOURCE_TAG = "DSL_LOADED";

    private static ExecutorService executorService;
    private static DslErrorCollector errorCollector;

    private static Set<UUID> currentDslQuestIds = ConcurrentHashMap.newKeySet();
    private static Set<UUID> currentDslQuestLineIds = ConcurrentHashMap.newKeySet();
    private static Set<DslValidator> validators = ConcurrentHashMap.newKeySet();

    public static DslErrorCollector getErrorCollector() {
        return errorCollector;
    }

    public static void registerDslQuest(UUID questId) {
        currentDslQuestIds.add(questId);
    }

    public static void registerDslQuestLine(UUID questLineId) {
        currentDslQuestLineIds.add(questLineId);
    }

    public static void loadDslQuests() {
        errorCollector = new DslErrorCollector();
        DslLayoutCalculator.clearTracking();
        currentDslQuestIds.clear();
        currentDslQuestLineIds.clear();
        validators.clear();

        File dslDir = new File(BQ_Settings.defaultDir, DSL_LOADER_DIR);

        if (!dslDir.exists()) {
            BetterQuesting.logger.info("DSL quest loader directory not found, skipping: " + dslDir.getAbsolutePath());
            return;
        }

        if (!dslDir.isDirectory()) {
            BetterQuesting.logger
                .warn("DSL quest loader path exists but is not a directory: " + dslDir.getAbsolutePath());
            return;
        }

        Set<UUID> existingDslQuests = collectExistingDslQuests();
        Set<UUID> existingDslQuestLines = collectExistingDslQuestLines();

        long startTime = System.currentTimeMillis();

        try {
            executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            File gamemodeDir = new File(BQ_Settings.defaultDir, DSL_GAMEMODE_DIR);
            if (gamemodeDir.exists() && gamemodeDir.isDirectory()) {
                loadDslDirectory(gamemodeDir, gamemodeDir);
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }

            long duration = System.currentTimeMillis() - startTime;

            for (DslValidator validator : validators) {
                validator.validateAllPrerequisites(QuestDatabase.INSTANCE, currentDslQuestIds);
            }

            removeObsoleteDslQuests(existingDslQuests, currentDslQuestIds);
            removeObsoleteDslQuestLines(existingDslQuestLines, currentDslQuestLineIds);

            if (errorCollector != null) {
                errorCollector.printToConsole();
                errorCollector.reportToOperators();
            }
        } catch (InterruptedException e) {
            BetterQuesting.logger.error("DSL loading interrupted", e);
            if (executorService != null) {
                executorService.shutdownNow();
            }
            Thread.currentThread()
                .interrupt();
        } catch (Exception e) {
            BetterQuesting.logger.error("Error during DSL quest loading", e);
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } finally {
            executorService = null;
        }
    }

    private static Set<UUID> collectExistingDslQuests() {
        Set<UUID> dslQuests = new HashSet<>();

        for (UUID questId : QuestDatabase.INSTANCE.keySet()) {
            IQuest quest = QuestDatabase.INSTANCE.get(questId);
            if (quest != null && quest.getProperty(DslProps.DSL_SOURCE, false)) {
                dslQuests.add(questId);
            }
        }

        return dslQuests;
    }

    private static Set<UUID> collectExistingDslQuestLines() {
        Set<UUID> dslQuestLines = new HashSet<>();

        for (UUID lineId : QuestLineDatabase.INSTANCE.keySet()) {
            betterquesting.api.questing.IQuestLine questLine = QuestLineDatabase.INSTANCE.get(lineId);
            if (questLine != null && questLine.getProperty(DslProps.DSL_SOURCE, false)) {
                dslQuestLines.add(lineId);
            }
        }

        return dslQuestLines;
    }

    private static void removeObsoleteDslQuests(Set<UUID> existingDslQuests, Set<UUID> currentDslQuests) {
        Set<UUID> toRemove = new HashSet<>(existingDslQuests);
        toRemove.removeAll(currentDslQuests);

        if (toRemove.isEmpty()) {
            BetterQuesting.logger.info("No obsolete DSL quests to remove");
            return;
        }

        synchronized (QuestDatabase.INSTANCE) {
            synchronized (QuestLineDatabase.INSTANCE) {
                for (UUID questId : toRemove) {
                    IQuest quest = QuestDatabase.INSTANCE.get(questId);
                    if (quest != null) {
                        String questName = quest.getProperty(betterquesting.api.properties.NativeProps.NAME, "Unknown");

                        // Remove from all quest lines
                        QuestLineDatabase.INSTANCE.removeQuest(questId);

                        // Remove from database
                        QuestDatabase.INSTANCE.remove(questId);
                    }
                }
            }
        }
    }

    private static void removeObsoleteDslQuestLines(Set<UUID> existingDslQuestLines, Set<UUID> currentDslQuestLines) {
        Set<UUID> toRemove = new HashSet<>(existingDslQuestLines);
        toRemove.removeAll(currentDslQuestLines);

        if (toRemove.isEmpty()) {
            BetterQuesting.logger.info("No obsolete DSL questlines to remove");
            return;
        }

        synchronized (QuestLineDatabase.INSTANCE) {
            for (UUID lineId : toRemove) {
                betterquesting.api.questing.IQuestLine questLine = QuestLineDatabase.INSTANCE.get(lineId);
                if (questLine != null) {
                    String lineName = questLine.getProperty(betterquesting.api.properties.NativeProps.NAME, "Unknown");
                    QuestLineDatabase.INSTANCE.remove(lineId);
                }
            }
        }
    }

    private static void loadDslDirectory(File dir, File rootDir) {
        if (!dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                loadDslDirectory(file, rootDir);
            } else if (file.getName()
                .endsWith(".dsl")) {
                    CompletableFuture<Void> future = CompletableFuture
                        .runAsync(() -> loadDslFile(file, rootDir), executorService);
                    futures.add(future);
                }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();
    }

    private static void loadDslFile(File dslFile, File rootDir) {
        String threadName = Thread.currentThread()
            .getName();
        try {
            BetterQuesting.logger.info("[" + threadName + "] Loading DSL file: " + dslFile.getName());

            DslParser parser = new DslParser();
            DslQuestData data = parser.parse(dslFile);

            if (data.quests.isEmpty()) {
                BetterQuesting.logger.warn("[" + threadName + "] No quests found in DSL file: " + dslFile.getName());
                return;
            }

            String questLineName = determineQuestLineName(dslFile, rootDir, data);
            boolean addToQuestLine = !questLineName.equals("NO_QUESTLINE");

            DslValidator validator = new DslValidator(errorCollector, dslFile.getName());
            validators.add(validator);
            DslLayoutCalculator layoutCalc = new DslLayoutCalculator();

            String layoutType = data.getMetadata("layout", "LINEAR");
            layoutCalc.setLayoutType(DslLayoutCalculator.parseLayoutType(layoutType));

            String spacingX = data.getMetadata("spacing_x");
            String spacingY = data.getMetadata("spacing_y");
            if (spacingX != null && spacingY != null) {
                try {
                    layoutCalc.setSpacing(Integer.parseInt(spacingX), Integer.parseInt(spacingY));
                } catch (NumberFormatException e) {
                }
            }

            String baseX = data.getMetadata("base_x");
            String baseY = data.getMetadata("base_y");
            if (baseX != null && baseY != null) {
                try {
                    layoutCalc.setBasePosition(Integer.parseInt(baseX), Integer.parseInt(baseY));
                } catch (NumberFormatException e) {
                }
            }

            DslQuestBuilder builder = new DslQuestBuilder(validator, layoutCalc);

            synchronized (QuestDatabase.INSTANCE) {
                synchronized (QuestLineDatabase.INSTANCE) {
                    builder.buildQuests(
                        data,
                        QuestDatabase.INSTANCE,
                        QuestLineDatabase.INSTANCE,
                        questLineName,
                        addToQuestLine);
                }
            }
        } catch (Exception e) {
            BetterQuesting.logger.error("[" + threadName + "] Error loading DSL file: " + dslFile.getName(), e);
        }
    }

    private static String determineQuestLineName(File dslFile, File rootDir, DslQuestData data) {
        String metadataQuestLine = data.getMetadata("quest_line");
        if (metadataQuestLine != null && !metadataQuestLine.isEmpty()) {
            return metadataQuestLine;
        }

        File parentDir = dslFile.getParentFile();

        if (parentDir.equals(rootDir)) {
            return "NO_QUESTLINE";
        }

        String folderName = parentDir.getName();
        return folderName.substring(0, 1)
            .toUpperCase() + folderName.substring(1);
    }

    public static void loadDslQuestsFrom(File customDir) {
        if (!customDir.exists() || !customDir.isDirectory()) {
            BetterQuesting.logger
                .warn("Custom DSL directory does not exist or is not a directory: " + customDir.getAbsolutePath());
            return;
        }

        try {
            executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            loadDslDirectory(customDir, customDir);

            executorService.shutdown();
            if (!executorService.awaitTermination(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                BetterQuesting.logger.warn("DSL loading timeout reached for custom directory");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            BetterQuesting.logger.error("DSL loading from custom directory interrupted", e);
            if (executorService != null) {
                executorService.shutdownNow();
            }
            Thread.currentThread()
                .interrupt();
        } finally {
            executorService = null;
        }
    }
}
