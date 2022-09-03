package me.zeddit.graves;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class GraveCreator implements Listener {

    private ItemStack skull;

    public GraveCreator() {
        onConfigReload(null);
    }


    public Location createGrave(Location loc, List<ItemStack> contents, Player owner) {
        final FileConfiguration config = GravesMain.getInstance().getConfig();
        final long duration = config.getLong("graveDuration");
        final long expiry = duration == -1 ? -1 : GravesMain.millisConvert(duration) + System.currentTimeMillis();
        final Grave grave = new Grave(owner.getUniqueId(), UUID.randomUUID(), expiry, contents);
        return grave.spawn(loc, skull, owner).getLocation();
    }

    @EventHandler
    public void onConfigReload(ConfigReloadEvent e) {
        skull = getSkull( GravesMain.getInstance().getConfig().getString("graveTexture"));
    }

    public static ItemStack getSkull(String texture) {
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
    private static GameProfile makeProfile(String b64) {
        // random uuid based on the b64 string
        UUID id = new UUID(
                b64.substring(b64.length() - 20).hashCode(),
                b64.substring(b64.length() - 10).hashCode()
        );
        GameProfile profile = new GameProfile(id, "Grave");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }

}
