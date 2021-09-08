package me.zeddit.graves;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;


public enum GraveKeys implements Keyed {
    GRAVE_OWNER("graveOwner"),
    EXPIRY("expiry"),
    INVENTORY_SIZE("invSize");

    private final String value;
    //This needs to be lazy initialised to avoid GravesMain.getInstance resolving to null.
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

