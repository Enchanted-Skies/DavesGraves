package me.zeddit.graves;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.MineDownStringifier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GraveCreator {

    private ItemStack skull;

    public GraveCreator() {
        reloadGraveTexture();
    }

    public void createGrave(Location loc, List<ItemStack> contents, Player owner) {
        if (loc.getY() <= 6) loc.setX(7.00);
        //needs to be made async.. at least in parts.
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class, (armorStand) -> {
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setBasePlate(false);
            armorStand.setSmall(true);
            armorStand.setCanMove(false);
            Objects.requireNonNull(armorStand.getEquipment()).setHelmet(skull);
            armorStand.customName(
                    new MineDown(
                            GravesMain.getInstance().getConfig()
                            .getString("nameTagFormat"))
                            .replaceFirst(true)
                            .replace("name", new MineDownStringifier().stringify(owner.displayName())).toComponent());
            final PersistentDataContainer container = armorStand.getPersistentDataContainer();
            container.set(GraveKeys.GRAVE_OWNER.toKey(), PersistentDataType.STRING, owner.getUniqueId().toString());
            final List<byte[]> inventory = contents.stream().map(ItemStack::serializeAsBytes).collect(Collectors.toList());
            for (int i = 0; i < inventory.size(); i++) {
                container.set(new NamespacedKey(GravesMain.getInstance(), String.valueOf(i)), PersistentDataType.BYTE_ARRAY, inventory.get(i));
            }
            container.set(GraveKeys.INVENTORY_SIZE.toKey(), PersistentDataType.INTEGER, inventory.size());
            final FileConfiguration config = GravesMain.getInstance().getConfig();
            final long duration = config.getLong("graveDuration");
            if (duration == -1) {
                container.set(GraveKeys.EXPIRY.toKey(), PersistentDataType.LONG, -1L);
            } else {
                final TimeUnit unit = TimeUnit
                        .valueOf(Objects.requireNonNull(config
                                .getString("durationUnit")).toUpperCase(Locale.ROOT));
                final long durationConv = TimeUnit.MILLISECONDS.convert(duration, unit);
                final long expiry = System.currentTimeMillis() + durationConv;
                container.set(GraveKeys.EXPIRY.toKey(), PersistentDataType.LONG, expiry);
                GravesMain.getInstance().getService().schedule(armorStand::remove, durationConv, TimeUnit.MILLISECONDS);
            }
        });
    }
    public void reloadGraveTexture() {
        skull = getSkull( GravesMain.getInstance().getConfig().getString("graveTexture"));
    }
    private ItemStack getSkull(String texture) {
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
        GameProfile profile = new GameProfile(id, "Grave");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }
}
