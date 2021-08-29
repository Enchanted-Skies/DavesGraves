package org.enchantedskies.enchantedgraves;

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
    private boolean isValid = true;

    public Grave(UUID playerUUID, int id) {
        this.playerUUID = playerUUID;
        this.id = id;
        this.asUUID = EnchantedGraves.dataManager.getArmorStandUUID(playerUUID, id);
        if (asUUID == null) isValid = false;
        this.epochSeconds = EnchantedGraves.dataManager.getEpochSeconds(playerUUID, id);
        this.base64Data = EnchantedGraves.dataManager.getContents(playerUUID, id);
        startGraveTimer();
    }

    public Grave(UUID playerUUID, UUID asUUID, long epochSeconds, String base64Data) {
        this.playerUUID = playerUUID;
        this.asUUID = asUUID;
        this.epochSeconds = epochSeconds;
        this.base64Data = base64Data;
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;
        id = EnchantedGraves.dataManager.getNextGraveID(playerUUID);
        startGraveTimer();
    }

    private void startGraveTimer() {
        //epoch seconds is time created.
        long currSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long diffSeconds = currSeconds - epochSeconds;
        long secondsLeft = EnchantedGraves.configManager.getGraveLifetime() - diffSeconds;
        if (secondsLeft <= 0) {
            Bukkit.getScheduler().runTaskLater(EnchantedGraves.getInstance(), () -> EnchantedGraves.dataManager.breakGrave(this), 1L);
            return;
        }
        Bukkit.getScheduler().runTaskLater(EnchantedGraves.getInstance(), () -> EnchantedGraves.dataManager.breakGrave(this), (secondsLeft * 20L));
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

    public boolean isValid() {
        return isValid;
    }


}
