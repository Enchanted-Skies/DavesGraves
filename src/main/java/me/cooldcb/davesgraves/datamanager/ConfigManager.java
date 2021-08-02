package me.cooldcb.davesgraves.datamanager;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.cooldcb.davesgraves.DavesGraves;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConfigManager {
    private final DavesGraves plugin = DavesGraves.getInstance();
    private FileConfiguration config;
    private final List<String> worldList;
    private final ItemStack graveHead;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
        worldList = reloadWorldList();
        graveHead = getCustomSkull(config.getString("grave-texture"));
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    private  List<String> reloadWorldList() {
        return config.getStringList("worlds");
    }

    public boolean canAllPlayersLoot() {
        return config.getBoolean("all-loot");
    }

    public int getItemReturnMode() {
        return config.getInt("item-return-mode");
    }

    public int getMaxGraves() {
        return config.getInt("max-graves");
    }

    public int getGraveLifetime() {
        return config.getInt("time");
    }

    public ItemStack getGraveHead() {
        return graveHead;
    }

    public boolean isWorldEnabled(String worldName) {
        return worldList.contains(worldName);
    }

    private ItemStack getCustomSkull(String texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
        Set<ProfileProperty> profileProperties = playerProfile.getProperties();
        profileProperties.add(new ProfileProperty("textures", texture));
        playerProfile.setProperties(profileProperties);
        skullMeta.setPlayerProfile(playerProfile);
        skull.setItemMeta(skullMeta);
        return skull;
    }
}
