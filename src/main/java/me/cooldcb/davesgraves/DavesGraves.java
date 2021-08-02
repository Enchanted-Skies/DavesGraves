package me.cooldcb.davesgraves;

import me.cooldcb.davesgraves.commands.GravesCmd;
import me.cooldcb.davesgraves.datamanager.ConfigManager;
import me.cooldcb.davesgraves.datamanager.DataManager;
import me.cooldcb.davesgraves.storage.Storage;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class DavesGraves extends JavaPlugin implements Listener {
    private static DavesGraves plugin;
    public static ConfigManager configManager;
    public static DataManager dataManager;

    private void setThreadIOName() {
        Storage.SERVICE.submit(() -> Thread.currentThread().setName("DavesGraves IO Thread"));
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

    public static DavesGraves getInstance() {
        return plugin;
    }

    private void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
