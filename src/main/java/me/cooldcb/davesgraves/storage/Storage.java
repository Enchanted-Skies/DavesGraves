package me.cooldcb.davesgraves.storage;

import me.cooldcb.davesgraves.Grave;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface Storage {
    ExecutorService SERVICE = Executors.newSingleThreadExecutor();
    void loadGraves();
    void saveGrave(Grave grave);
    void deleteGrave(UUID playerUUID, int graveID, Player player);

    int getNextGraveID(UUID playerUUID);
    int getLowestGraveID(UUID playerUUID);
    int getGraveCount(UUID playerUUID);

    UUID getArmorStandUUID(UUID playerUUID, int graveID);
    long getEpochSeconds(UUID playerUUID, int graveID);
    String getContents(UUID playerUUID, int graveID);

    boolean init();
}
