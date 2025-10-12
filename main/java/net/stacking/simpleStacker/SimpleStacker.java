package net.stacking.simpleStacker;

import net.stacking.simpleStacker.commands.StackCommand;
import net.stacking.simpleStacker.handlers.ItemHandler;
import net.stacking.simpleStacker.listeners.DurabilityUsageListener;
import net.stacking.simpleStacker.listeners.StackingListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SimpleStacker extends JavaPlugin {

    private static SimpleStacker instance;
    private ItemHandler itemHandler;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Load material stack sizes from config
        itemHandler = new ItemHandler();
        itemHandler.loadStackSizes();

        // Register listener to apply max stack size on stacks
        getServer().getPluginManager().registerEvents(
                new StackingListener(itemHandler.getTargets()),
                this
        );

        // NEW: Register listener to prevent using stacked damageable items
        getServer().getPluginManager().registerEvents(
                new DurabilityUsageListener(),
                this
        );

        // Register command executor from plugin.yml
        Objects.requireNonNull(getCommand("stack"), "Command 'stack' not found in plugin.yml")
                .setExecutor(new StackCommand(this));

        getLogger().info("SimpleStacker is enabled (SAFE MODE - Durability stacking enabled).");
        getLogger().info("Rules loaded: " + itemHandler.getTargets().size());
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleStacker has been disabled!");
    }

    public static SimpleStacker getInstance() {
        return instance;
    }

    public ItemHandler getItemHandler() {
        return itemHandler;
    }
}
