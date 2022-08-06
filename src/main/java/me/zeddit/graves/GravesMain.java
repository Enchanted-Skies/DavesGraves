package me.zeddit.graves;

import com.mojang.datafixers.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
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
        final GraveLogger graveLogger = new GraveLogger(2000);
        final CoreProtectLogger logger = new CoreProtectLogger();
        final List<Pair<UUID, Location>> expiredGraves =new ArrayList<>(); // change to Collections.synchronizedList
        // if concurrent modification exception traced back to this list
        final GraveCreator creator = new GraveCreator(logger, graveLogger, expiredGraves);
        final PlayerListener listener = new PlayerListener(creator, graveLogger);
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
                                    graveLogger.logExpiry(UUID.fromString(it.getPersistentDataContainer().getOrDefault(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, "")));
                                    it.remove();
                                }
                            }.runTaskLater(GravesMain.getInstance(), (exp - System.currentTimeMillis()) / 50);
                            return false;
                        }).forEach(it -> {
                            graveLogger.logExpiry(UUID.fromString(it.getPersistentDataContainer().getOrDefault(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, "")));
                            it.remove();
                        });
            }
        }, this);
        final PluginCommand command = Objects.requireNonNull(getCommand("graves"));
        final GraveCommand graveCommand = new GraveCommand(creator, graveLogger,expiredGraves);
        this.getServer().getPluginManager().registerEvents(graveCommand, this);
        command.setExecutor(graveCommand);
        command.setTabCompleter(graveCommand);
    }

    @Override
    public void onDisable() {
        final int aborted = service.shutdownNow().size();
        if (aborted == 0) return;
        Bukkit.getLogger().warning("Aborted cleaning up " + aborted + (aborted == 1 ? " grave!" : " graves!"));
    }

    public ScheduledExecutorService getService() {
        return service;
    }

    public static long millisConvert(long duration) {
        final TimeUnit unit = TimeUnit
                .valueOf(Objects.requireNonNull(GravesMain.getInstance().getConfig()
                        .getString("durationUnit")).toUpperCase(Locale.ROOT));
        return TimeUnit.MILLISECONDS.convert(duration, unit);
    }
}
