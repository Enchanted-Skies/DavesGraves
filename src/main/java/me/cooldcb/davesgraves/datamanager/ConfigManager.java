package me.cooldcb.davesgraves.datamanager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.cooldcb.davesgraves.DavesGraves;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public class ConfigManager {

    private final DavesGraves plugin = DavesGraves.getInstance();
    private FileConfiguration config;
    private List<String> worldList;
    private ItemStack graveHead;
    private boolean canAllPlayersLoot;
    private int itemReturnMode; //possibly replace with an Enum.
    private int maxGraves;
    private int graveLifetime;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        worldList = reloadWorldList();
        graveHead = getCustomSkull(config.getString("grave-texture"));
        canAllPlayersLoot = config.getBoolean("all-loot");
        itemReturnMode = config.getInt("item-return-mode");
        maxGraves = config.getInt("max-graves");
        graveLifetime = config.getInt("time");
    }

    private List<String> reloadWorldList() {
        return config.getStringList("worlds");
    }

    public boolean canAllPlayersLoot() {
        return canAllPlayersLoot;
    }

    public int getItemReturnMode() {
        return itemReturnMode;
    }

    public int getMaxGraves() {
        return maxGraves;
    }

    public int getGraveLifetime() {
        return graveLifetime;
    }

    public ItemStack getGraveHead() {
        return graveHead;
    }

    public boolean isWorldEnabled(String worldName) {
        return worldList.contains(worldName);
    }

    public ItemStack getCustomSkull(String texture) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        try {
            Method metaSetProfileMethod = skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
            metaSetProfileMethod.setAccessible(true);
            metaSetProfileMethod.invoke(skullMeta, makeProfile(texture));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // if in an older API where there is no setProfile method,
            // we set the profile field directly.
            try {
                Field metaProfileField = skullMeta.getClass().getDeclaredField("profile");
                metaProfileField.setAccessible(true);
                metaProfileField.set(skullMeta, makeProfile(texture));

            } catch (NoSuchFieldException | IllegalAccessException ex2) {
                ex2.printStackTrace();
            }
        }
        skull.setItemMeta(skullMeta);
        return skull;
    }

    private GameProfile makeProfile(String b64) {
        // random uuid based on the b64 string
        UUID id = new UUID(
            b64.substring(b64.length() - 20).hashCode(),
            b64.substring(b64.length() - 10).hashCode()
        );
        GameProfile profile = new GameProfile(id, "Player");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }
}
