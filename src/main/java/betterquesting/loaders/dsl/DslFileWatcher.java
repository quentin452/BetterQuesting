package betterquesting.loaders.dsl;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import betterquesting.api.events.DatabaseEvent;
import betterquesting.api.events.DatabaseEvent.DBType;
import betterquesting.api.storage.BQ_Settings;
import betterquesting.core.BetterQuesting;
import betterquesting.handlers.SaveLoadHandler;
import betterquesting.loaders.dsl.DslErrorCollector;
import betterquesting.loaders.dsl.DslFileWatcher;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

public class DslFileWatcher {

    private static final String DSL_LOADER_DIR = "questsloader";
    private static final String DSL_GAMEMODE_DIR = "questsloader/gamemode";
    private static WatchService watchService;
    private static Thread watchThread;
    private static boolean isRunning = false;

    private static final long DEBOUNCE_DELAY_MS = 2000;
    private static long lastReloadTime = 0;
    private static boolean reloadPending = false;

    private static final Map<String, Long> modifiedFiles = new HashMap<>();

    private static final Map<WatchKey, Path> watchKeys = new HashMap<>();

    public static synchronized void startWatching() {
        if (isRunning) {
            BetterQuesting.logger.info("DSL file watcher is already running");
            return;
        }

        File dslDir = new File(BQ_Settings.defaultDir, DSL_LOADER_DIR);
        if (!dslDir.exists() || !dslDir.isDirectory()) {
            BetterQuesting.logger.info("DSL directory does not exist, file watcher not started");
            return;
        }

        try {
            watchService = FileSystems.getDefault()
                .newWatchService();

            File gamemodeDir = new File(BQ_Settings.defaultDir, DSL_GAMEMODE_DIR);
            if (gamemodeDir.exists() && gamemodeDir.isDirectory()) {
                registerRecursive(gamemodeDir.toPath());
            } else {
                if (!gamemodeDir.mkdirs()) {
                    BetterQuesting.logger.warn("Failed to create gamemode directory");
                }
            }

            isRunning = true;

            watchThread = new Thread(() -> watchLoop(), "DSL-FileWatcher");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            BetterQuesting.logger.error("Failed to start DSL file watcher", e);
        }
    }

    public static synchronized void stopWatching() {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        if (watchThread != null) {
            watchThread.interrupt();
            try {
                watchThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
            }
            watchThread = null;
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                BetterQuesting.logger.error("Error closing DSL file watcher", e);
            }
            watchService = null;
        }

        watchKeys.clear();
    }

    private static void registerRecursive(Path path) throws IOException {
        WatchKey key = path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeys.put(key, path);

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs)
                throws IOException {
                if (!dir.equals(path)) {
                    WatchKey subKey = dir.register(
                        watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                    watchKeys.put(subKey, dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void watchLoop() {
        while (isRunning) {
            try {
                WatchKey key = watchService.poll(500, TimeUnit.MILLISECONDS);

                if (key == null) {
                    if (reloadPending && (System.currentTimeMillis() - lastReloadTime) >= DEBOUNCE_DELAY_MS) {
                        performReload();
                    }
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();

                    if (fileName == null) {
                        continue;
                    }

                    String fileNameStr = fileName.toString();

                    if (fileNameStr.endsWith(".dsl")) {
                        handleFileChange(kind, fileNameStr);
                    }

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path dir = (Path) key.watchable();
                        Path child = dir.resolve(fileName);
                        if (Files.isDirectory(child)) {
                            try {
                                registerRecursive(child);
                            } catch (IOException e) {
                                BetterQuesting.logger.error("Failed to register new directory: " + child, e);
                            }
                        }
                    }

                    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        Path dir = (Path) key.watchable();
                        Path child = dir.resolve(fileName);
                        watchKeys.entrySet()
                            .removeIf(entry -> {
                                Path watchedPath = entry.getValue();
                                return watchedPath.startsWith(child);
                            });
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    Path watchedPath = watchKeys.remove(key);
                    if (watchedPath != null) {
                        BetterQuesting.logger.info("Unregistered watch key for deleted directory: " + watchedPath);
                    }
                    continue;
                }

            } catch (InterruptedException e) {
                BetterQuesting.logger.info("DSL file watcher interrupted");
                Thread.currentThread()
                    .interrupt();
                break;
            } catch (Exception e) {
                BetterQuesting.logger.error("Error in DSL file watcher", e);
            }
        }

        BetterQuesting.logger.info("DSL file watcher thread stopped");
    }

    private static void handleFileChange(WatchEvent.Kind<?> kind, String fileName) {
        String action = "modified";
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            action = "created";
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            action = "deleted";
        }

        BetterQuesting.logger.info("DSL file " + action + ": " + fileName);

        modifiedFiles.put(fileName, System.currentTimeMillis());

        lastReloadTime = System.currentTimeMillis();
        reloadPending = true;
    }


    private static void performReload() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            reloadPending = false;
            modifiedFiles.clear();
            return;
        }

        reloadPending = false;

        int fileCount = modifiedFiles.size();

        try {
            DslQuestLoader.loadDslQuests();
            SaveLoadHandler.INSTANCE.markDirty();
            if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
                MinecraftForge.EVENT_BUS.post(new DatabaseEvent.Update(DBType.QUEST));
                MinecraftForge.EVENT_BUS.post(new DatabaseEvent.Update(DBType.CHAPTER));
            }
            
            DslErrorCollector errorCollector = DslQuestLoader.getErrorCollector();
            if (errorCollector != null) {
                errorCollector.reportToOperators();
                if (errorCollector.hasErrors()) {
                    BetterQuesting.logger.warn("DSL auto-reload completed with errors - check console for details");
                } else {
                    BetterQuesting.logger.info("DSL auto-reload completed successfully");
                }
            }

        } catch (Exception e) {
            BetterQuesting.logger.error("Error during DSL auto-reload", e);
        } finally {
            modifiedFiles.clear();
        }
    }

    public static boolean isWatching() {
        return isRunning;
    }
}
