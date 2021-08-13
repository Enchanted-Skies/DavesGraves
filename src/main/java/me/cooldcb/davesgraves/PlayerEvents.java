package me.cooldcb.davesgraves;

import me.cooldcb.davesgraves.libraries.ItemSerialization;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class PlayerEvents implements Listener {
    private final NamespacedKey graveKey = new NamespacedKey(DavesGraves.getInstance(), "Grave");

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!DavesGraves.configManager.isWorldEnabled(player.getWorld().getName())) return;
        List<ItemStack> drops = event.getDrops();
        createGrave(player, drops);
        event.getDrops().clear();
    }

    private boolean isNullContainerData(Player player, String[] containerData, ArmorStand stand) {
        if (containerData[0].equals("null")) {
            player.sendMessage("§7The Grave rots before your eyes.");
            stand.remove();
            return true;
        }
        return false;
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();
        String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
        if (graveContainer == null) return;
        event.setCancelled(true);
        String[] containerData = graveContainer.split("\\|");
        if (isNullContainerData(event.getPlayer(), containerData, armorStand)) {
            return;
        }
        playerInteractWithGrave(event.getPlayer(), armorStand, containerData);
    }

    @EventHandler
    public void onPlayerDamageEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand armorStand)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
        if (graveContainer == null) return;
        event.setCancelled(true);
        String[] containerData = graveContainer.split("\\|");
        if (isNullContainerData(player, containerData, armorStand)) {
            return;
        }
        playerInteractWithGrave(player, armorStand, containerData);
    }

    @EventHandler
    public void onPlayerCrouch(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isDead()) return;
        List<Entity> entities = player.getNearbyEntities(0.25, 0.25, 0.25);
        for (Entity entity : entities) {
            if (!(entity instanceof ArmorStand armorStand)) continue;
            String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
            if (graveContainer == null) continue;
            event.setCancelled(true);
            String[] containerData = graveContainer.split("\\|");
            if (isNullContainerData(player, containerData, armorStand)) {
                return;
            }
            playerInteractWithGrave(player, armorStand, containerData);
        }
    }

    private void playerInteractWithGrave(Player player, Entity entity, String[] containerData) {
        String graveOwnerUUIDStr = containerData[0];
        UUID graveOwnerUUID = UUID.fromString(graveOwnerUUIDStr);
        String graveID = containerData[1];
        if (!DavesGraves.configManager.canAllPlayersLoot()) {
            UUID playerUUID = player.getUniqueId();
            if (!playerUUID.equals(graveOwnerUUID)) {
                player.sendMessage("§cThis is not your loot!");
                return;
            }
        }
        DavesGraves.dataManager.getGrave(graveOwnerUUID, Integer.parseInt(graveID), (grave) -> {
            if (grave.isValid()) DavesGraves.dataManager.breakGrave(grave, player);
            else {
                player.sendMessage("§7The Grave rots before your eyes.");
                entity.remove();
            }
        });
    }

    private void createGrave(Player player, List<ItemStack> drops) {
        if (drops.size() == 0) return;
        ItemStack[] dropArr = new ItemStack[drops.size()];
        drops.toArray(dropArr);
        String base64Data = ItemSerialization.itemStackArrayToBase64(dropArr);
        UUID playerUUID = player.getUniqueId();
        Location pLoc = player.getLocation();
        if (pLoc.getY() <= 6) pLoc.setY(7);
        int randomDirection = (int) (Math.random() * 4) * 90;
        ArmorStand graveAS = pLoc.getWorld().spawn(pLoc, ArmorStand.class, (armorStand -> {
            try {
                armorStand.setGravity(false);
                armorStand.setVisible(false);
                armorStand.setInvulnerable(false);
                armorStand.setRotation(randomDirection, 0f);
                armorStand.setBasePlate(false);
                armorStand.setSmall(true);
                armorStand.setCustomName("§6§l" + player.getName() + "§6§l's Loot");
                armorStand.setCustomNameVisible(true);
                armorStand.getEquipment().setHelmet(DavesGraves.configManager.getGraveHead());
                armorStand.getPersistentDataContainer().set(graveKey, PersistentDataType.STRING, playerUUID + "|" + DavesGraves.dataManager.getNextGraveID(player.getUniqueId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        Grave grave = new Grave(playerUUID, graveAS.getUniqueId(), LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), base64Data);
        DavesGraves.dataManager.saveGrave(grave);
        Bukkit.getScheduler().runTaskLater(DavesGraves.getInstance(), () -> player.sendMessage("§7You just died, a grave has been created at §c" + pLoc.getBlockX() + ", " + pLoc.getBlockY() + ", " + pLoc.getBlockZ() + " §7with your loot in."), 20);
    }
}
