package me.terrorbyte.test;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import org.reprogle.honeypot.common.providers.Behavior;
import org.reprogle.honeypot.common.providers.BehaviorProvider;
import org.reprogle.honeypot.common.providers.BehaviorType;

import java.util.Objects;

@Behavior(type = BehaviorType.CUSTOM, name = "chicken-storm", icon = Material.CHICKEN_SPAWN_EGG, configurable = true)
public class DemoBehavior extends BehaviorProvider {

    private BukkitTask task;

    @Override
    public boolean process(Player p, @Nullable Block block, @Nullable YamlDocument config) {

        // Over the course of 2.5 seconds, summon 50 chickens.
        // Then, 10 seconds after *each individual chicken* is summoned, kill it and play the Enderman death sound >:)
        final int[] j = {0};

        int chickenCount = config == null ? 50 : config.getInt("chicken-count", 50);
        int dieAfter = config == null ? 10 : config.getInt("die-after", 10);

        if (chickenCount < 1) chickenCount = 50;
        if (dieAfter < 1) dieAfter = 10;

        // Must be copied to effectively final variable to be used in runTaskTimer()
        final int finalChickenCount = chickenCount;
        final int finalDieAfter = dieAfter;

        task = Bukkit.getScheduler().runTaskTimer(Test.getPlugin(), () -> {
            if (j[0] > finalChickenCount) {
                task.cancel();
                return;
            } else {
                j[0]++;
            }

            Chicken chicken = (Chicken) Objects.requireNonNull(Bukkit.getWorld(p.getWorld().getName()).spawnEntity(p.getLocation().add(0, 1, 0), EntityType.CHICKEN));
            chicken.setAdult();

            new BukkitRunnable() {
                @Override
                public void run() {
                    chicken.setHealth(0.0);
                    p.playSound(chicken.getLocation(), Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 2, 1);
                }
            }.runTaskLater(Test.getPlugin(), 20L * finalDieAfter);
        }, 0L, 1L);

        Bukkit.getServer().broadcastMessage("The chicken storm has arrived, good luck");

        return true;
    }
}
