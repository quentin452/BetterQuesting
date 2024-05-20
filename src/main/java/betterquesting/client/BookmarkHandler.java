package betterquesting.client;

import betterquesting.core.BetterQuesting;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BookmarkHandler {

    private static final List<String> BOOKMARKS = new ArrayList<>();
    private static final String BOOKMARK_DIR = BetterQuesting.MODID + "/bookmarks/";
    private static File bookmarkFile;

    public static boolean bookmarkQuest(UUID questId) {
        String uuid = questId.toString();
        boolean added = false;
        if(BOOKMARKS.contains(uuid)){
            BOOKMARKS.remove(uuid);
        }else{
            BOOKMARKS.add(uuid);
            added = true;
        }

        saveBookmarks();
        return added;
    }

    public static boolean isBookmarked(UUID questId) {
        String uuid = questId.toString();
        return BOOKMARKS.contains(uuid);
    }

    private static void saveBookmarks() {
        try{
            FileUtils.writeLines(bookmarkFile, BOOKMARKS);
        } catch(IOException ignored) {
            BetterQuesting.logger.warn("Failed to save bookmarks.");
        }
    }

    public static void loadBookmarks(String address) {
        BOOKMARKS.clear();
        String identifier = getIdentifier(address);
        bookmarkFile = new File(BOOKMARK_DIR, String.format("%s.txt", identifier));

        if(!bookmarkFile.exists()) return;

        try{
            List<String> bookmarks = FileUtils.readLines(bookmarkFile, StandardCharsets.UTF_8);
            BOOKMARKS.addAll(bookmarks);
        } catch (IOException ignored) {
            BetterQuesting.logger.warn("Failed to load bookmarks for {}", identifier);
        }
    }

    private static String getIdentifier(String address){
        if(Minecraft.getMinecraft().isSingleplayer()){
            return FMLCommonHandler.instance().getMinecraftServerInstance().getFolderName();
        }

        int index = address.indexOf("/") + 1;
        return address.substring(index).replace(":", ".");
    }
}