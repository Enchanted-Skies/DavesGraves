package org.enchantedskies.enchantedgraves;

import org.enchantedskies.enchantedgraves.commands.GravesCmd;
import org.enchantedskies.enchantedgraves.datamanager.ConfigManager;
import org.enchantedskies.enchantedgraves.datamanager.DataManager;
import org.enchantedskies.enchantedgraves.storage.Storage;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class EnchantedGraves extends JavaPlugin implements Listener {
    private static EnchantedGraves plugin;
    public static ConfigManager configManager;
    public static DataManager dataManager;

    private void setThreadIOName() {
        Storage.SERVICE.submit(() -> Thread.currentThread().setName("EnchantedGraves IO Thread"));
    }

    @Override
    public void onEnable() {
        plugin = this;
        setThreadIOName();
        configManager = new ConfigManager();
        dataManager = new DataManager();
        dataManager.initAsync((successful) -> {
            Listener[] listeners = new Listener[] {
                new PlayerEvents()
            };
            registerEvents(listeners);

            getCommand("graves").setExecutor(new GravesCmd());

            dataManager.loadGraves();
        });
    }

    public static EnchantedGraves getInstance() {
        return plugin;
    }

    private void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
