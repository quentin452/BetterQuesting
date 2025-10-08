package betterquesting.loaders.dsl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import betterquesting.api.storage.BQ_Settings;
import betterquesting.core.BetterQuesting;

public class DslInitializer {

    private static final String DSL_LOADER_DIR = "questsloader";
    private static final String DSL_GAMEMODE_DIR = "questsloader/gamemode";

    public static void initialize() {
        createDslDirectories();
    }


    private static void createDslDirectories() {
        File loaderDir = new File(BQ_Settings.defaultDir, DSL_LOADER_DIR);
        File gamemodeDir = new File(BQ_Settings.defaultDir, DSL_GAMEMODE_DIR);

        if (!loaderDir.exists()) {
            if (!loaderDir.mkdirs()) {
                BetterQuesting.logger.warn("Failed to create DSL loader directory: " + loaderDir.getAbsolutePath());
                return;
            }
        }

        if (!gamemodeDir.exists()) {
            if (!gamemodeDir.mkdirs()) {
                BetterQuesting.logger.warn("Failed to create DSL gamemode directory: " + gamemodeDir.getAbsolutePath());
            }
        }
    }
}
