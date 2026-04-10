package me.terrorbyte.test;

import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.reprogle.honeypot.common.providers.Behavior;
import org.reprogle.honeypot.common.providers.BehaviorProvider;
import org.reprogle.honeypot.common.providers.BehaviorType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Behavior(type = BehaviorType.CUSTOM, name = "chicken-storm", icon = Material.CHICKEN_SPAWN_EGG, configurable = true)
public class DemoBehavior extends BehaviorProvider {

    private static final int DEFAULT_CHICKEN_COUNT = 50;
    private static final int DEFAULT_DIE_AFTER = 10;
    private static final boolean DEFAULT_THROW_BLOCKS = false;
    private static final long MIN_GROWTH_TICKS = 40L;
    private static final long MAX_GROWTH_TICKS = 100L;
    private static final long MIN_ROAM_TARGET_TICKS = 30L;
    private static final long MAX_ROAM_TARGET_TICKS = 72L;
    private static final double ROAM_TARGET_REACHED_DISTANCE_SQUARED = 6.25;
    private static final int BLOCK_SAMPLE_ATTEMPTS = 48;
    private static final int OUTER_PULL_SAMPLE_ATTEMPTS = 24;
    private static final int MAX_THROWN_BLOCKS_PER_SWEEP = 8;
    private static final double BLOCK_PULL_RADIUS_MULTIPLIER = 1.9;
    private static final double BLOCK_PULL_RADIUS_BONUS = 3.5;
    private static final Set<Material> UNTHROWABLE_MATERIALS = EnumSet.of(
            Material.BEDROCK,
            Material.BARRIER,
            Material.END_PORTAL,
            Material.END_GATEWAY,
            Material.END_PORTAL_FRAME,
            Material.NETHER_PORTAL,
            Material.REINFORCED_DEEPSLATE,
            Material.LIGHT
    );

