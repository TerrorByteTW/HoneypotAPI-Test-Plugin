package me.terrorbyte.test;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.reprogle.honeypot.api.events.HoneypotPrePlayerBreakEvent;
import org.reprogle.honeypot.api.events.HoneypotPrePlayerInteractEvent;
import org.reprogle.honeypot.storagemanager.HoneypotBlockManager;

public class Test extends JavaPlugin implements Listener {
    
    public static boolean testActive = false; 
    public Test plugin;

    @Override
    public void onEnable(){
        plugin = this;
        this.getLogger().info("Enabling!");
        getCommand("testcommand").setExecutor(this);
        getCommand("checkblock").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable(){
        this.getLogger().info("Disabling!");
    }

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Enable event cancelling
        if(label.equalsIgnoreCase("testcommand")) {
            if (testActive) {
                testActive = false;
                ((Player) sender).sendMessage("Honeypot event cancelling is off!");
                return true;
            } else {
                testActive = true;
                ((Player) sender).sendMessage("Honeypot event cancelling is on!");
                return true;
            }
        
        // Use the HoneypotBlockManager API to check for Honeypot Blocks. This is just an example, refer to the Javadoc for more info. Only applicable in v2.1.1+
        } else if (label.equalsIgnoreCase("checkblock")) {
            Player player = (Player) sender;

            // Create a HoneypotBlockManager object to manage Honeypot Blocks safely
            HoneypotBlockManager hbm = new HoneypotBlockManager();

            // If the target block exactly 5 blocks away is a Honeypot, let the player no
            if(hbm.isHoneypotBlock(player.getTargetBlockExact(5))) {
                player.sendMessage("I'm a Honeypot! :)");
                return true;
            } else {
                player.sendMessage("I'm not a Honeypot :(");
                return true;
            }
        }
        

        return false;
    }

}
