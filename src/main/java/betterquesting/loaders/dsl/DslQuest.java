package betterquesting.loaders.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DslQuest {

    public String id;
    public Map<String, String> properties = new HashMap<>();
    public List<String> taskLines = new ArrayList<>();
    public List<String> rewardLines = new ArrayList<>();
    public List<String> choiceRewards = new ArrayList<>();
    public int lineNumber = 0;

    public DslQuest(String id) {
        this.id = id;
    }

    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        return "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
}