    @Override
    public boolean process(Player p, @Nullable Block block, @Nullable YamlDocument config) {
        int chickenCount = sanitize(config == null ? DEFAULT_CHICKEN_COUNT : config.getInt("chicken-count", DEFAULT_CHICKEN_COUNT), DEFAULT_CHICKEN_COUNT);
        int dieAfter = sanitize(config == null ? DEFAULT_DIE_AFTER : config.getInt("die-after", DEFAULT_DIE_AFTER), DEFAULT_DIE_AFTER);
        boolean throwBlocks = config != null && config.getBoolean("throw-blocks", DEFAULT_THROW_BLOCKS);

        Location origin = block != null
                ? block.getLocation().clone().add(0.5, 0.15, 0.5)
                : p.getLocation().clone().add(0.0, 0.15, 0.0);
        World world = origin.getWorld();

        if (world == null) {
            return false;
        }

        origin.setY(resolveTerrainAnchorY(world, origin, origin.getY()));

        List<Chicken> chickens = new ArrayList<>(chickenCount);
        long growthTicks = Math.clamp(chickenCount, MIN_GROWTH_TICKS, MAX_GROWTH_TICKS);
        long totalTicks = growthTicks + (20L * dieAfter);
        double maxRadius = Math.min(9.0, 1.75 + (chickenCount * 0.06));
        double maxHeight = Math.min(18.0, 3.0 + (chickenCount * 0.12));
        double leashRadius = Math.max(14.0, maxRadius * 4.8);
        Block protectedBlock = block;

        new BukkitRunnable() {
            private final Location tornadoCenter = origin.clone();
            private Location previousCenter = origin.clone();
            private long tick;
            private long nextRoamTargetTick;
            private double spin;
            private Location roamTarget = origin.clone();
            private final Vector drift = new Vector();

            @Override
            public void run() {
                if (!world.isChunkLoaded(tornadoCenter.getBlockX() >> 4, tornadoCenter.getBlockZ() >> 4)) {
                    cleanup(false);
                    cancel();
                    return;
                }

                if (tick >= totalTicks) {
                    cleanup(true);
                    cancel();
                    return;
                }

                double growth = easeOutCubic(Math.min(1.0, (double) tick / (double) growthTicks));
                int desiredChickens = Math.max(1, (int) Math.ceil(chickenCount * growth));

                while (chickens.size() < desiredChickens) {
                    chickens.add(spawnChicken(world, tornadoCenter));
                }

                updateMovement(growth, leashRadius);
                spin += 0.24 + (growth * 0.06);

                double currentRadius = 0.6 + (maxRadius * growth);
                double currentHeight = 1.5 + (maxHeight * growth);
                int liveChickens = 0;

                if (throwBlocks && tick % 3L == 0L) {
                    throwNearbyBlocks(world, previousCenter, tornadoCenter, currentRadius, currentHeight, spin, protectedBlock);
                }

                for (int i = 0; i < chickens.size(); i++) {
                    Chicken chicken = chickens.get(i);

                    if (!chicken.isValid()) {
                        continue;
                    }

                    liveChickens++;

                    double ratio = chickens.size() == 1 ? 0.5 : (double) i / (double) (chickens.size() - 1);
                    double spiralAngle = spin + (ratio * Math.PI * (6.0 + (growth * 2.0))) + (i * 0.35);
                    double radius = 0.45 + (currentRadius * Math.pow(ratio, 1.15));
                    double verticalOffset = 0.4 + (currentHeight * ratio) + (0.2 * Math.sin((tick * 0.22) + i));

                    Location target = tornadoCenter.clone().add(
                            Math.cos(spiralAngle) * radius,
                            verticalOffset,
                            Math.sin(spiralAngle) * radius
                    );
                    target.setYaw((float) Math.toDegrees(spiralAngle + Math.PI));
                    target.setPitch(-10.0F);

                    chicken.teleport(target);
                }

                if (liveChickens == 0) {
                    cancel();
                    return;
                }

                spawnParticles(world, tornadoCenter, currentRadius, currentHeight, spin, throwBlocks);

                if (tick % 12L == 0L) {
                    world.playSound(tornadoCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 0.35F, 1.4F);
                }

                previousCenter = tornadoCenter.clone();
                tick++;
            }

            private void updateMovement(double growth, double leashRadius) {
                double speed = 0.17 + (growth * 0.22);
                double roamRadius = Math.max(10.0, leashRadius * (0.7 + (growth * 0.25)));

                if (tick >= nextRoamTargetTick || horizontalDistanceSquared(tornadoCenter, roamTarget) <= ROAM_TARGET_REACHED_DISTANCE_SQUARED) {
                    roamTarget = pickRoamTarget(origin, roamRadius);
                    nextRoamTargetTick = tick + ThreadLocalRandom.current().nextLong(MIN_ROAM_TARGET_TICKS, MAX_ROAM_TARGET_TICKS + 1);
                }

                Vector desiredDirection = roamTarget.toVector().subtract(tornadoCenter.toVector());
                desiredDirection.setY(0.0);

                if (desiredDirection.lengthSquared() > 1.0E-4) {
                    desiredDirection.normalize().multiply(speed);
                    drift.multiply(0.5).add(desiredDirection.multiply(0.5));
                }

                Vector homePull = origin.toVector().subtract(tornadoCenter.toVector());
                homePull.setY(0.0);
                double distanceFromOrigin = homePull.length();

                if (distanceFromOrigin > leashRadius) {
                    drift.add(homePull.normalize().multiply(0.22 + (growth * 0.1)));
                } else if (distanceFromOrigin > leashRadius * 0.72) {
                    drift.add(homePull.normalize().multiply(0.04));
                }

                if (drift.lengthSquared() < (speed * speed * 0.12)) {
                    drift.add(randomHorizontalVector(speed * 0.18));
                }

                if (drift.lengthSquared() > (speed * speed)) {
                    drift.normalize().multiply(speed);
                }

                tornadoCenter.add(drift);
                tornadoCenter.setY(resolveTerrainAnchorY(world, tornadoCenter, tornadoCenter.getY()));
            }

            private void cleanup(boolean dramaticExit) {
                for (Chicken chicken : chickens) {
                    if (!chicken.isValid()) {
                        continue;
                    }

                    if (!dramaticExit) {
                        chicken.remove();
                        continue;
                    }

                    world.spawnParticle(Particle.CLOUD, chicken.getLocation().add(0.0, 0.4, 0.0), 8, 0.15, 0.25, 0.15, 0.02);
                    chicken.setInvulnerable(false);
                    chicken.setHealth(0.0);
                }

                if (dramaticExit) {
                    world.spawnParticle(Particle.CLOUD, tornadoCenter.clone().add(0.0, maxHeight * 0.4, 0.0), 40, maxRadius * 0.3, maxHeight * 0.2, maxRadius * 0.3, 0.03);
                    world.playSound(tornadoCenter, Sound.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 1.8F, 0.8F);
                }
            }
        }.runTaskTimer(Test.getPlugin(), 0L, 1L);

        Bukkit.getServer().broadcastMessage(throwBlocks
                ? "A chicken tornado is roaming and ripping up blocks. Take cover."
                : "A chicken tornado is roaming. Take cover.");

        return true;
    }

