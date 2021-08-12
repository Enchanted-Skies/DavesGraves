package me.cooldcb.davesgraves.commands;

import me.cooldcb.davesgraves.DavesGraves;
import me.cooldcb.davesgraves.Grave;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GravesCmd implements CommandExecutor, TabCompleter {
    private final DavesGraves plugin = DavesGraves.getInstance();
    private final NamespacedKey graveKey = new NamespacedKey(DavesGraves.getInstance(), "Grave");

    @Override
    public boolean onCommand(@NotNull CommandSender sender,@NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot run this command!");
            return true;
        }
        if (args.length == 1 && args[0].equals("reload")) {
            if (!player.hasPermission("davesgraves.command.reload")) {
                player.sendMessage("§cInsufficient permissions");
                return true;
            }
            DavesGraves.configManager.reloadConfig();
            player.sendMessage("§aConfig reloaded");
        } else if (args.length == 1 && args[0].equals("kill")) {
            if (!player.hasPermission("davesgraves.command.kill")) {
                player.sendMessage("§cInsufficient permissions");
                return true;
            }
            World world = player.getWorld();
            for (Entity entity :  world.getEntities()) {
                if (!(entity instanceof ArmorStand armorStand)) continue;
                String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
                if (graveContainer == null) continue;
                entity.remove();
            }
        } else if (args.length == 1 && args[0].equals("break")) {
            if (!player.hasPermission("davesgraves.command.kill")) {
                player.sendMessage("§cInsufficient permissions");
                return true;
            }
            World world = player.getWorld();
            for (Entity entity :  world.getEntities()) {
                if (!(entity instanceof ArmorStand armorStand)) continue;
                String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
                if (graveContainer == null) continue;
                String[] containerData = graveContainer.split("\\|");
                Grave grave = new Grave(UUID.fromString(containerData[0]), Integer.parseInt(containerData[1]));
                if (!grave.isValid()) entity.remove();
                DavesGraves.dataManager.breakGrave(grave);
            }
        } else {
            PluginDescriptionFile pdf = plugin.getDescription();
            player.sendMessage("You are currently running DavesGraves Version: " + pdf.getVersion());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> tabComplete = new ArrayList<>();
        List<String> wordCompletion = new ArrayList<>();
        boolean wordCompletionSuccess = false;
        if (args.length == 1) {
            if (commandSender.hasPermission("davesgraves.command.reload")) tabComplete.add("reload");
            if (commandSender.hasPermission("davesgraves.command.kill")) tabComplete.add("kill");
        }

        for (String currTab : tabComplete) {
            int currArg = args.length - 1;
            if (currTab.startsWith(args[currArg])) {
                wordCompletion.add(currTab);
                wordCompletionSuccess = true;
            }
        }
        if (wordCompletionSuccess) return wordCompletion;
        return tabComplete;
    }
}
