package me.zeddit.graves;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DeathListener implements Listener {


    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final List<ItemStack> drops = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        final Location standLoc = e.getEntity().getLocation().clone();
        if (standLoc.getY() <= 6) standLoc.setX(7.00);
        //needs to be made async.. at least in parts. Features to be impl'd- custom skull texture, executor to remove graves after expiry
        ArmorStand stand = standLoc.getWorld().spawn(standLoc, ArmorStand.class, (armorStand) -> {
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setBasePlate(false);
            armorStand.setSmall(true);
            armorStand.setCanMove(false);
            final PersistentDataContainer container = armorStand.getPersistentDataContainer();
            container.set(GraveKeys.GRAVE_OWNER.toKey(), PersistentDataType.STRING, e.getEntity().getUniqueId().toString());
            final FileConfiguration config = GravesMain.getInstance().getConfig();
            final TimeUnit unit = TimeUnit
                    .valueOf(Objects.requireNonNull(config
                            .getString("durationUnit")).toUpperCase(Locale.ROOT));
            //this will panic upon unboxing a null value
            final long duration = config.getLong("graveDuration");
            final long expiry = System.currentTimeMillis() + unit.convert(duration, TimeUnit.MILLISECONDS);
            container.set(GraveKeys.EXPIRY.toKey(), PersistentDataType.LONG, expiry);
            final List<byte[]> inventory = drops.stream().map(ItemStack::serializeAsBytes).collect(Collectors.toList());
            for (int i = 0; i < inventory.size(); i++) {
                container.set(new NamespacedKey(GravesMain.getInstance(), String.valueOf(i)), PersistentDataType.BYTE_ARRAY, inventory.get(i));
            }
            container.set(GraveKeys.INVENTORY_SIZE.toKey(), PersistentDataType.INTEGER, inventory.size());

        });
    }
}
