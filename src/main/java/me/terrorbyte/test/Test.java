package me.terrorbyte.test;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import me.terrorbyte.honeypot.api.events.HoneypotPrePlayerBreakEvent;
import me.terrorbyte.honeypot.api.events.HoneypotPrePlayerInteractEvent;

public class Test extends JavaPlugin implements Listener {
    
    public static boolean testActive = false; 
    public Test plugin;

    @Override
    public void onEnable(){
        plugin = this;
        this.getLogger().info("Enabling!");
        getCommand("testcommand").setExecutor(this);
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
        if (testActive) {
            testActive = false;
            ((Player) sender).sendMessage("Off!");
        } else {
            testActive = true;
            ((Player) sender).sendMessage("On!");
        }

        return true;
    }

}
