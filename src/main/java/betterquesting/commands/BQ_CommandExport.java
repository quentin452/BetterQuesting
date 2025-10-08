package betterquesting.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;

import betterquesting.api.storage.BQ_Settings;

public class BQ_CommandExport extends CommandBase {

    @Override
    public String getCommandName() {
        return "bq_export";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bq_export <items|entities|fluids|oredicts|biomes|dimensions|all> [modid]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        String type = args[0].toLowerCase();
        String modFilter = args.length > 1 ? args[1].toLowerCase() : null;

        File exportDir = new File(BQ_Settings.defaultDir, "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        try {
            switch (type) {
                case "items":
                    exportItems(exportDir, modFilter, sender);
                    break;
                case "entities":
                    exportEntities(exportDir, modFilter, sender);
                    break;
                case "fluids":
                    exportFluids(exportDir, modFilter, sender);
                    break;
                case "oredicts":
                    exportOreDicts(exportDir, modFilter, sender);
                    break;
                case "biomes":
                    exportBiomes(exportDir, sender);
                    break;
                case "dimensions":
                    exportDimensions(exportDir, sender);
                    break;
                case "all":
                    exportItems(exportDir, modFilter, sender);
                    exportEntities(exportDir, modFilter, sender);
                    exportFluids(exportDir, modFilter, sender);
                    exportOreDicts(exportDir, modFilter, sender);
                    exportBiomes(exportDir, sender);
                    exportDimensions(exportDir, sender);
                    break;
                default:
                    sender.addChatMessage(new ChatComponentText("§cUnknown export type: " + type));
                    sender.addChatMessage(
                        new ChatComponentText("§eAvailable types: items, entities, fluids, oredicts, biomes, dimensions, all"));
                    return;
            }
        } catch (Exception e) {
            sender.addChatMessage(new ChatComponentText("§cError during export: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    private void exportItems(File exportDir, String modFilter, ICommandSender sender) throws IOException {
        File file = new File(exportDir, modFilter != null ? "items_" + modFilter + ".txt" : "items_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> itemList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting Item Export\n");
        writer.write("# Format: modid:item_name\n");
        writer.write("# Use these IDs in DSL quest files\n\n");

        for (Object obj : Item.itemRegistry.getKeys()) {
            String itemId = obj.toString();

            if (modFilter != null && !itemId.startsWith(modFilter + ":")) {
                continue;
            }

            Item item = (Item) Item.itemRegistry.getObject(itemId);
            if (item != null) {
                try {
                    ItemStack stack = new ItemStack(item, 1, 0);
                    String displayName = stack.getDisplayName();
                    itemList.add(String.format("%-50s # %s", itemId, displayName));
                    count++;
                } catch (Exception e) {
                    itemList.add(itemId + " # (Error getting display name)");
                    count++;
                }
            }
        }

        Collections.sort(itemList);
        for (String line : itemList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(new ChatComponentText("§aExported " + count + " items to " + file.getName()));
    }

    private void exportEntities(File exportDir, String modFilter, ICommandSender sender) throws IOException {
        File file = new File(exportDir, modFilter != null ? "entities_" + modFilter + ".txt" : "entities_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> entityList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting Entity Export\n");
        writer.write("# Format: modid:entity_name\n");
        writer.write("# Use these IDs in DSL quest files for kill tasks\n\n");

        for (Object obj : EntityList.stringToClassMapping.keySet()) {
            String entityId = obj.toString();

            if (modFilter != null && !entityId.startsWith(modFilter + ".")) {
                continue;
            }

            entityList.add(entityId);
            count++;
        }

        Collections.sort(entityList);
        for (String line : entityList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(new ChatComponentText("§aExported " + count + " entities to " + file.getName()));
    }

    private void exportFluids(File exportDir, String modFilter, ICommandSender sender) throws IOException {
        File file = new File(exportDir, modFilter != null ? "fluids_" + modFilter + ".txt" : "fluids_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> fluidList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting Fluid Export\n");
        writer.write("# Format: fluid_name\n");
        writer.write("# Use these IDs in DSL quest files for fluid tasks\n\n");

        for (Fluid fluid : FluidRegistry.getRegisteredFluids()
            .values()) {
            String fluidName = fluid.getName();

            if (modFilter != null && !fluidName.startsWith(modFilter)) {
                continue;
            }

            fluidList.add(String.format("%-30s # %s", fluidName, fluid.getLocalizedName(null)));
            count++;
        }

        Collections.sort(fluidList);
        for (String line : fluidList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(new ChatComponentText("§aExported " + count + " fluids to " + file.getName()));
    }

    private void exportOreDicts(File exportDir, String modFilter, ICommandSender sender) throws IOException {
        File file = new File(exportDir, modFilter != null ? "oredicts_" + modFilter + ".txt" : "oredicts_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> oreDictList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting OreDictionary Export\n");
        writer.write("# Format: oredict_name (item1, item2, ...)\n");
        writer.write("# Use these names in DSL quest files with ore: prefix\n\n");

        for (String oreName : OreDictionary.getOreNames()) {
            if (modFilter != null) {
                // Check if any of the ore entries match the mod filter
                boolean hasMatch = false;
                for (ItemStack stack : OreDictionary.getOres(oreName)) {
                    String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
                    if (itemId != null && itemId.startsWith(modFilter + ":")) {
                        hasMatch = true;
                        break;
                    }
                }
                if (!hasMatch) continue;
            }

            List<ItemStack> ores = OreDictionary.getOres(oreName);
            if (!ores.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-30s # ", oreName));

                int maxItems = Math.min(3, ores.size());
                for (int i = 0; i < maxItems; i++) {
                    ItemStack stack = ores.get(i);
                    String itemId = Item.itemRegistry.getNameForObject(stack.getItem());
                    if (itemId != null) {
                        sb.append(itemId);
                        if (i < maxItems - 1) sb.append(", ");
                    }
                }
                if (ores.size() > 3) {
                    sb.append(" (+" + (ores.size() - 3) + " more)");
                }

                oreDictList.add(sb.toString());
                count++;
            }
        }

        Collections.sort(oreDictList);
        for (String line : oreDictList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(
            new ChatComponentText("§aExported " + count + " ore dictionary entries to " + file.getName()));
    }

    private void exportBiomes(File exportDir, ICommandSender sender) throws IOException {
        File file = new File(exportDir, "biomes_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> biomeList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting Biome Export\n");
        writer.write("# Format: biome_id biome_name\n");
        writer.write("# Use these IDs in DSL quest files for visit tasks: visit dimension_id biome biome_id\n\n");

        net.minecraft.world.biome.BiomeGenBase[] biomes = net.minecraft.world.biome.BiomeGenBase.getBiomeGenArray();
        for (int i = 0; i < biomes.length; i++) {
            if (biomes[i] != null) {
                biomeList.add(String.format("%-5d # %s", i, biomes[i].biomeName));
                count++;
            }
        }

        Collections.sort(biomeList);
        for (String line : biomeList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(new ChatComponentText("§aExported " + count + " biomes to " + file.getName()));
    }

    private void exportDimensions(File exportDir, ICommandSender sender) throws IOException {
        File file = new File(exportDir, "dimensions_all.txt");
        FileWriter writer = new FileWriter(file);

        List<String> dimensionList = new ArrayList<>();
        int count = 0;

        writer.write("# BetterQuesting Dimension Export\n");
        writer.write("# Format: dimension_id dimension_name\n");
        writer.write("# Use these IDs in DSL quest files for visit tasks: visit dimension_id\n\n");
        writer.write("# Standard Minecraft dimensions:\n");
        writer.write("#  0 = Overworld\n");
        writer.write("# -1 = Nether\n");
        writer.write("#  1 = The End\n");
        writer.write("# Modded dimensions typically use IDs starting from 400+\n\n");

        Integer[] dimensionIds = net.minecraftforge.common.DimensionManager.getIDs();
        for (int dimId : dimensionIds) {
            try {
                net.minecraft.world.WorldProvider provider = net.minecraftforge.common.DimensionManager
                    .createProviderFor(dimId);
                String dimName = provider != null ? provider.getDimensionName() : "Unknown";
                dimensionList.add(String.format("%-5d # %s", dimId, dimName));
                count++;
            } catch (Exception e) {
                dimensionList.add(String.format("%-5d # (Error getting name)", dimId));
                count++;
            }
        }

        Collections.sort(dimensionList);
        for (String line : dimensionList) {
            writer.write(line + "\n");
        }

        writer.close();
        sender.addChatMessage(new ChatComponentText("§aExported " + count + " dimensions to " + file.getName()));
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                "items",
                "entities",
                "fluids",
                "oredicts",
                "biomes",
                "dimensions",
                "all");
        }
        return null;
    }
}
