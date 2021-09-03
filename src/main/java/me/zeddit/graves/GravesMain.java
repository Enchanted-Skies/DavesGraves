package me.zeddit.graves;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GravesMain extends JavaPlugin {
    private static GravesMain instance;
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public static GravesMain getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        service.submit(() -> Thread.currentThread().setName("Graves Async IO Thread 1"));
        saveDefaultConfig();
        final CoreProtectLogger logger = new CoreProtectLogger();
        final GraveCreator creator = new GraveCreator(logger);
        final DeathListener listener = new DeathListener(creator);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onWorldLoad(WorldLoadEvent e) {
                e.getWorld().getEntities().stream()
                        .filter(it -> it instanceof ArmorStand)
                        .filter(it -> {
                            long exp = it.getPersistentDataContainer().getOrDefault(GraveKeys.EXPIRY.toKey(), PersistentDataType.LONG, -2L);
                            if (exp == -1) {
                                return false;
                            }
                            if (exp == -2 || System.currentTimeMillis() >= exp) {
                                return true; // invalid so remove in foreach
                            }
                            //This means they need to be collected at their expiry time
                            service.schedule(it::remove,exp - System.currentTimeMillis() , TimeUnit.MILLISECONDS);
                            return false;
                        }).forEach(Entity::remove);
            }
        }, this);
        Objects.requireNonNull(getCommand("graves")).setExecutor(new GraveCommand(creator));
    }

    @Override
    public void onDisable() {
    }

    public ScheduledExecutorService getService() {
        return service;
    }
}
