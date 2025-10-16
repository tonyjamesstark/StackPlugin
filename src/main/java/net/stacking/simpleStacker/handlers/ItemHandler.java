package net.stacking.simpleStacker.handlers;

import net.stacking.simpleStacker.SimpleStacker;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ItemHandler {

    private final Map<Material, Integer> targets = new HashMap<>();

    public void loadStackSizes() {
        Logger log = SimpleStacker.getInstance().getLogger();
        ConfigurationSection itemsSection = SimpleStacker.getInstance().getConfig().getConfigurationSection("items");

        if (itemsSection == null) {
            log.warning("No items section found in config!");
            return;
        }

        int success = 0, skippedDamageable = 0, invalid = 0;

        for (String materialName : itemsSection.getKeys(false)) {
            int stackSize = itemsSection.getInt(materialName);

            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                log.fine("Unknown material (skipping): " + materialName);
                invalid++;
                continue;
            }

            if (stackSize < 1 || stackSize > 99) {
                log.fine("Invalid stack size " + stackSize + " for " + material + " (must be 1..99) - skipping");
                invalid++;
                continue;
            }

            if (material.getMaxDurability() > 0 && stackSize > 1) {
                log.fine("Skipping damageable item " + material + " with size " + stackSize + " (MAX_STACK_SIZE>1 conflicts with durability).");
                skippedDamageable++;
                continue;
            }

            targets.put(material, stackSize);
            success++;
        }

        log.info("Loaded " + success + " stack rules, skipped " + skippedDamageable + " damageable, " + invalid + " invalid.");
    }

    public Map<Material, Integer> getTargets() {
        return targets;
    }
}
