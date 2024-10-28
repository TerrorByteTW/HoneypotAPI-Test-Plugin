package me.terrorbyte.test;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;
import org.reprogle.honeypot.Registry;
import org.reprogle.honeypot.api.events.HoneypotPrePlayerBreakEvent;
import org.reprogle.honeypot.api.events.HoneypotPrePlayerInteractEvent;

public class Test extends JavaPlugin implements Listener {

    public static boolean testActive = false;
    public static Test plugin;

    @EventHandler
    public static void HoneypotPrePlayerBreakEvent(HoneypotPrePlayerBreakEvent event) {
        if (testActive) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Test is active so break event is cancelled!");
        }
        event.getPlayer().sendMessage("You threw the event!");
    }

    @EventHandler
    public static void HoneypotPrePlayerInteractEvent(HoneypotPrePlayerInteractEvent event) {
        if (testActive) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Test is active so interact event is cancelled!");
        }
        event.getPlayer().sendMessage("You threw the event!");
    }

    public static Test getPlugin() {
        return plugin;
    }

    // This is how you would register behavior providers
    @Override
    public void onLoad() {
        Registry.getBehaviorRegistry().register(new DemoBehavior());
        Registry.getStorageManagerRegistry().register(new DemoHoneypotStore());
    }

    @Override
    public void onEnable() {
        plugin = this;
        this.getLogger().info("Enabling!");
        getCommand("testcommand").setExecutor(this);
        getCommand("checkblock").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.getLogger().info("Disabling!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, @NotNull String[] args) {

        // Enable event cancelling
        if (label.equalsIgnoreCase("testcommand")) {
            if (testActive) {
                testActive = false;
                sender.sendMessage("Honeypot event cancelling is off!");
                return true;
            } else {
                testActive = true;
                sender.sendMessage("Honeypot event cancelling is on!");
                return true;
            }

            // Use the HoneypotBlockManager API to check for Honeypot Blocks. This is just an example, refer to the Javadoc for more info. Only applicable in v2.1.1+
        } else if (label.equalsIgnoreCase("checkblock")) {
            Player player = (Player) sender;

            // Starting in version 2.5.1, creating a new instance of the manager class is deprecated. Starting in the
            // immediate next version, this functionality will be completely removed. Please be sure to update your plugins by then
            // HoneypotBlockManager hbm = new HoneypotBlockManager();

            // If the target block exactly 5 blocks away is a Honeypot, let the player no
            if (Registry.getStorageProvider().isHoneypotBlock(player.getTargetBlockExact(5))) {
                player.sendMessage("I'm a Honeypot! :)");
            } else {
                player.sendMessage("I'm not a Honeypot :(");
            }
            return true;
        }


        return false;
    }

}
