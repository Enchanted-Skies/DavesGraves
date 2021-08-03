package me.cooldcb.davesgraves.commands;

import me.cooldcb.davesgraves.DavesGraves;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.List;

public class GravesCmd implements CommandExecutor, TabCompleter {
    private final DavesGraves plugin = DavesGraves.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        } else {
            PluginDescriptionFile pdf = plugin.getDescription();
            player.sendMessage("You are currently running DavesGraves Version: " + pdf.getVersion());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        List<String> tabComplete = new ArrayList<>();
        List<String> wordCompletion = new ArrayList<>();
        boolean wordCompletionSuccess = false;
        if (args.length == 1) {
            if (commandSender.hasPermission("davesgraves.command.reload")) tabComplete.add("reload");
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
