package betterquesting.loaders.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DslQuestData {

    public Map<String, String> metadata = new HashMap<>();
    public List<DslQuest> quests = new ArrayList<>();

    public String getMetadata(String key) {
        return metadata.get(key);
    }

    public String getMetadata(String key, String defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }
}