    private static int sanitize(int configuredValue, int fallback) {
        return configuredValue < 1 ? fallback : configuredValue;
    }

    private static Chicken spawnChicken(World world, Location center) {
        Chicken chicken = (Chicken) world.spawnEntity(center.clone().add(0.0, 0.6, 0.0), EntityType.CHICKEN);
        chicken.setAdult();
        chicken.setAgeLock(true);
        chicken.setAI(false);
        chicken.setGravity(false);
        chicken.setCollidable(false);
        chicken.setInvulnerable(true);
        chicken.setSilent(true);
        chicken.setRemoveWhenFarAway(true);
        return chicken;
    }

    private static void throwNearbyBlocks(World world, Location previousCenter, Location center, double radius, double height, double spin, @Nullable Block protectedBlock) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int thrown = 0;
        int travelSteps = Math.max(1, (int) Math.ceil(previousCenter.distance(center) * 2.5));
        double maxPullDistance = Math.max(14.0, (radius * 2.2) + 5.0);
        double maxPullDistanceSquared = maxPullDistance * maxPullDistance;
        double outerPullRadius = Math.clamp(radius + BLOCK_PULL_RADIUS_BONUS, radius * BLOCK_PULL_RADIUS_MULTIPLIER,
            maxPullDistance - 1.0);

        for (int attempt = 0; attempt < BLOCK_SAMPLE_ATTEMPTS && thrown < MAX_THROWN_BLOCKS_PER_SWEEP; attempt++) {
            double progress = random.nextDouble();
            Location sampleCenter = interpolate(previousCenter, center, progress);
            double heightRatio = random.nextDouble(0.05, 0.98);
            double shellRadius = Math.max(0.6, radius * Math.pow(heightRatio, 1.08));
            double ringBias = 0.72 + (random.nextDouble() * 0.46);
            double angle = spin + (heightRatio * Math.PI * 1.4) + random.nextDouble(0.0, Math.PI * 2.0);

            sampleCenter.add(
                    Math.cos(angle) * shellRadius * ringBias,
                    0.2 + (height * heightRatio),
                    Math.sin(angle) * shellRadius * ringBias
            );

            Block candidate = resolveThrowableBlock(world, sampleCenter, center, maxPullDistanceSquared, protectedBlock);

            if (candidate == null) {
                continue;
            }

            launchBlock(candidate, center, spin, random);
            thrown++;
        }

