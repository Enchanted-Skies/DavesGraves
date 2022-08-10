package me.zeddit.graves;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;



public enum GraveKeys implements Keyed {
    //As uuid
    GRAVE_OWNER("graveOwner"),
    EXPIRY("expiry"),
    INVENTORY_SIZE("invSize"), // should be removed, redundant
    INVENTORY("inv"),
    GRAVE_ID("graveId");

    private final String value;
    private NamespacedKey key = null;


    GraveKeys(String value) {
        this.value = value;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        if (key == null) {
            key = new NamespacedKey(GravesMain.getInstance(), value);
        }
        return key;
    }

    public static GraveKeys fromKey(NamespacedKey key) {
        for (GraveKeys i : GraveKeys.values()) {
            if (key.value().equals(i.value)) {
                return i;
            }
        }
        return null;
    }
}

