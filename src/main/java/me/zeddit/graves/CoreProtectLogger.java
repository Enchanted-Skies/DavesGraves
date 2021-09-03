package me.zeddit.graves;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;


import static org.bukkit.Bukkit.getServer;

public class CoreProtectLogger {

    private final boolean isEnabled;
    private CoreProtectAPI api;

    public CoreProtectLogger() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");
        if (!(plugin instanceof CoreProtect)) {
            isEnabled =false;
            logFailure();
            return;
        }
        CoreProtectAPI cp = ((CoreProtect) plugin).getAPI();
        if (!cp.isEnabled()) {
            isEnabled =false;
            logFailure();
            return;
        }
        if (cp.APIVersion() < 7) {
            isEnabled = false;
            logFailure();
            return;
        }
        api =cp;
        isEnabled = true;
        api.testAPI();
        Bukkit.getLogger().info("Loaded CoreProtectAPI successfully!");
    }

    private void logFailure() {
        Bukkit.getLogger().warning("Could not find/load CoreProtectAPI!");
    }

    public void logInventory(Player owner, Location loc) {
        if (!isEnabled) return;
        api.logContainerTransaction(owner.getName(), loc);
    }
}
