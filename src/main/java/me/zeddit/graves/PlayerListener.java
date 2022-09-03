package me.zeddit.graves;


import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import de.themoep.minedown.adventure.MineDown;
import me.zeddit.graves.serialisation.GraveSerialiser;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static me.zeddit.graves.GraveLogger.playerName;


public class PlayerListener implements Listener {
    private final GraveCreator creator;
    private final GraveLogger logger;

    public PlayerListener(GraveCreator creator, GraveLogger logger) {
        this.creator = creator;
        this.logger = logger;
    }


    private void unpackGrave(ArmorStand stand, Player manipulator) {
        final FileConfiguration config =GravesMain.getInstance().getConfig();
        final boolean ownerLoot = config.getBoolean("onlyOwnersCanLoot");
        final String ownerIDStr = stand.getPersistentDataContainer().get(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING);
        final Component invalidGrave = new MineDown(config.getString("invalidGrave")).toComponent();
        if (ownerIDStr == null) {
            stand.remove();
            manipulator.sendMessage(invalidGrave);
            logger.logRaw(String.format("Could not open grave because the owner was null. opener:%s (openeruuid:%s)", playerName(manipulator), manipulator.getUniqueId()));
            return;
        }
        final long expiry = stand.getPersistentDataContainer().getOrDefault(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG, -2L);
        if (expiry == -2) {
            stand.remove();
            manipulator.sendMessage(invalidGrave);
            logger.logRaw(String.format("Could not open grave because there was no expiry value. opener:%s (openeruuid:%s) creatoruuid:%s", playerName(manipulator), manipulator.getUniqueId(), ownerIDStr));
            return;
        }
        if (expiry != -1) {
            if (System.currentTimeMillis() >= expiry) {
                stand.remove();
                manipulator.sendMessage(invalidGrave);
                logger.logRaw(String.format("Could not open grave because it expired. opener:%s (openeruuid:%s) creatoruuid:%s", playerName(manipulator), manipulator.getUniqueId(), ownerIDStr));
                return;
            }
        }
        if (ownerLoot && !(UUID.fromString(ownerIDStr).equals(manipulator.getUniqueId()))) {
            manipulator.sendMessage(new MineDown(config.getString("doesNotOwnMessage")).toComponent());
            logger.logRaw(String.format("Could not open grave because the person opening did not own the grave. opener:%s (openeruuid:%s) creatoruuid:%s", playerName(manipulator), manipulator.getUniqueId(), ownerIDStr));
            return;
        }
        try {
            final List<ItemStack> toDrop = unpackInventory(stand.getPersistentDataContainer());
            final Location itemLoc = stand.getLocation().clone();
            stand.remove();
            final boolean b = GravesMain.getInstance().getConfig().getBoolean("fireImmunity");
            toDrop.forEach(it -> applyFireImmunity(itemLoc.getWorld().dropItem(itemLoc, it),b));
            logger.logOpen(manipulator, itemLoc, UUID.fromString(ownerIDStr), toDrop);
        } catch (IOException ex) {
            stand.remove();
            manipulator.sendMessage(invalidGrave);
            logger.logRaw(String.format("Could not open grave because there was an error unpacking the inventory. opener:%s (openeruuid:%s) creatoruuid:%s", playerName(manipulator), manipulator.getUniqueId(), ownerIDStr));
        }
        Grave.getActiveGraves().remove(stand);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final FileConfiguration config = GravesMain.getInstance().getConfig();
        if (e.getDrops().isEmpty()) {
            e.getEntity().sendMessage(new MineDown(config.getString("emptyInv")).toComponent());
            return;
        }
        final List<ItemStack> items = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        e.getEntity().sendMessage(new MineDown(config.getString("deathMsg")).replaceFirst(true)
                .replace("coords", formatLocation(creator.createGrave(e.getEntity().getLocation(), items, e.getEntity()))).toComponent());
    }

    private String formatLocation(Location loc) {
        return String.format("%s, %s, %s", Math.round(loc.getX()), Math.round(loc.getY()), Math.round(loc.getZ()));
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent e) {
        final ArmorStand stand = e.getRightClicked();
        if (!stand.getPersistentDataContainer().has(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING)) return;
        e.setCancelled(true);
        unpackGrave(stand, e.getPlayer());
    }

    private List<ItemStack> unpackInventory(PersistentDataContainer container) throws IOException {
        final byte[] newArr = container.getOrDefault(GraveKeys.INVENTORY.getKey(), PersistentDataType.BYTE_ARRAY, new byte[0]);
        List<ItemStack> inv;
        if (newArr.length != 0) {
            inv =  GraveSerialiser.deserialise(newArr);
        } else {
            inv =  unpackInventoryOld(container);
        }
        return inv;
    }

    @Deprecated
    private List<ItemStack> unpackInventoryOld(PersistentDataContainer container) throws IOException {
        //Old serialisation format -- should be removed
        final int len = container.getOrDefault(GraveKeys.INVENTORY_SIZE.getKey(), PersistentDataType.INTEGER, -1);
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

    private void applyFireImmunity(Item it, boolean fireImmunity) {
        if (fireImmunity) {
            it.setInvulnerable(true);
        }
    }

    @EventHandler
    public void externalGraveBreak(EntityDamageByEntityEvent e) {
        if ((e.getEntity() instanceof final ArmorStand stand) && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && (e.getDamager() instanceof final Player player)) {
            if (stand.getPersistentDataContainer().has(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING)) {
                e.setCancelled(true);
                unpackGrave(stand, player);
            }
        }
    }

}