        for (int attempt = 0; attempt < OUTER_PULL_SAMPLE_ATTEMPTS && thrown < MAX_THROWN_BLOCKS_PER_SWEEP; attempt++) {
            double progress = random.nextDouble();
            Location sampleCenter = interpolate(previousCenter, center, progress);
            double angle = spin * 0.55 + random.nextDouble(0.0, Math.PI * 2.0);
            double distance = outerPullRadius * (0.55 + (random.nextDouble() * 0.45));
            Location samplePoint = sampleCenter.clone().add(
                    Math.cos(angle) * distance,
                    random.nextDouble(-0.45, 0.85),
                    Math.sin(angle) * distance
            );

            Block candidate = resolveThrowableBlock(world, samplePoint, center, maxPullDistanceSquared, protectedBlock);

            if (candidate == null) {
                continue;
            }

            launchBlock(candidate, center, spin, random);
            thrown++;
        }

        // Sweep the lowest part of the funnel along its path so terrain contact is based on the tornado body,
        // not just the center point.
        for (int step = 0; step <= travelSteps && thrown < MAX_THROWN_BLOCKS_PER_SWEEP; step++) {
            double progress = (double) step / (double) travelSteps;
            Location sampleCenter = interpolate(previousCenter, center, progress);
            int ringSamples = Math.max(8, (int) Math.ceil(radius * 2.2));
            double baseRadius = Math.max(1.1, radius * 1.1);

            for (int i = 0; i < ringSamples && thrown < MAX_THROWN_BLOCKS_PER_SWEEP; i++) {
                double angle = spin + (Math.PI * 2.0 * i / ringSamples);
                Location samplePoint = sampleCenter.clone().add(
                        Math.cos(angle) * baseRadius,
                        0.35,
                        Math.sin(angle) * baseRadius
                );

                Block candidate = resolveThrowableBlock(world, samplePoint, center, maxPullDistanceSquared, protectedBlock);

                if (candidate == null) {
                    continue;
                }

                launchBlock(candidate, center, spin, random);
                thrown++;
            }
        }
    }

    private static @Nullable Block resolveThrowableBlock(World world, Location samplePoint, Location tornadoCenter, double maxPullDistanceSquared, @Nullable Block protectedBlock) {
        int x = samplePoint.getBlockX();
        int y = samplePoint.getBlockY();
        int z = samplePoint.getBlockZ();

        for (int yOffset : new int[]{0, -1, 1, -2, 2}) {
            Block candidate = world.getBlockAt(x, y + yOffset, z);

            if (canThrowBlock(candidate, tornadoCenter, maxPullDistanceSquared, protectedBlock)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean canThrowBlock(Block block, Location tornadoCenter, double maxPullDistanceSquared, @Nullable Block protectedBlock) {
        Material type = block.getType();

        if (type.isAir() || !type.isSolid() || block.isLiquid() || UNTHROWABLE_MATERIALS.contains(type)) {
            return false;
        }

        if (protectedBlock != null
                && block.getWorld().equals(protectedBlock.getWorld())
                && block.getX() == protectedBlock.getX()
                && block.getY() == protectedBlock.getY()
                && block.getZ() == protectedBlock.getZ()) {
            return false;
        }

        if (block.getState() instanceof TileState) {
            return false;
        }

        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
            return false;
        }

        Location blockLocation = block.getLocation().clone().add(0.5, 0.5, 0.5);
        return !(blockLocation.distanceSquared(tornadoCenter.clone().add(0.0, 2.0, 0.0)) > maxPullDistanceSquared);
    }

    private static void launchBlock(Block block, Location center, double spin, ThreadLocalRandom random) {
        BlockData blockData = block.getBlockData().clone();
        Location spawnLocation = block.getLocation().clone().add(0.5, 0.1, 0.5);
        FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(spawnLocation, blockData);

        fallingBlock.setDropItem(false);
        fallingBlock.setCancelDrop(false);
        fallingBlock.setHurtEntities(false);

        Vector radial = spawnLocation.toVector().subtract(center.toVector());
        radial.setY(0.0);

        if (radial.lengthSquared() < 1.0E-4) {
            radial = randomHorizontalVector(1.0);
        } else {
            radial.normalize();
        }

        Vector tangential = new Vector(-radial.getZ(), 0.0, radial.getX());

        if (Math.sin(spin) < 0.0) {
            tangential.multiply(-1.0);
        }

        Vector launchVelocity = tangential.multiply(0.65 + random.nextDouble(0.35))
                .add(radial.multiply(0.45 + random.nextDouble(0.45)));
        launchVelocity.setY(0.7 + random.nextDouble(0.45));

        block.setType(Material.AIR, false);
        fallingBlock.setVelocity(launchVelocity);
        block.getWorld().spawnParticle(Particle.BLOCK, spawnLocation, 16, 0.22, 0.22, 0.22, blockData);
        block.getWorld().playSound(spawnLocation, Sound.BLOCK_STONE_BREAK, SoundCategory.BLOCKS, 0.35F, 0.8F + random.nextFloat() * 0.4F);
    }

    private static void spawnParticles(World world, Location center, double radius, double height, double spin, boolean throwBlocks) {
        world.spawnParticle(
                Particle.CLOUD,
                center.clone().add(0.0, height * 0.45, 0.0),
                Math.max(8, (int) Math.round(radius * 5.0)),
                radius * 0.35,
                height * 0.4,
                radius * 0.35,
                0.02
        );

        world.spawnParticle(Particle.END_ROD, center.clone().add(0.0, height + 0.5, 0.0), 2, 0.2, 0.2, 0.2, 0.0);

        Location orbitingCloud = center.clone().add(
                Math.cos(spin * 1.2) * Math.max(0.7, radius * 0.65),
                height * 0.65,
                Math.sin(spin * 1.2) * Math.max(0.7, radius * 0.65)
        );
        world.spawnParticle(Particle.CLOUD, orbitingCloud, 4, 0.12, 0.18, 0.12, 0.01);

        if (throwBlocks) {
            world.spawnParticle(Particle.SMOKE, center.clone().add(0.0, 0.8, 0.0), 6, radius * 0.2, 0.45, radius * 0.2, 0.01);
        }
    }

    private static double easeOutCubic(double value) {
        return 1.0 - Math.pow(1.0 - value, 3);
    }

    private static Vector randomHorizontalVector(double magnitude) {
        double angle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        return new Vector(Math.cos(angle), 0.0, Math.sin(angle)).multiply(magnitude);
    }

    private static double resolveTerrainAnchorY(World world, Location center, double fallbackY) {
        Block surfaceBlock = world.getHighestBlockAt(center.getBlockX(), center.getBlockZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);

        if (surfaceBlock.getType().isAir()) {
            return fallbackY;
        }

        return surfaceBlock.getY() + 1.15;
    }

    private static Location pickRoamTarget(Location origin, double maxRadius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.0, Math.PI * 2.0);
        double minRadius = maxRadius * 0.35;
        double distance = minRadius + ((maxRadius - minRadius) * Math.sqrt(random.nextDouble()));

        return origin.clone().add(
                Math.cos(angle) * distance,
                0.0,
                Math.sin(angle) * distance
        );
    }

    private static double horizontalDistanceSquared(Location first, Location second) {
        double deltaX = first.getX() - second.getX();
        double deltaZ = first.getZ() - second.getZ();
        return (deltaX * deltaX) + (deltaZ * deltaZ);
    }

    private static Location interpolate(Location start, Location end, double progress) {
        double clampedProgress = Math.clamp(progress, 0.0, 1.0);
        return start.clone().add(
                (end.getX() - start.getX()) * clampedProgress,
                (end.getY() - start.getY()) * clampedProgress,
                (end.getZ() - start.getZ()) * clampedProgress
        );
    }
}
