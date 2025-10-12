package net.stacking.simpleStacker.commands;

import net.stacking.simpleStacker.SimpleStacker;
import net.stacking.simpleStacker.handlers.ItemHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class StackCommand implements CommandExecutor {

    private final SimpleStacker plugin;

    public StackCommand(SimpleStacker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("simplestacker.use")) {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "You don't have permission to use this command!");
            return true;
        }

        int stackedCount = stackInventory(player);

        if (stackedCount > 0) {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✓ " + ChatColor.LIGHT_PURPLE + "Successfully stacked your items!");
        } else {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "✗ " + ChatColor.LIGHT_PURPLE + "No items to stack!");
        }

        return true;
    }

    private int stackInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        Map<String, ItemStackGroup> groups = new HashMap<>();
        int stackedCount = 0;

        // Build groups with strict key: material + exact name + exact lore [+ shulker color + normalized contents]
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            String key = buildStrictKey(item);
            groups.computeIfAbsent(key, k -> new ItemStackGroup(item)).addAmount(item.getAmount());
        }

        // Clear inventory
        inventory.clear();

        // Re-create stacks using configured per-material max, and write the max onto the new stacks
        ItemHandler handler = plugin.getItemHandler();
        Map<Material, Integer> targets = handler.getTargets();

        for (ItemStackGroup group : groups.values()) {
            ItemStack template = group.getTemplate();
            int total = group.getTotalAmount();

            // Use configured target if present, else current stack max
            int targetMax = targets.getOrDefault(template.getType(), template.getMaxStackSize());
            // Clamp to 1..99 for safety
            if (targetMax < 1) targetMax = 1;
            if (targetMax > 99) targetMax = 99;

            while (total > 0) {
                int stackSize = Math.min(total, targetMax);
                ItemStack stack = template.clone();
                stack.setAmount(stackSize);

                // Apply the MAX_STACK_SIZE component to this new stack explicitly
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    try {
                        // Damageable items cannot have >1; targets skipped already, but keep safe:
                        if (!(template.getType().getMaxDurability() > 0 && targetMax > 1)) {
                            meta.setMaxStackSize(targetMax);
                        }
                        stack.setItemMeta(meta);
                    } catch (Throwable ignored) {
                        // If some meta rejects, just skip applying
                    }
                }

                // Add to inventory
                HashMap<Integer, ItemStack> leftover = inventory.addItem(stack);
                if (!leftover.isEmpty()) {
                    // Drop if cannot fit
                    player.getWorld().dropItem(player.getLocation(), leftover.values().iterator().next());
                }

                total -= stackSize;
                stackedCount++;
            }
        }

        player.updateInventory();
        return stackedCount;
    }

    // Strict key: material + exact display name + exact lore (legacy API only)
    // If shulker: also require same color (type name) and contents signature (order-independent)
    private String buildStrictKey(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return "air";

        StringBuilder key = new StringBuilder(item.getType().name());
        ItemMeta meta = item.getItemMeta();

        // Exact display name (legacy String)
        String name = "";
        if (meta != null && meta.hasDisplayName()) {
            name = meta.getDisplayName();
        }
        key.append("|name=").append(name);

        // Exact lore (legacy String list)
        String loreSig = "";
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                loreSig = lore.toString();
            }
        }
        key.append("|lore=").append(loreSig);

        // Shulker-specific: color/type already in material name. Also include contents signature
        if (isShulkerBox(item)) {
            key.append("|shulker=").append(buildShulkerContentsSignature(item));
        }

        return key.toString();
    }

    private boolean isShulkerBox(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_SHULKER_BOX");
    }

    // Order-independent contents signature:
    // Combine identical inner items (by material + name + lore) and sum their amounts; sort keys for stable signature.
    private String buildShulkerContentsSignature(ItemStack shulkerItem) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta)) return "empty";
        BlockStateMeta meta = (BlockStateMeta) shulkerItem.getItemMeta();
        if (!(meta.getBlockState() instanceof ShulkerBox)) return "empty";

        ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
        Map<String, Integer> contentCounts = new HashMap<>();

        for (ItemStack content : shulker.getInventory().getContents()) {
            if (content == null || content.getType() == Material.AIR) continue;
            String innerKey = buildNonShulkerStrictKey(content);
            contentCounts.merge(innerKey, content.getAmount(), Integer::sum);
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : contentCounts.entrySet()) {
            parts.add(e.getKey() + "x" + e.getValue());
        }
        parts.sort(Comparator.naturalOrder());

        return String.join(",", parts);
    }

    // Non-shulker strict key (material + exact name + exact lore) - legacy API only
    private String buildNonShulkerStrictKey(ItemStack item) {
        StringBuilder key = new StringBuilder(item.getType().name());
        ItemMeta meta = item.getItemMeta();

        String name = "";
        if (meta != null && meta.hasDisplayName()) {
            name = meta.getDisplayName();
        }
        key.append("|name=").append(name);

        String loreSig = "";
        if (meta != null && meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                loreSig = lore.toString();
            }
        }
        key.append("|lore=").append(loreSig);

        return key.toString();
    }

    private static class ItemStackGroup {
        private final ItemStack template;
        private int totalAmount;

        ItemStackGroup(ItemStack template) {
            this.template = template.clone();
            this.template.setAmount(1);
            this.totalAmount = 0;
        }

        void addAmount(int amount) {
            this.totalAmount += amount;
        }

        ItemStack getTemplate() {
            return template;
        }

        int getTotalAmount() {
            return totalAmount;
        }
    }
}
