package me.zeddit.graves;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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
        final GraveLogger graveLogger = GraveLogger.init(2000);
        final GraveCreator creator = new GraveCreator();
        final PlayerListener listener = new PlayerListener(creator, graveLogger);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getPluginManager().registerEvents(creator,this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityAdd(EntityAddToWorldEvent e) {
                Stream.of(e.getEntity())
                        .filter(it -> it instanceof ArmorStand)
                        .map(it -> (ArmorStand) it)
                        .map(it -> {
                            long exp = it.getPersistentDataContainer().getOrDefault(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, Long.MAX_VALUE);
                            if (exp == -1) {
                                return new Pair<>(1, it);
                            }
                            if (System.currentTimeMillis() >= exp) {
                                return new Pair<>(-1,it); // should be removed
                            }
                            if (isLegacyGrave(it)) return new Pair<>(0,it);
                            //This means they need to be collected at their expiry time
                            if (!Grave.getActiveGraves().contains(it) && exp != Long.MAX_VALUE) {
                                new Grave(it);
                            }
                            return new Pair<>(1,it);
                        }).forEach(it -> {
                            if (it.getFirst() == 1) return;
                            it.getSecond().remove();
                            if (it.getFirst() == 0) {
                                graveLogger.logRaw("Removed a legacy grave!");
                            } else {
                                final String owner = it.getSecond().getPersistentDataContainer().get(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING);
                                if (owner == null) {
                                    graveLogger.logRaw("Removed grave with unknown owner!");
                                } else {
                                    graveLogger.logExpiry(UUID.fromString(owner));
                                }
                            }
                        });
            }
        }, this);
        final PluginCommand command = Objects.requireNonNull(getCommand("graves"));
        final GraveCommand graveCommand = new GraveCommand(creator, graveLogger);
        this.getServer().getPluginManager().registerEvents(graveCommand, this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onConfigReload(ConfigReloadEvent e) {
                instance.reloadConfig();
            }
        }, this);
        command.setExecutor(graveCommand);
        command.setTabCompleter(graveCommand);
    }

    private boolean isLegacyGrave(ArmorStand it) {
        if (it.getPersistentDataContainer().has(new NamespacedKey("davesgraves", "grave"), PersistentDataType.STRING)) {
            return true;
        }
        return it.getPersistentDataContainer().has(new NamespacedKey("enchantedgraves", "grave"), PersistentDataType.STRING);
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
