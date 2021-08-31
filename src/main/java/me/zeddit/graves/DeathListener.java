package me.zeddit.graves;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeathListener implements Listener {
    private final GraveCreator creator;

    public DeathListener(GraveCreator creator) {
        this.creator = creator;
    }
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final List<ItemStack> items = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        creator.createGrave(e.getEntity().getLocation(), items, e.getEntity());
    }
    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        final ArmorStand stand = e.getRightClicked();
        if (!stand.getPersistentDataContainer().has(GraveKeys.GRAVE_OWNER.toKey(), PersistentDataType.STRING)) return;
        final FileConfiguration config =GravesMain.getInstance().getConfig();
        final boolean ownerLoot = config.getBoolean("onlyOwnersCanLoot");
        final String ownerIDStr = stand.getPersistentDataContainer().get(GraveKeys.GRAVE_OWNER.toKey(), PersistentDataType.STRING);
        final Component invalidGrave = new MineDown(config.getString("invalidGrave")).toComponent();
        if (ownerIDStr == null) {
            e.getRightClicked().remove();
            e.getPlayer().sendMessage(invalidGrave);
            return;
        }
        if (ownerLoot && !(UUID.fromString(ownerIDStr).equals(e.getPlayer().getUniqueId()))) {
            e.getPlayer().sendMessage(new MineDown(config.getString("doesNotOwnMessage")).toComponent());
            return;
        }
        final long expiry = stand.getPersistentDataContainer().getOrDefault(GraveKeys.EXPIRY.toKey(), PersistentDataType.LONG, -2L);
        if (expiry == -2) {
            e.getRightClicked().remove();
            e.getPlayer().sendMessage(invalidGrave);
            return;
        }
        if (expiry != -1) {
            if (System.currentTimeMillis() >= expiry) {
                e.getRightClicked().remove();
                e.getPlayer().sendMessage(invalidGrave);
                return;
            }
        }
        try {
            final List<ItemStack> toDrop = unpackInventory(stand.getPersistentDataContainer());
            final Location itemLoc = stand.getLocation().clone();
            stand.remove();
            toDrop.forEach(it -> itemLoc.getWorld().dropItem(itemLoc, it));
        } catch (IOException ex) {
            e.getRightClicked().remove();
            e.getPlayer().sendMessage(invalidGrave);
        }
    }

    private List<ItemStack> unpackInventory(PersistentDataContainer container) throws IOException {
        final int len = container.getOrDefault(GraveKeys.INVENTORY_SIZE.toKey(), PersistentDataType.INTEGER, -1);
        if (len < 0) {
            throw new IOException("Invalid length of inventory, or could not find the mapping at all.");
        }
        final List<ItemStack> itemStacks = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            try {
                final NamespacedKey key = new NamespacedKey(GravesMain.getInstance(), String.valueOf(i));
                final byte[] bytes = container.get(key, PersistentDataType.BYTE_ARRAY);
                if (bytes == null) {
                    throw new IOException("Could not find contents for index " + i + " !");
                }
                itemStacks.add(ItemStack.deserializeBytes(bytes));
            } catch (Exception e) {
                throw new IOException("Deserialisation error");
            }
        }
        return itemStacks;
    }

}
