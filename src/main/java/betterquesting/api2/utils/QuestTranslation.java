package betterquesting.api2.utils;

import betterquesting.api.properties.IPropertyContainer;
import betterquesting.api.properties.IPropertyType;
import betterquesting.api.properties.NativeProps;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.IQuestLine;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public class QuestTranslation {
    private static final String QUEST_NAME_KEY = "betterquesting.quest.%s.name";
    private static final String QUEST_DESCRIPTION_KEY = "betterquesting.quest.%s.desc";
    private static final String QUEST_LINE_NAME_KEY = "betterquesting.questline.%s.name";
    private static final String QUEST_LINE_DESCRIPTION_KEY = "betterquesting.questline.%s.desc";

    /**
     * We'll look up translation keys directly from the map, to avoid needing to perform string
     * comparison to check if a key is missing.
     *
     * <p>This must be a supplier, because this class is called on the server (in order to build
     * quest translation keys during saving). The {@link I18n} class is not available on the server,
     * so attempting to build this map will cause a crash.
     */
    private static final Supplier<Map<String, String>> translations =
            Suppliers.memoize(
                    () -> {
                        try {
                            Field localeField = ReflectionHelper.findField(I18n.class, "i18nLocale", "field_135054_a");
                            Field translationsField = ReflectionHelper.findField(Locale.class, "field_135032_a");
                            return (Map<String, String>) translationsField.get(localeField.get(null));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    });

    public static String translate(String text, Object... args) {
        String out = I18n.format(text, args);
        if (out.startsWith("Format error: ")) {
            return text; // TODO: Find a more reliable way of detecting translation failure
        }
        return out;
    }
    
    public static String translateTrimmed(String text, Object... args) {
        return translate(text, args).replaceAll("\r", "");
    }

    public static String buildQuestNameKey(UUID questId) {
        return String.format(QUEST_NAME_KEY, questId);
    }

    public static String translateQuestName(UUID questId, IQuest quest) {
        return translateProperty(buildQuestNameKey(questId), quest, NativeProps.NAME);
    }

    public static String translateQuestName(Map.Entry<UUID, IQuest> entry) {
        return translateQuestName(entry.getKey(), entry.getValue());
    }

    public static String buildQuestDescriptionKey(UUID questId) {
        return String.format(QUEST_DESCRIPTION_KEY, questId);
    }

    public static String translateQuestDescription(UUID questId, IQuest quest) {
        return translateProperty(buildQuestDescriptionKey(questId), quest, NativeProps.DESC);
    }

    public static String translateQuestDescription(Map.Entry<UUID, IQuest> entry) {
        return translateQuestDescription(entry.getKey(), entry.getValue());
    }

    public static String buildQuestLineNameKey(UUID questLineId) {
        return String.format(QUEST_LINE_NAME_KEY, questLineId);
    }

    public static String translateQuestLineName(UUID questLineId, IQuestLine questLine) {
        return translateProperty(buildQuestLineNameKey(questLineId), questLine, NativeProps.NAME);
    }

    public static String translateQuestLineName(Map.Entry<UUID, IQuestLine> entry) {
        return translateQuestLineName(entry.getKey(), entry.getValue());
    }

    public static String buildQuestLineDescriptionKey(UUID questLineId) {
        return String.format(QUEST_LINE_DESCRIPTION_KEY, questLineId);
    }

    public static String translateQuestLineDescription(UUID questLineId, IQuestLine questLine) {
        return translateProperty(
                buildQuestLineDescriptionKey(questLineId), questLine, NativeProps.DESC);
    }

    public static String translateQuestLineDescription(Map.Entry<UUID, IQuestLine> entry) {
        return translateQuestLineDescription(entry.getKey(), entry.getValue());
    }

    /**
     * Returns the translation, if one exists for {@code key}.
     * If no translation exists, then {@code property} is fetched from {@code container}.
     */
    private static String translateProperty(
            String key, IPropertyContainer container, IPropertyType<String> property) {
        String translation = translations.get().get(key);
        if (translation != null) {
            return String.format(translation);
        }

        return container.getProperty(property);
    }
}
