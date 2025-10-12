package net.stacking.simpleStacker.listeners;

import net.stacking.simpleStacker.SimpleStacker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class StackingListener implements Listener {
    private final Map<Material, Integer> targets;

    public StackingListener(Map<Material, Integer> targets) {
        this.targets = targets;
    }

    private boolean isDamageable(Material mat) {
        return mat.getMaxDurability() > 0;
    }

    private void apply(ItemStack stack) {
        if (stack == null) return;
        Integer target = targets.get(stack.getType());
        if (target == null) return;
        if (target < 1 || target > 99) return;
        if (isDamageable(stack.getType()) && target > 1) {
            // Skip damageable items > 1 (conflicts with MAX_DAMAGE component)
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        try {
            if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != target) {
                meta.setMaxStackSize(target);
                stack.setItemMeta(meta);
            }
        } catch (Throwable t) {
            try {
                SimpleStacker.getInstance().getLogger().fine(
                    "Failed to apply max stack to " + stack.getType() + ": " + t.getMessage()
                );
            } catch (Throwable ignored) {
            }
        }
    }

    private void applyAll(Inventory inv) {
        if (inv == null) return;
        for (ItemStack it : inv.getContents()) apply(it);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(PrepareItemCraftEvent e) {
        ItemStack res = e.getInventory().getResult();
        if (res != null) apply(res);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        Bukkit.getScheduler().runTask(SimpleStacker.getInstance(), () -> {
            applyAll(e.getWhoClicked().getOpenInventory().getTopInventory());
            if (e.getWhoClicked() instanceof Player p) {
                applyAll(p.getInventory());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent e) {
        ItemStack cursor = e.getCursor();
        if (cursor != null) apply(cursor);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        applyAll(p.getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntityType() != EntityType.ITEM) return;
        Item item = (Item) e.getEntity();
        apply(item.getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        apply(e.getItem().getItemStack());
    }
}
