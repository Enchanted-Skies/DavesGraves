package me.zeddit.graves;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;


public class GraveCommand implements CommandExecutor {

    private final GraveCreator creator;
    private final GraveLogger logger;

    public GraveCommand(GraveCreator creator, GraveLogger logger) {
        this.creator = creator;
        this.logger = logger;
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

    private void clear(CommandSender sender) {
        if (!sender.hasPermission("graves.clear")) {
            sendNoPerms(sender);
            return;
        }
        final AtomicInteger count = new AtomicInteger(0);
        Bukkit.getWorlds().stream()
                .flatMap(it -> it.getEntities().stream())
                .filter(it -> it instanceof ArmorStand)
                .filter(it -> it.getPersistentDataContainer().has(GraveKeys.GRAVE_OWNER.getKey(), PersistentDataType.STRING))
                .forEach(it -> {it.remove(); count.getAndIncrement();});
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
    private void sendNoPerms(CommandSender sender) {
        sender.sendMessage(Component.text("You are not authenticated enough to perform this command!", NamedTextColor.RED));
    }
}
