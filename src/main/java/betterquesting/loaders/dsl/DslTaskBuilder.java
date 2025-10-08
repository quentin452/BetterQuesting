package betterquesting.loaders.dsl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.Fluid;

import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api.utils.BigItemStack;
import bq_standard.tasks.*;

public class DslTaskBuilder {

    private static final Pattern QUOTED_PATTERN = Pattern.compile("\"([^\"]*)\"");
    private DslValidator validator;
    private int currentLine;

    public DslTaskBuilder(DslValidator validator) {
        this.validator = validator;
    }

    public void buildTask(String taskDef, IQuest quest, int lineNumber) {
        this.currentLine = lineNumber;
        try {
            String[] parts = taskDef.split("\\s+");
            if (parts.length == 0) {
                return;
            }

            String taskType = parts[0].toLowerCase();

            if (!validator.validateTaskType(taskType, currentLine, taskDef)) {
                return;
            }

            ITask task = null;

            switch (taskType) {
                case "checkbox":
                    task = buildCheckboxTask(taskDef);
                    break;
                case "craft":
                    task = buildCraftingTask(taskDef);
                    break;
                case "kill":
                    task = buildHuntTask(taskDef);
                    break;
                case "collect":
                    task = buildRetrievalTask(taskDef);
                    break;
                case "visit":
                    task = buildLocationTask(taskDef);
                    break;
                case "interact":
                    task = buildInteractTask(taskDef);
                    break;
                case "break":
                case "blockbreak":
                    task = buildBlockBreakTask(taskDef);
                    break;
                case "fluid":
                    task = buildFluidTask(taskDef);
                    break;
                case "meeting":
                    task = buildMeetingTask(taskDef);
                    break;
                case "retrieval":
                    task = buildRetrievalTask(taskDef);
                    break;
                case "optional_retrieval":
                    task = buildOptionalRetrievalTask(taskDef);
                    break;
                case "xp":
                    task = buildXPTask(taskDef);
                    break;
                case "scoreboard":
                    task = buildScoreboardTask(taskDef);
                    break;
                default:
                    System.out.println("[BQ] WARNING: Unknown task type: " + taskType);
                    return;
            }

            if (task != null) {
                int taskId = quest.getTasks()
                    .nextID();
                quest.getTasks()
                    .add(taskId, task);
            }
        } catch (Exception e) {
            System.err.println("[BQ] Error building task: " + taskDef);
            e.printStackTrace();
        }
    }

    private ITask buildCheckboxTask(String taskDef) {
        return new TaskCheckbox();
    }

