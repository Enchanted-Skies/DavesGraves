package me.cooldcb.davesgraves;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class Grave {
    private final UUID playerUUID;
    private int id;
    private final UUID asUUID;
    private final long epochSeconds;
    private final String base64Data;

    public Grave(UUID playerUUID, int id) {
        this.playerUUID = playerUUID;
        this.id = id;
        this.asUUID = DavesGraves.dataManager.getArmorStandUUID(playerUUID, id);
        this.epochSeconds = DavesGraves.dataManager.getEpochSeconds(playerUUID, id);
        this.base64Data = DavesGraves.dataManager.getContents(playerUUID, id);
        startGraveTimer();
    }

    public Grave(UUID playerUUID, UUID asUUID, long epochSeconds, String base64Data) {
        this.playerUUID = playerUUID;
        this.asUUID = asUUID;
        this.epochSeconds = epochSeconds;
        this.base64Data = base64Data;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        id = DavesGraves.dataManager.getNextGraveID(playerUUID);
        startGraveTimer();
    }

    public int getGraveID() {
        return id;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public UUID getArmorStandUUID() {
        return asUUID;
    }

    public long getEpochSeconds() {
        return epochSeconds;
    }

    public String getBase64Data() {
        return base64Data;
    }

    private void startGraveTimer() {
        long currSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long diffSeconds = currSeconds - epochSeconds;
        long secondsLeft = DavesGraves.configManager.getGraveLifetime() - diffSeconds;
        if (secondsLeft < 0) {
            DavesGraves.dataManager.breakGrave(this);
            return;
        }
        Bukkit.getScheduler().runTaskLater(DavesGraves.getInstance(), () -> DavesGraves.dataManager.breakGrave(this), secondsLeft * 20L);
    }
}
