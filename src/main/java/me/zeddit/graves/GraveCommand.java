package me.zeddit.graves;

import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GraveCommand implements TabExecutor, Listener {

    private final GraveCreator creator;
    private final GraveLogger logger;
    private final ArrayList<Inventory> inventories = new ArrayList<>();
    private final List<Pair<UUID, Location>> expiredGraves;

    public GraveCommand(GraveCreator creator, GraveLogger logger, List<Pair<UUID, Location>> expiredGraves) {
        this.creator = creator;
        this.logger = logger;
        this.expiredGraves = expiredGraves;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            reload(commandSender);
        } else if (args[0].equalsIgnoreCase("clear")) {
            clear(commandSender);
        } else if (args[0].equalsIgnoreCase("savelog")) {
            save(commandSender);
        } else if (args[0].equalsIgnoreCase("display") && commandSender instanceof Player player) {
            display(player);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("graves.reload")) {
            sendNoPerms(sender);
            return;
        }
        final GravesMain main= GravesMain.getInstance();
        main.reloadConfig();
        creator.reloadGraveTexture();
        sender.sendMessage(Component.text("Reloaded graves successfully!", NamedTextColor.GREEN));
    }

    private Stream<ArmorStand> computeAllGraves() {
        return Bukkit.getWorlds().stream()
                .flatMap(it -> it.getEntities().stream())
                .map(it -> it instanceof ArmorStand ? (ArmorStand) it : null)
                .filter(Objects::nonNull)
                .filter(it -> it.getPersistentDataContainer().has(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING));
    }

    private void clear(CommandSender sender) {
        if (!sender.hasPermission("graves.clear")) {
            sendNoPerms(sender);
            return;
        }
        final AtomicInteger count = new AtomicInteger(0);
        computeAllGraves().forEach(it -> {it.remove(); count.getAndIncrement();});
        int countInt = count.get();
        sender.sendMessage(Component.text("Removed " + countInt + (countInt == 1 ? " grave" : " graves") + "!", NamedTextColor.GREEN));
    }
    private void save(CommandSender sender) {
        if (!sender.hasPermission("graves.save")) {
            sendNoPerms(sender);
            return;
        }
        logger.queueFlush();
        sender.sendMessage(Component.text("Queued a flush of the log buffer!", NamedTextColor.GREEN));
    }

    private void display(Player player) {
        final List<ArmorStand> playerGraves = computeAllGraves()
        .filter(it -> Objects.equals(it.getPersistentDataContainer().get(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING), player.getUniqueId().toString()))
        .collect(Collectors.toList());
        final var expiredPlayerGraves = expiredGraves
                .stream()
                .filter(it -> it.getFirst().equals(player.getUniqueId())).collect(Collectors.toList());
        if (playerGraves.isEmpty() && expiredPlayerGraves.isEmpty()) {
            player.sendMessage(Component.text("You have no active graves to display!").color(NamedTextColor.RED));
            return;
        }
        final Inventory inv = Bukkit.createInventory(player, InventoryType.CHEST);
        playerGraves.forEach(it -> {
            final Long exp = it.getPersistentDataContainer().get(GraveKeys.EXPIRY.getKey(), PersistentDataType.LONG);
            if (exp == null) {
                throw new NullPointerException("Expiry of grave cannot be null!");
            }
            inv.addItem(formatGrave(exp,it.getLocation()));
        });
        expiredPlayerGraves.forEach(it -> inv.addItem(formatExpiredGrave(it.getSecond())));
        inventories.add(inv);
        player.openInventory(inv);
    }

    private Component formatWorld(World world) {
        final String pre = "World: ";
        final String name = switch (world.getName()) {
            case "world" -> "The Overworld";
            case "world_nether" -> "The Nether";
            case "world_the_end" -> "The End";
            default -> world.getName();
        };
        return Component.text(pre + name).color(NamedTextColor.DARK_GRAY);
    }
    private ItemStack workItemStack(ItemStack stack, Consumer<ItemMeta> fn) {
        final ItemMeta meta = Objects.requireNonNull(stack.getItemMeta());
        fn.accept(meta);
        stack.setItemMeta(meta);
        return stack;
    }


    private ItemStack formatGrave(long expiry, Location loc) {
        final ItemStack stack = GraveCreator.getSkull(GravesMain.getInstance().getConfig().getString("graveTexture"));
        return workItemStack(stack, (meta) -> {
            meta.displayName(Component.text("Grave").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            final long expMin = TimeUnit.MINUTES.convert(expiry-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            final Component expFmt = expiry == -1L ? Component.text("Your grave doesn't expire.")
                    .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                    : Component.text("Your grave expires in ").color(NamedTextColor.GOLD).append(
                    Component.text(expMin + " minutes!").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD));
            final var lore = unwrapLoc(loc);
            lore.add(expFmt);
            meta.lore(lore);
        });
    }

    private ItemStack formatExpiredGrave(Location loc) {
        final ItemStack stack = GraveCreator.getSkull(GravesMain.getInstance().getConfig().getString("expiredGraveTexture"));
        return workItemStack(stack, (meta) -> {
            meta.displayName(Component.text("Irretrievable Grave").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
            final var lore = unwrapLoc(loc);
            lore.add(Component.text("This grave has been opened, or has expired.").color(NamedTextColor.RED));
            meta.lore(lore);
        });
    }

    private List<Component> unwrapLoc(Location loc) {
        final ArrayList<Component> comps = new ArrayList<>();
        comps.add(formatWorld(loc.getWorld()));
        comps.add(gray("X:", loc.getX()));
        comps.add(gray("Y:", loc.getY()));
        comps.add(gray("Z:", loc.getZ()));
        return comps;
    }

    private String truncate(double d) {
        return String.valueOf(d).split("\\.")[0];
    }

    private void sendNoPerms(CommandSender sender) {
        sender.sendMessage(Component.text("You are not authenticated enough to perform this command!", NamedTextColor.RED));
    }
    private Component gray(String s, Double d) {
        return Component.text(s + " " + truncate(d)).color(NamedTextColor.DARK_GRAY);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length != 1) return null;
        return Stream.of("display","clear","reload","savelog").filter( it -> it.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
    }

    //Listening
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (inventories.contains(e.getInventory())) e.setCancelled(true);
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (inventories.contains(e.getClickedInventory())) e.setCancelled(true);
    }

}
