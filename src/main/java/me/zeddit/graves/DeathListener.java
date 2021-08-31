package me.zeddit.graves;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private NamespacedKey mainKey = new NamespacedKey(GravesMain.getInstance(), "");

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        final List<ItemStack> drops = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        final Location standLoc = e.getEntity().getLocation().clone();
        if (standLoc.getY() <= 6) standLoc.setX(7.00);
        ArmorStand stand = standLoc.getWorld().spawnEntity(standLoc)

    }
}
