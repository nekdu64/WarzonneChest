package org.warzonechest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public final class WarzoneChestPlugin extends JavaPlugin {

    private final Map<Location, ChestRarity> activeChests = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("WzChest").setExecutor(new ChestSpawnCommand(this));
        Bukkit.getPluginManager().registerEvents(new ChestListener(this), this);
        scheduleFixedChestSpawns();
        getCommand("wzchest").setTabCompleter(new WzChestTabCompleter());
    }

    public Map<Location, ChestRarity> getActiveChests() {
        return activeChests;
    }


    public void scheduleFixedChestSpawns() {
        ConfigurationSection horaires = getConfig().getConfigurationSection("coffres.horaires");
        if (horaires == null) return;

        for (String heureStr : horaires.getKeys(false)) {
            String[] split = heureStr.split(":");
            if (split.length != 2) continue;

            int targetHour = Integer.parseInt(split[0]);
            int targetMinute = Integer.parseInt(split[1]);

            long delayTicks = getDelayUntil(targetHour, targetMinute);
            getServer().getScheduler().runTaskLater(this, () -> spawnChestFromConfig(heureStr), delayTicks);

            getLogger().info("Spawn prévu à " + heureStr + " dans " + (delayTicks / 20 / 60) + " minutes.");
        }
    }

    private long getDelayUntil(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime target = now.withHour(hour).withMinute(minute).withSecond(0);

        if (target.isBefore(now)) {
            target = target.plusDays(1);
        }

        Duration duration = Duration.between(now, target);
        return duration.getSeconds() * 20;
    }

    public void spawnChestFromConfig(String horaireKey) {
        List<Map<?, ?>> rarities = getConfig().getMapList("coffres.horaires." + horaireKey);
        if (rarities.isEmpty()) return;

        List<Map.Entry<ChestRarity, Integer>> chances = new ArrayList<>();
        for (Map<?, ?> entry : rarities) {
            ChestRarity rarity = ChestRarity.fromString(String.valueOf(entry.get("rarete")));
            int poids = (int) entry.get("poids");
            if (rarity != null) {
                chances.add(new AbstractMap.SimpleEntry<>(rarity, poids));
            }
        }

        ChestRarity selected = getRandomRarity(chances);
        Location location = getRandomLocationFromConfig(selected);

        if (location != null) {
            int delayMinutes =  this.getConfig().getInt("coffres.delay", 5);
            spawnChestWithDelay(location, selected,delayMinutes);
        }
    }

    public ChestRarity getRandomRarity(List<Map.Entry<ChestRarity, Integer>> rarities) {
        int total = rarities.stream().mapToInt(Map.Entry::getValue).sum();
        int rand = new Random().nextInt(total);
        int cumulative = 0;
        for (Map.Entry<ChestRarity, Integer> entry : rarities) {
            cumulative += entry.getValue();
            if (rand < cumulative) return entry.getKey();
        }
        return rarities.get(0).getKey(); // fallback
    }

    public Location getRandomLocationFromConfig(ChestRarity rarity) {
        List<String> locs = getConfig().getStringList("coffres." + rarity.name().toLowerCase() + ".locations");
        if (locs.isEmpty()) return null;

        String raw = locs.get(new Random().nextInt(locs.size()));
        String[] parts = raw.split(":");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);

        return new Location(world, x, y, z);
    }

    public void spawnChestWithDelay(Location location, ChestRarity rarity, int delayMinutes) {
        long delayTicks = delayMinutes * 60L * 20L;
        long currentTicks = 0;
        int delaySeconds = delayMinutes * 60;

        // Planning des annonces intermédiaires
        for (int remaining = delaySeconds; remaining > 0; remaining--) {
            int r = remaining;

            int finalRemaining = remaining;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                boolean shouldAnnounce = false;

                if (r % 600 == 0 && r > 1200) { // toutes les 10 minutes (si > 20 min)
                    shouldAnnounce = true;
                } else if (r % 300 == 0 && r > 300) { // toutes les 5 minutes (entre 20 et 5 min)
                    shouldAnnounce = true;
                } else if (r % 60 == 0 && r > 60) { // chaque minute (> 1 min)
                    shouldAnnounce = true;
                } else if (r == 30 || r == 10) {
                    shouldAnnounce = true;
                }

                if (r <= 5) {
                    location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0.5, r, 0.5), 30, 0.3, 0.3, 0.3, 0.05);
                    location.getWorld().playSound(location.clone().add(0.5, r, 0.5), Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, 5, 1);
                }

                if (shouldAnnounce|| finalRemaining == delaySeconds) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "[Warzone] Un coffre " + rarity.getDisplayName() +
                            ChatColor.YELLOW + " apparaîtra en " + ChatColor.AQUA + formatLocation(location) +
                            ChatColor.YELLOW + " dans "+ formatRemainingTime(r));
                }
            }, 20L * (delaySeconds - r));
        }

        // Tâche finale pour faire apparaître le coffre
        Bukkit.getScheduler().runTaskLater(this, () -> spawnChest(location, rarity), delayTicks);
    }

    private String formatRemainingTime(int seconds) {
        if (seconds >= 60) {
            int min = seconds / 60;
            return min + " minute" + (min > 1 ? "s" : "");
        } else {
            return seconds + " seconde" + (seconds > 1 ? "s" : "");
        }
    }


    public void spawnChest(Location location, ChestRarity rarity) {
        Block block = location.getBlock();
        block.setType(Material.TRAPPED_CHEST);
        this.getActiveChests().put(location, rarity);

        Bukkit.broadcastMessage(ChatColor.GREEN + "[Warzone] Le coffre " + rarity.getDisplayName() + ChatColor.GREEN + " est apparu en " + ChatColor.AQUA + formatLocation(location) + ChatColor.GREEN + " !");
    }

    private String formatLocation(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
