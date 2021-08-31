package me.zeddit.graves;

import org.bukkit.plugin.java.JavaPlugin;

public final class GravesMain extends JavaPlugin {
    public static JavaPlugin instance;

    public static JavaPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
    }
}
