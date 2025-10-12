package net.stacking.simpleStacker.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * Prevents players from using stacked damageable items
 * Shows error message: "You must take one item out of the stack and use that!"
 */
public class DurabilityUsageListener implements Listener {

    private static final String ERROR_MESSAGE = ChatColor.RED + "You must take one item out of the stack and use that!";

    /**
     * Check if an item is damageable (has durability)
     */
    private boolean isDamageable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getType().getMaxDurability() > 0;
    }

    /**
     * Check if item is stacked (amount > 1) and damageable
     */
    private boolean isStackedDamageable(ItemStack item) {
        return item != null && item.getAmount() > 1 && isDamageable(item);
    }

    /**
     * Prevent using tools (pickaxe, axe, shovel, hoe, etc.) when stacked
     * Handles right-click and left-click actions
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if player is trying to use a stacked damageable item
        if (isStackedDamageable(item)) {
            Action action = event.getAction();

            // Check if action would use the item
            boolean wouldUseItem = false;

            // Right-click actions that use durability
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                Material type = item.getType();
                // Tools that use durability on right-click
                if (type.name().endsWith("_HOE") || // Tilling
                        type.name().endsWith("_AXE") || // Stripping logs
                        type.name().endsWith("_SHOVEL") || // Path creation
                        type == Material.FLINT_AND_STEEL ||
                        type == Material.SHEARS) {
                    wouldUseItem = true;
                }
            }

            // Left-click actions (breaking blocks handled in BlockBreakEvent)
            // But we prevent the swing animation here
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
                if (item.getType().name().endsWith("_PICKAXE") ||
                        item.getType().name().endsWith("_AXE") ||
                        item.getType().name().endsWith("_SHOVEL") ||
                        item.getType().name().endsWith("_SWORD") ||
                        item.getType().name().endsWith("_HOE")) {
                    wouldUseItem = true;
                }
            }

            if (wouldUseItem) {
                event.setCancelled(true);
                player.sendMessage(ERROR_MESSAGE);
            }
        }
    }

    /**
     * Prevent breaking blocks with stacked tools
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isStackedDamageable(item)) {
            event.setCancelled(true);
            player.sendMessage(ERROR_MESSAGE);
        }
    }

    /**
     * Prevent attacking entities with stacked weapons/tools
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (isStackedDamageable(item)) {
            event.setCancelled(true);
            player.sendMessage(ERROR_MESSAGE);
        }
    }

    /**
     * Catch-all: Prevent any item damage when stacked
     * This catches edge cases like armor taking damage, fishing rods, etc.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();

        if (item != null && item.getAmount() > 1 && isDamageable(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ERROR_MESSAGE);
        }
    }
}
