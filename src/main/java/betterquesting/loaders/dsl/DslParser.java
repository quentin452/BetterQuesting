package betterquesting.loaders.dsl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.Level;

import betterquesting.api.api.QuestingAPI;

public class DslParser {

    private BufferedReader reader;
    private int lineNumber = 0;
    private String currentLine = null;

    public DslQuestData parse(File dslFile) throws IOException {
        DslQuestData data = new DslQuestData();
        reader = new BufferedReader(new FileReader(dslFile));
        lineNumber = 0;

        try {
            String line;
            while ((line = readNextLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("@")) {
                    parseMetadata(line, data);
                }
                else if (line.startsWith(">quest")) {
                    parseQuest(line, data);
                }
            }
        } finally {
            reader.close();
        }

        return data;
    }

    private String readNextLine() throws IOException {
        String line = reader.readLine();
        if (line != null) {
            lineNumber++;
            currentLine = line;
            return line.trim();
        }
        currentLine = null;
        return null;
    }

    private void parseMetadata(String line, DslQuestData data) {
        String[] parts = line.substring(1)
            .split("\\s+", 2);
        if (parts.length == 2) {
            data.metadata.put(parts[0].trim(), parts[1].trim());
        } else if (parts.length == 1) {
            data.metadata.put(parts[0].trim(), "");
        }
    }

    private void parseQuest(String line, DslQuestData data) throws IOException {
        String questId = line.substring(6)
            .trim();
        if (questId.isEmpty()) {
            QuestingAPI.getLogger()
                .log(Level.WARN, "Quest without ID at line " + lineNumber);
            return;
        }

        DslQuest quest = new DslQuest(questId);
        quest.lineNumber = lineNumber;

        String propLine;
        boolean inChoiceReward = false;

        while ((propLine = peekNextMeaningfulLine()) != null) {
            if (!currentLine.startsWith(" ") && !currentLine.startsWith("\t")) {
                break;
            }

            readNextLine();

            if (inChoiceReward && (currentLine.startsWith("        ") || currentLine.startsWith("\t\t"))) {
                quest.choiceRewards.add(propLine);
                continue;
            } else if (inChoiceReward) {
                inChoiceReward = false;
            }

            if (propLine.contains(":")) {
                int colonIndex = propLine.indexOf(":");
                String key = propLine.substring(0, colonIndex)
                    .trim();
                String value = propLine.substring(colonIndex + 1)
                    .trim();

                if ("reward".equals(key) && "choice".equals(value)) {
                    quest.properties.put(key, value);
                    inChoiceReward = true;
                } else {
                    quest.properties.put(key, value);
                }
            }
        }

        data.quests.add(quest);
    }

    private String peekNextMeaningfulLine() throws IOException {
        reader.mark(10000);
        String line;

        while ((line = readNextLine()) != null) {
            if (!line.isEmpty() && !line.startsWith("#")) {
                reader.reset(); 
                lineNumber--;
                return line;
            }
            reader.mark(10000); 
        }

        return null;
    }
}
