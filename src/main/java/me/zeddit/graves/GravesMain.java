package me.zeddit.graves;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
                            long exp = it.getPersistentDataContainer().getOrDefault(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, -2L);
                            if (exp == -1) {
                                return false;
                            }
                            if (exp == -2 || System.currentTimeMillis() >= exp) {
                                return true; // invalid so remove in foreach
                            }
                            //This means they need to be collected at their expiry time
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    it.remove();
                                }
                            }.runTaskLater(GravesMain.getInstance(), (exp - System.currentTimeMillis()) / 50);
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
