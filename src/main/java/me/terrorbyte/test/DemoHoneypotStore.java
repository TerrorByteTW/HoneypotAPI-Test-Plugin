package me.terrorbyte.test;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.reprogle.honeypot.common.storageproviders.HoneypotBlockObject;
import org.reprogle.honeypot.common.storageproviders.HoneypotPlayerHistoryObject;
import org.reprogle.honeypot.common.storageproviders.HoneypotStore;
import org.reprogle.honeypot.common.storageproviders.StorageProvider;

import java.util.List;
import java.util.logging.Logger;

@HoneypotStore(name = "DemoHoneypotStore")
public class DemoHoneypotStore extends StorageProvider {
    @Override
    public void createHoneypotBlock(Block block, String s) {
        Logger.getLogger("minecraft").info("Create honeypot block was called!");
    }

    @Override
    public void removeHoneypotBlock(Block block) {
        Logger.getLogger("minecraft").info("Remove honeypot block was called!");
    }

    @Override
    public boolean isHoneypotBlock(Block block) {
        Logger.getLogger("minecraft").info("Is Honeypot block was called!");
        return false;
    }

    @Override
    public HoneypotBlockObject getHoneypotBlock(Block block) {
        Logger.getLogger("minecraft").info("Get honeypot block was called!");
        return null;
    }

    @Override
    public String getAction(Block block) {
        Logger.getLogger("minecraft").info("Get action was called!");
        return "";
    }

    @Override
    public void deleteAllHoneypotBlocks(@Nullable World world) {
        Logger.getLogger("minecraft").info("Delete all honeypot blocks was called!");
    }

    @Override
    public List<HoneypotBlockObject> getAllHoneypots(@Nullable World world) {
        Logger.getLogger("minecraft").info("Get all honeypot blocks was called!");
        return List.of();
    }

    @Override
    public List<HoneypotBlockObject> getNearbyHoneypots(Location location, int i) {
        Logger.getLogger("minecraft").info("Get nearby honeypot blocks was called!");
        return List.of();
    }

    @Override
    public void addPlayer(Player player, int i) {
        Logger.getLogger("minecraft").info("Add player was called!");
    }

    @Override
    public void setPlayerCount(Player player, int i) {
        Logger.getLogger("minecraft").info("Set player count was called!");
    }

    @Override
    public int getCount(Player player) {
        Logger.getLogger("minecraft").info("Get player count was called!");
        return 0;
    }

    @Override
    public int getCount(OfflinePlayer offlinePlayer) {
        Logger.getLogger("minecraft").info("Get player count was called!");
        return 0;
    }

    @Override
    public void deleteAllHoneypotPlayers() {
        Logger.getLogger("minecraft").info("Delete all honeypot players was called!");
    }

    @Override
    public void addPlayerHistory(Player player, HoneypotBlockObject honeypotBlockObject, String s) {
        Logger.getLogger("minecraft").info("Add player history was called!");
    }

    @Override
    public List<HoneypotPlayerHistoryObject> getPlayerHistory(Player player) {
        Logger.getLogger("minecraft").info("Get player history was called!");
        return List.of();
    }

    @Override
    public void deletePlayerHistory(Player player, int... ints) {
        Logger.getLogger("minecraft").info("Delete player history was called!");
    }

    @Override
    public void deleteAllHistory() {
        Logger.getLogger("minecraft").info("Delete all history was called!");
    }
}
