package betterquesting.loaders.dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DslLayoutCalculator {

    public enum LayoutType {
        MANUAL,
        LINEAR,
        GRID,
        CIRCLE,
        SQUARE
    }

    private static final int DEFAULT_SPACING_X = 20;
    private static final int DEFAULT_SPACING_Y = 20;
    private static final int CIRCLE_RADIUS = 50;

    private LayoutType layoutType = LayoutType.MANUAL;
    private int spacingX = DEFAULT_SPACING_X;
    private int spacingY = DEFAULT_SPACING_Y;
    private int baseX = 0;
    private int baseY = 0;

    private static Map<String, Set<String>> usedPositions = new HashMap<>();

    public DslLayoutCalculator() {}

    public DslLayoutCalculator(LayoutType layoutType) {
        this.layoutType = layoutType;
    }

    public void setLayoutType(LayoutType layoutType) {
        this.layoutType = layoutType;
    }

    public void setSpacing(int x, int y) {
        this.spacingX = x;
        this.spacingY = y;
    }

    public void setBasePosition(int x, int y) {
        this.baseX = x;
        this.baseY = y;
    }

    public int[] calculatePosition(int questIndex, int totalQuests, Integer manualX, Integer manualY,
        String questLineName) {
        int x, y;

        if (layoutType == LayoutType.MANUAL && manualX != null && manualY != null) {
            x = manualX;
            y = manualY;
        } else {
            switch (layoutType) {
                case LINEAR:
                    x = baseX;
                    y = baseY - (questIndex * spacingY);
                    break;

                case GRID:
                    int cols = (int) Math.ceil(Math.sqrt(totalQuests));
                    int row = questIndex / cols;
                    int col = questIndex % cols;
                    x = baseX + (col * spacingX);
                    y = baseY - (row * spacingY);
                    break;

                case CIRCLE:
                    double angle = (2.0 * Math.PI * questIndex) / totalQuests;
                    x = baseX + (int) (CIRCLE_RADIUS * Math.cos(angle));
                    y = baseY + (int) (CIRCLE_RADIUS * Math.sin(angle));
                    break;

                case SQUARE:
                    int[] spiral = calculateSpiralPosition(questIndex);
                    x = baseX + (spiral[0] * spacingX);
                    y = baseY + (spiral[1] * spacingY);
                    break;

                case MANUAL:
                default:
                    x = baseX;
                    y = baseY - (questIndex * spacingY);
                    break;
            }
        }

        if (questLineName != null) {
            int[] adjusted = avoidOverlap(x, y, questLineName);
            x = adjusted[0];
            y = adjusted[1];
        }

        return new int[] { x, y };
    }

    private int[] calculateSpiralPosition(int index) {
        if (index == 0) return new int[] { 0, 0 };

        int x = 0, y = 0;
        int dx = 0, dy = -1;

        for (int i = 0; i < index; i++) {
            if (x == y || (x < 0 && x == -y) || (x > 0 && x == 1 - y)) {
                int temp = dx;
                dx = -dy;
                dy = temp;
            }
            x += dx;
            y += dy;
        }

        return new int[] { x, y };
    }

    private int[] avoidOverlap(int x, int y, String questLineName) {
        Set<String> positions = usedPositions.get(questLineName);
        if (positions == null) {
            positions = new HashSet<>();
            usedPositions.put(questLineName, positions);
        }

        String key = x + "," + y;
        int attempts = 0;
        int maxAttempts = 100;

        while (positions.contains(key) && attempts < maxAttempts) {
            attempts++;
            int offset = (attempts / 4 + 1) * 5;
            switch (attempts % 4) {
                case 0:
                    x += offset;
                    break;
                case 1:
                    y += offset;
                    break;
                case 2:
                    x -= offset;
                    break;
                case 3:
                    y -= offset;
                    break;
            }
            key = x + "," + y;
        }

        positions.add(key);
        return new int[] { x, y };
    }

    public static void clearTracking() {
        usedPositions.clear();
    }

    public static LayoutType parseLayoutType(String value) {
        if (value == null) return LayoutType.MANUAL;

        value = value.toUpperCase()
            .trim();
        try {
            return LayoutType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return LayoutType.MANUAL;
        }
    }
}
