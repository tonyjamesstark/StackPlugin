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

    private void apply(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return;

        Integer target = targets.get(stack.getType());
        if (target == null) return;
        if (target < 1 || target > 99) return;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        try {
            if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != target) {
                meta.setMaxStackSize(target);
                stack.setItemMeta(meta);
            }
        } catch (Throwable t) {
            // Silently fail
        }
    }

    private void applyAll(Inventory inv) {
        if (inv == null) return;
        try {
            for (ItemStack it : inv.getContents()) {
                if (it != null) apply(it);
            }
        } catch (Exception e) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(PrepareItemCraftEvent e) {
        try {
            ItemStack res = e.getInventory().getResult();
            if (res != null) apply(res);
        } catch (Exception ex) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        try {
            Bukkit.getScheduler().runTask(SimpleStacker.getInstance(), () -> {
                try {
                    applyAll(e.getWhoClicked().getOpenInventory().getTopInventory());
                    if (e.getWhoClicked() instanceof Player p) {
                        applyAll(p.getInventory());
                    }
                } catch (Exception ex) {
                    // Fail silently
                }
            });
        } catch (Exception ex) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreative(InventoryCreativeEvent e) {
        try {
            ItemStack cursor = e.getCursor();
            if (cursor != null) apply(cursor);
        } catch (Exception ex) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        try {
            Player p = e.getPlayer();
            Bukkit.getScheduler().runTask(SimpleStacker.getInstance(), () -> {
                try {
                    applyAll(p.getInventory());
                } catch (Exception ex) {
                    // Fail silently
                }
            });
        } catch (Exception ex) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent e) {
        try {
            if (e.getEntityType() != EntityType.ITEM) return;
            Item item = (Item) e.getEntity();
            apply(item.getItemStack());
        } catch (Exception ex) {
            // Fail silently
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        try {
            apply(e.getItem().getItemStack());
        } catch (Exception ex) {
            // Fail silently
        }
    }
}
