package me.cooldcb.davesgraves.datamanager;

import me.cooldcb.davesgraves.DavesGraves;
import me.cooldcb.davesgraves.Grave;
import me.cooldcb.davesgraves.storage.Storage;
import me.cooldcb.davesgraves.storage.YmlStorage;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.function.Consumer;

public class DataManager {
    private Storage storage;

    public void initAsync(Consumer<Boolean> onComplete) {
        Storage.SERVICE.submit(() -> {
            storage = new YmlStorage();
            final boolean init = storage.init();
            new BukkitRunnable() {
                @Override
                public void run() {
                    onComplete.accept(init);
                }
            }.runTask(DavesGraves.getInstance());
        });
    }

    public void getGrave(UUID playerUUID, int graveID, Consumer<Grave> onComplete) {
        Storage.SERVICE.submit(() -> {
            final Grave grave = new Grave(playerUUID, graveID);
            new BukkitRunnable() {
                @Override
                public void run() {
                    onComplete.accept(grave);
                }
            }.runTask(DavesGraves.getInstance());
        });
    }

    public void saveGrave(Grave grave) {
        Storage.SERVICE.submit(() -> {
            UUID playerUUID = grave.getPlayerUUID();
            if (getGraveCount(playerUUID) >= DavesGraves.configManager.getMaxGraves()) {
                int lowestID = getLowestGraveID(playerUUID);
                if (lowestID != -1) breakGrave(grave);
            }
            storage.saveGrave(grave);
        });
    }

    public void breakGrave(Grave grave) {
        Storage.SERVICE.submit(() -> storage.deleteGrave(grave.getPlayerUUID(), grave.getGraveID(), null));
    }

    public void breakGrave(Grave grave, Player collector) {
        Storage.SERVICE.submit(() -> storage.deleteGrave(grave.getPlayerUUID(), grave.getGraveID(), collector));
    }

//    public void breakGrave(UUID playerUUID, int graveID) {
//        Storage.SERVICE.submit(() -> storage.deleteGrave(playerUUID, graveID, null));
//    }

//    public void breakGrave(UUID playerUUID, int graveID, Player collector) {
//        Storage.SERVICE.submit(() -> storage.deleteGrave(playerUUID, graveID, collector));
//    }

    public int getNextGraveID(UUID playerUUID) {
        return storage.getNextGraveID(playerUUID);
    }

    public int getLowestGraveID(UUID playerUUID) {
        return storage.getLowestGraveID(playerUUID);
    }

    public int getGraveCount(UUID playerUUID) {
        return storage.getGraveCount(playerUUID);
    }

    public UUID getArmorStandUUID(UUID playerUUID, int graveID) {
        return storage.getArmorStandUUID(playerUUID, graveID);
    }

    public long getEpochSeconds(UUID playerUUID, int graveID) {
        return storage.getEpochSeconds(playerUUID, graveID);
    }

    public String getContents(UUID playerUUID, int graveID) {
        return storage.getContents(playerUUID, graveID);
    }
}
