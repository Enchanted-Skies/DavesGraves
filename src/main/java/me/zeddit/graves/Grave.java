package me.zeddit.graves;

import com.mojang.datafixers.util.Pair;
import de.themoep.minedown.adventure.MineDown;
import de.themoep.minedown.adventure.MineDownStringifier;
import me.zeddit.graves.serialisation.GraveSerialiser;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Grave {

    private boolean initialised;
    private final UUID graveOwner;
    private final UUID graveId;
    private final long expiry;
    private final int invSize;
    private final List<ItemStack> inv;

    private static final List<Pair<UUID, Location>> expiredGraves = new ArrayList<>();
    private static final List<ArmorStand> activeGraves = new ArrayList<>();


    public Grave(UUID graveOwner, UUID graveId, long expiry, List<ItemStack> inv) {
        this.graveOwner = graveOwner;
        this.graveId = graveId;
        this.expiry = expiry;
        this.inv = inv;
        this.invSize = inv.size();
    }

    public Grave(ArmorStand stand) {
        final PersistentDataContainer container = stand.getPersistentDataContainer();
        graveOwner = UUID.fromString(inv(container.get(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING)));
        graveId = UUID.fromString(inv(container.get(GraveKeys.GRAVE_ID.getKey(),PersistentDataType.STRING)));
        expiry= inv(container.get(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG));
        invSize = inv(container.get(GraveKeys.INVENTORY_SIZE.getKey(), PersistentDataType.INTEGER));
        inv = GraveSerialiser.deserialise(
                inv(container.get(GraveKeys.INVENTORY.getKey(), PersistentDataType.BYTE_ARRAY)));
        scheduleAutoCleanup(stand);
    }

    private static <T> T inv(T x) {
        if (x == null) {
            throw new IllegalStateException("container value null");
        }
        return x;
    }

    public ArmorStand spawn(Location loc, ItemStack skull, Player owner) {
        if (initialised) {
            throw new IllegalStateException("Grave already exists!");
        } else {
            CoreProtectLogger.getInstance().logInventory(owner, loc);
            if (loc.getY() <= loc.getWorld().getMinHeight() + 6) {
                loc.setY(loc.getWorld().getMinHeight() + 7.00);
            }
            //needs to be made async.. at least in parts.
            return loc.getWorld().spawn(loc, ArmorStand.class, (armorStand) -> {
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
                container.set(GraveKeys.GRAVE_ID.getKey(), PersistentDataType.STRING, graveId.toString());
                container.set(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, graveOwner.toString());
            /*final List<byte[]> inventory = contents.stream().map(ItemStack::serializeAsBytes).collect(Collectors.toList());
            for (int i = 0; i < inventory.size(); i++) {
                container.set(new NamespacedKey(GravesMain.getInstance(), String.valueOf(i)), PersistentDataType.BYTE_ARRAY, inventory.get(i));
            }*/
                container.set(GraveKeys.INVENTORY_SIZE.getKey(), PersistentDataType.INTEGER, invSize); // old format
                final byte[] inventory = new GraveSerialiser(inv).serialise();
                container.set(GraveKeys.INVENTORY.getKey(), PersistentDataType.BYTE_ARRAY, inventory);
                container.set(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, expiry);
                scheduleAutoCleanup(armorStand);
                GraveLogger.getInstance().logCreate(owner, loc, inv);
            });
        }
    }

    public void scheduleAutoCleanup(ArmorStand grave) {
        activeGraves.add(grave);
        final GravesMain instance = GravesMain.getInstance();
        final long duration = (expiry - System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                activeGraves.remove(grave);
                grave.remove();
                final var pair =new Pair<>(graveOwner, grave.getLocation());
                expiredGraves.add(pair);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        expiredGraves.remove(pair);
                    }
                }.runTaskLater(instance, GravesMain.millisConvert(instance.getConfig().getLong("expiredDuration")) / 50); // ms to tick
                GraveLogger.getInstance().logExpiry(UUID.fromString(grave.getPersistentDataContainer().getOrDefault(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING, "")));
            }
        }.runTaskLater(GravesMain.getInstance(), duration / 50); // ms to tick
    }

    public static List<Pair<UUID, Location>> getExpiredGraves() {
        return expiredGraves;
    }

    public static List<ArmorStand> getActiveGraves() {
        return activeGraves;
    }
}
