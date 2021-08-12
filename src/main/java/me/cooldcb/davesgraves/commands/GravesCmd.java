package me.cooldcb.davesgraves.commands;

import me.cooldcb.davesgraves.DavesGraves;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GravesCmd implements CommandExecutor, TabCompleter {
    private final DavesGraves plugin = DavesGraves.getInstance();

    private final NamespacedKey key = new NamespacedKey(plugin, "Grave");

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
            for (World w : Bukkit.getWorlds()) {
                for (Entity e :  w.getEntities()) {
                    final var container = e.getPersistentDataContainer();
                    if (container.has(key, PersistentDataType.STRING)) {
                        e.remove();
                    }
                }
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
