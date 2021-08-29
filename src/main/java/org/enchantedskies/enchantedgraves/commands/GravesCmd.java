package org.enchantedskies.enchantedgraves.commands;

import org.enchantedskies.enchantedgraves.EnchantedGraves;
import org.enchantedskies.enchantedgraves.Grave;
import org.bukkit.ChatColor;
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
    private final EnchantedGraves plugin = EnchantedGraves.getInstance();
    private final NamespacedKey graveKey = new NamespacedKey(EnchantedGraves.getInstance(), "Grave");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            PluginDescriptionFile pdf = plugin.getDescription();
            sender.sendMessage("You are currently running EnchantedGraves Version: " + pdf.getVersion());
        }
        if (args.length == 1 && args[0].equals("reload")) {
            if (!sender.hasPermission("enchantedgraves.command.reload")) {
                sender.sendMessage("§cInsufficient permissions");
                return true;
            }
            EnchantedGraves.configManager.reloadConfig();
            sender.sendMessage("§aConfig reloaded");
        }
        if (sender instanceof Player player) {
            if (args.length == 1 && args[0].equals("kill")) {
                if (!sender.hasPermission("enchantedgraves.command.kill")) {
                    sender.sendMessage("§cInsufficient permissions");
                    return true;
                }
                int tally = 0;
                World world = player.getWorld();
                for (Entity entity :  world.getEntities()) {
                    if (!(entity instanceof ArmorStand armorStand)) continue;
                    String graveContainer = armorStand.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
                    if (graveContainer == null) continue;
                    entity.remove();
                    tally++;
                }
                player.sendMessage(ChatColor.GREEN + "Removed " + tally + (tally == 1 ? " entity!" : " entities!"));
            } else if (args.length == 1 && args[0].equals("break")) {
                if (!player.hasPermission("enchantedgraves.command.kill")) {
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
                    EnchantedGraves.dataManager.breakGrave(grave);
                }
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, String[] args) {
        List<String> tabComplete = new ArrayList<>();
        List<String> wordCompletion = new ArrayList<>();
        boolean wordCompletionSuccess = false;
        if (args.length == 1) {
            if (commandSender.hasPermission("enchantedgraves.command.reload")) tabComplete.add("reload");
            if (commandSender.hasPermission("enchantedgraves.command.kill")) tabComplete.add("kill");
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