    private ITask buildCraftingTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Craft task requires item ID and count: craft \"item_id\" count",
                taskDef);
            return null;
        }

        String itemId = extractQuoted(taskDef);
        if (!validator.validateItemId(itemId, currentLine, taskDef)) {
            return null; 
        }

        int count;
        try {
            count = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid count '" + parts[parts.length - 1] + "' - must be a number",
                taskDef);
            return null;
        }

        TaskCrafting task = new TaskCrafting();
        ItemStack stack = getItemStack(itemId, count);
        if (stack != null) {
            task.requiredItems.add(new BigItemStack(stack));
        }

        return task;
    }

    private ITask buildHuntTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            return null;
        }

        String entityId = extractQuoted(taskDef);
        int count = Integer.parseInt(parts[parts.length - 1]);

        TaskHunt task = new TaskHunt();
        task.idName = entityId;
        task.required = count;

        return task;
    }

    private ITask buildRetrievalTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Collect task requires item ID and count: collect \"item_id\" count",
                taskDef);
            return null;
        }

        String itemId = extractQuoted(taskDef);
        if (!validator.validateItemId(itemId, currentLine, taskDef)) {
            return null;
        }

        int count;
        try {
            count = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid count '" + parts[parts.length - 1] + "' - must be a number",
                taskDef);
            return null;
        }

        TaskRetrieval task = new TaskRetrieval();
        ItemStack stack = getItemStack(itemId, count);
        if (stack != null) {
            task.requiredItems.add(new BigItemStack(stack));
        }

        return task;
    }

    private ITask buildOptionalRetrievalTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Optional retrieval task requires item ID and count: optional_retrieval \"item_id\" count",
                taskDef);
            return null;
        }

        String itemId = extractQuoted(taskDef);
        if (!validator.validateItemId(itemId, currentLine, taskDef)) {
            return null;
        }

        int count;
        try {
            count = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid count '" + parts[parts.length - 1] + "' - must be a number",
                taskDef);
            return null;
        }

        TaskOptionalRetrieval task = new TaskOptionalRetrieval();
        ItemStack stack = getItemStack(itemId, count);
        if (stack != null) {
            task.requiredItems.add(new BigItemStack(stack));
        }

        return task;
    }

    private ITask buildLocationTask(String taskDef) {
        TaskLocation task = new TaskLocation();

        String[] parts = taskDef.split("\\s+");
        if (parts.length < 2) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Visit task requires dimension ID: visit <dimension_id>",
                taskDef);
            return null;
        }

        try {
            task.dim = Integer.parseInt(parts[1]);
            validator.validateDimensionId(task.dim, currentLine, taskDef);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid dimension ID '" + parts[1] + "' - must be a number (0=Overworld, -1=Nether, 1=End, etc.)",
                taskDef);
            task.dim = 0; 
        }

        if (parts.length >= 5 && !parts[2].equals("biome") && !parts[2].equals("range")) {
            try {
                task.x = Integer.parseInt(parts[2]);
                task.y = Integer.parseInt(parts[3]);
                task.z = Integer.parseInt(parts[4]);
                task.visible = true;
            } catch (NumberFormatException e) {
            }
        }

        for (int i = 2; i < parts.length; i++) {
            if ("biome".equals(parts[i]) && i + 1 < parts.length) {
                String biomeParam = parts[i + 1].replace("\"", "");
                
                // Try to parse as integer ID first
                try {
                    task.biome = Integer.parseInt(biomeParam);
                } catch (NumberFormatException e) {
                    // If not a number, try to get ID by name
                    task.biome = getBiomeIdByName(biomeParam);
                    if (task.biome == -1) {
                        validator.addError(
                            DslError.Severity.WARNING,
                            currentLine,
                            "Unknown biome '" + biomeParam + "' - please use biome ID instead of name for reliability",
                            taskDef);
                    }
                }
            }
            if ("range".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    task.range = Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                }
            }
        }

        return task;
    }
    
    private int getBiomeIdByName(String biomeName) {
        try {
            Class<?> biomeGenBase = Class.forName("net.minecraft.world.biome.BiomeGenBase");
            Object[] biomeList = (Object[]) biomeGenBase.getField("biomeList").get(null);
            
            for (int i = 0; i < biomeList.length; i++) {
                if (biomeList[i] != null) {
                    try {
                        java.lang.reflect.Field biomeNameField = biomeList[i].getClass().getField("biomeName");
                        String name = (String) biomeNameField.get(biomeList[i]);
                        if (name != null && name.equalsIgnoreCase(biomeName)) {
                            System.out.println("[BQ DSL] Found biome '" + biomeName + "' with ID: " + i);
                            return i;
                        }
                    } catch (NoSuchFieldException e) {
                        continue;
                    }
                }
            }
            
            System.err.println("[BQ DSL] Biome '" + biomeName + "' not found in biome list");
        } catch (Exception e) {
            System.err.println("[BQ DSL] Error getting biome by name: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private ITask buildInteractTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Interact task requires item ID and count: interact \"item_id\" count",
                taskDef);
            return null;
        }

        String itemId = extractQuoted(taskDef);
        if (!validator.validateItemId(itemId, currentLine, taskDef)) {
            return null; 
        }

        TaskInteractItem task = new TaskInteractItem();

        ItemStack stack = getItemStack(itemId, 1);
        if (stack != null) {
            task.targetItem = new BigItemStack(stack);
        }

        for (int i = 0; i < parts.length; i++) {
            try {
                task.required = Integer.parseInt(parts[i]);
                break;
            } catch (NumberFormatException e) {
            }
        }

        return task;
    }

    private ITask buildBlockBreakTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Block break task requires block ID and count: blockbreak \"block_id\" count",
                taskDef);
            return null;
        }

        String blockId = extractQuoted(taskDef);
        if (!validator.validateItemId(blockId, currentLine, taskDef)) {
            return null;
        }

        int count;
        try {
            count = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid count '" + parts[parts.length - 1] + "' - must be a number",
                taskDef);
            return null;
        }

        TaskBlockBreak task = new TaskBlockBreak();
        task.blockTypes.clear(); // Clear the default entry
        
        ItemStack stack = getItemStack(blockId, count);
        if (stack != null) {
            // Create NbtBlockType from the ItemStack
            bq_standard.NbtBlockType blockType = new bq_standard.NbtBlockType(
                net.minecraft.block.Block.getBlockFromItem(stack.getItem()),
                stack.getItemDamage());
            blockType.n = count;
            task.blockTypes.add(blockType);
        }

        return task;
    }

    private ITask buildFluidTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Fluid task requires fluid name and amount: fluid \"fluid_name\" amount",
                taskDef);
            return null;
        }

        String fluidName = extractQuoted(taskDef);
        if (fluidName.isEmpty()) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Fluid task requires a valid fluid name in quotes",
                taskDef);
            return null;
        }

        int amount;
        try {
            amount = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            validator.addError(
                DslError.Severity.ERROR,
                currentLine,
                "Invalid amount '" + parts[parts.length - 1] + "' - must be a number",
                taskDef);
            return null;
        }

        TaskFluid task = new TaskFluid();

        Fluid fluid = FluidRegistry.getFluid(fluidName);
        if (fluid != null) {
            FluidStack fluidStack = new FluidStack(fluid, amount);
            task.requiredFluids.add(fluidStack);
        } else {
            validator.addError(
                DslError.Severity.WARNING,
                currentLine,
                "Unknown fluid '" + fluidName + "' - task may not work correctly",
                taskDef);
        }

        return task;
    }

    private ITask buildMeetingTask(String taskDef) {
        String entityId = extractQuoted(taskDef);

        TaskMeeting task = new TaskMeeting();
        task.idName = entityId;

        return task;
    }

    private ITask buildXPTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 2) {
            return null;
        }

        int amount = Integer.parseInt(parts[1]);

        TaskXP task = new TaskXP();
        task.amount = amount;
        task.consume = false;
        task.levels = false;

        return task;
    }

    private ITask buildScoreboardTask(String taskDef) {
        String[] parts = taskDef.split("\\s+");
        if (parts.length < 3) {
            return null;
        }

        String objective = parts[1];
        int score = Integer.parseInt(parts[2]);

        TaskScoreboard task = new TaskScoreboard();
        task.scoreName = objective;
        task.target = score;
        task.operation = TaskScoreboard.ScoreOperation.MORE_OR_EQUAL;

        return task;
    }

    private String extractQuoted(String text) {
        Matcher matcher = QUOTED_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private ItemStack getItemStack(String itemId, int count) {
        try {
            Item item = (Item) Item.itemRegistry.getObject(itemId);
            if (item != null) {
                return new ItemStack(item, count, 0);
            }
        } catch (Exception e) {
            System.out.println("[BQ] WARNING: Could not find item: " + itemId);
        }
        return null;
    }
}
