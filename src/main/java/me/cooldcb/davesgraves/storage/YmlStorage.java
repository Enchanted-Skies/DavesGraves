package me.cooldcb.davesgraves.storage;

import me.cooldcb.davesgraves.DavesGraves;
import me.cooldcb.davesgraves.Grave;
import me.cooldcb.davesgraves.libraries.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class YmlStorage implements Storage {
    private final DavesGraves plugin = DavesGraves.getInstance();
    private final NamespacedKey graveKey = new NamespacedKey(DavesGraves.getInstance(), "Grave");
    private File dataFile;
    private YamlConfiguration config;
    private final ReentrantLock fileLock = new ReentrantLock();

    @Override
    public void loadGraves() {
        for (String playerUUIDStr : config.getKeys(false)) {
            ConfigurationSection playerSection = config.getConfigurationSection(playerUUIDStr);
            UUID playerUUID = UUID.fromString(playerUUIDStr);
            for (String graveIDStr : playerSection.getKeys(false)) {
                new Grave(playerUUID, Integer.parseInt(graveIDStr));
            }
        }
    }

    @Override
    public void saveGrave(Grave grave) {
        fileLock.lock();
        ConfigurationSection playerSection = config.getConfigurationSection(grave.getPlayerUUID().toString());
        if (playerSection == null) playerSection = config.createSection(grave.getPlayerUUID().toString());
        ConfigurationSection graveSection = playerSection.createSection(String.valueOf(grave.getGraveID()));
        graveSection.set("as-uuid", grave.getArmorStandUUID().toString());
        graveSection.set("time", grave.getEpochSeconds());
        graveSection.set("contents", grave.getBase64Data());
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    @Override
    public void deleteGrave(UUID playerUUID, int graveID, Player player) {
        ConfigurationSection playerSection = config.getConfigurationSection(playerUUID.toString());
        if (playerSection == null) {
            Bukkit.getLogger().warning("Attempted to delete data from the UUID " + playerUUID + " but the user was not found.");
            return;
        }

        ConfigurationSection graveSection = playerSection.getConfigurationSection(String.valueOf(graveID));
        if (graveSection == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            String asUUID = graveSection.getString("as-uuid", "");
            ArmorStand graveAS = (ArmorStand) Bukkit.getEntity(UUID.fromString(asUUID));
            if (graveAS != null) {
                Location asLoc = graveAS.getEyeLocation();
                graveAS.getPersistentDataContainer().set(graveKey, PersistentDataType.STRING, "null");
                graveAS.remove();
                asLoc.getWorld().spawnParticle(Particle.CLOUD, asLoc, 4);
                ItemStack[] itemArr;
                try {
                    itemArr = ItemSerialization.itemStackArrayFromBase64(graveSection.getString("contents"));
                } catch (IOException err) {
                    err.printStackTrace();
                    return;
                }
                int itemReturnMode = DavesGraves.configManager.getItemReturnMode();
                if (itemReturnMode != 1 && player == null) itemReturnMode = 1;
                switch (itemReturnMode) {
                    case 0 -> giveItemsToPlayer(itemArr, player);
                    case 1 -> dropItems(itemArr, asLoc);
                }
            }
            fileLock.lock();
            playerSection.set(String.valueOf(graveID), null);
            try {
                config.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fileLock.unlock();
            }
        });
    }


    @Override
    public int getNextGraveID(UUID playerUUID) {
        ConfigurationSection playerSection = config.getConfigurationSection(playerUUID.toString());
        if (playerSection == null) return 0;
        int highestID = -1;
        for (String graveIDStr : playerSection.getKeys(false)) {
            int graveID = Integer.parseInt(graveIDStr);
            if (graveID > highestID) highestID = graveID;
        }
        return highestID + 1;
    }

    @Override
    public int getLowestGraveID(UUID playerUUID) {
        ConfigurationSection playerSection = config.getConfigurationSection(playerUUID.toString());
        if (playerSection == null) return -1;
        int lowestID = Integer.MAX_VALUE;
        for (String graveIDStr : playerSection.getKeys(false)) {
            int graveID = Integer.parseInt(graveIDStr);
            if (graveID < lowestID) lowestID = graveID;
        }
        return lowestID;
    }

    @Override
    public int getGraveCount(UUID playerUUID) {
        ConfigurationSection playerSection = config.getConfigurationSection(playerUUID.toString());
        if (playerSection == null) return 0;
        return playerSection.getKeys(false).size();
    }


    @Override
    public UUID getArmorStandUUID(UUID playerUUID, int graveID) {
        String uuidStr = config.getString(playerUUID + "." + graveID + ".as-uuid");
        if (uuidStr == null) return null;
        return UUID.fromString(uuidStr);
    }

    @Override
    public long getEpochSeconds(UUID playerUUID, int graveID) {
        return config.getLong(playerUUID + "." + graveID + ".date", DavesGraves.configManager.getGraveLifetime());
    }

    @Override
    public String getContents(UUID playerUUID, int graveID) {
        return config.getString(playerUUID + "." + graveID + ".contents");
    }


    @Override
    public boolean init() {
        File dataFile = new File(plugin.getDataFolder(),"graves.yml");
        try {
            if (dataFile.createNewFile()) plugin.getLogger().info("File Created: graves.yml");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.dataFile = dataFile;
        config = YamlConfiguration.loadConfiguration(dataFile);
        return true;
    }

    private void giveItemsToPlayer(ItemStack[] itemSet, Player player) {
        Inventory playerInv = player.getInventory();
        for (ItemStack item : itemSet) {
            HashMap<Integer, ItemStack> itemMap = playerInv.addItem(item);
            for (ItemStack droppedItem : itemMap.values()) {
                player.getWorld().dropItem(player.getLocation(), droppedItem);
            }
        }
    }

    private void dropItems(ItemStack[] itemSet, Location location) {
        for (ItemStack item : itemSet) {
            location.getWorld().dropItemNaturally(location, item);
        }
    }
}
