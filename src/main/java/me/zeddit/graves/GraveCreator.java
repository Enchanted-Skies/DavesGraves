package me.zeddit.graves;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.util.Pair;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.MineDownStringifier;
import me.zeddit.graves.serialisation.GraveSerialiser;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class GraveCreator {

    private ItemStack skull;
    private final CoreProtectLogger logger;
    private final GraveLogger graveLogger;
    private final List<Pair<UUID, Location>> expiredGraves;

    public GraveCreator(CoreProtectLogger logger, GraveLogger graveLogger, List<Pair<UUID, Location>> expiredGraves) {
        this.graveLogger = graveLogger;
        this.logger = logger;
        this.expiredGraves = expiredGraves;
        reloadGraveTexture();
    }


    public void createGrave(Location loc, List<ItemStack> contents, Player owner) {
        logger.logInventory(owner, loc);
        if (loc.getY() <= loc.getWorld().getMinHeight() + 6) {
            loc.setY(loc.getWorld().getMinHeight() + 7.00);
        }
        //needs to be made async.. at least in parts.
        loc.getWorld().spawn(loc, ArmorStand.class, (armorStand) -> {
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setBasePlate(false);
            armorStand.setSmall(true);
            armorStand.setCanMove(false);
            Objects.requireNonNull(armorStand.getEquipment()).setHelmet(skull);
            armorStand.setCustomNameVisible(true);
            final String stringName =GravesMain.getInstance().getConfig()
                    .getString("nameTagFormat");
            final String ownerStringified = new MineDownStringifier().stringify(owner.displayName());
            final Component name =  new MineDown(
                    stringName)
                    .replaceFirst(true)
                    .replace("name", ownerStringified).toComponent();
            armorStand.customName(name);
            final PersistentDataContainer container = armorStand.getPersistentDataContainer();
            container.set(GraveKeys.GRAVE_ID.getKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
            container.set(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, owner.getUniqueId().toString());
            /*final List<byte[]> inventory = contents.stream().map(ItemStack::serializeAsBytes).collect(Collectors.toList());
            for (int i = 0; i < inventory.size(); i++) {
                container.set(new NamespacedKey(GravesMain.getInstance(), String.valueOf(i)), PersistentDataType.BYTE_ARRAY, inventory.get(i));
            }*/
            container.set(GraveKeys.INVENTORY_SIZE.getKey(), PersistentDataType.INTEGER, contents.size()); // old format
            final byte[] inventory = new GraveSerialiser(contents).serialise();
            container.set(GraveKeys.INVENTORY.getKey(), PersistentDataType.BYTE_ARRAY, inventory);
            final FileConfiguration config = GravesMain.getInstance().getConfig();
            final long duration = config.getLong("graveDuration");
            if (duration == -1) {
                container.set(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, -1L);
            } else {
                final long durationConv = GravesMain.millisConvert(duration);
                final long expiry = System.currentTimeMillis() + durationConv;
                final GravesMain instance =GravesMain.getInstance();
                container.set(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, expiry);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        armorStand.remove();
                        final var pair =new Pair<>(owner.getUniqueId(), armorStand.getLocation());
                        expiredGraves.add(pair);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                expiredGraves.remove(pair);
                            }
                        }.runTaskLater(instance, GravesMain.millisConvert(instance.getConfig().getLong("expiredDuration")) / 50); // ms to tick
                        graveLogger.logExpiry(UUID.fromString(armorStand.getPersistentDataContainer().getOrDefault(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, "")));
                    }
                }.runTaskLater(GravesMain.getInstance(), durationConv / 50); // ms to tick
            }
            graveLogger.logCreate(owner, loc, contents);
        });
    }

    public void reloadGraveTexture() {
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
