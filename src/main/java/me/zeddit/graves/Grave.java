package me.zeddit.graves;

import me.zeddit.graves.serialisation.GraveSerialiser;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public record Grave(UUID graveOwner, UUID graveId, long expiry, int invSize, List<ItemStack> inv) {
    public static Grave fromStand(ArmorStand stand) {
        final PersistentDataContainer container = stand.getPersistentDataContainer();
        final UUID graveOwner = UUID.fromString(inv(container.get(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING)));
        final UUID graveId = UUID.fromString(inv(container.get(GraveKeys.GRAVE_ID.getKey(),PersistentDataType.STRING)));
        final long expiry= inv(container.get(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG));
        final int invSize = inv(container.get(GraveKeys.INVENTORY_SIZE.getKey(), PersistentDataType.INTEGER));
        final List<ItemStack> inv = GraveSerialiser.deserialise(
                inv(container.get(GraveKeys.INVENTORY.getKey(), PersistentDataType.BYTE_ARRAY)));
        return new Grave(graveOwner,graveId,expiry,invSize,inv);
    }
    private static <T> T inv(T x) {
        if (x == null) {
            throw new IllegalStateException("container value null");
        }
        return x;
    }
}
