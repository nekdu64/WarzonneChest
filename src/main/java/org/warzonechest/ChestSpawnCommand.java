package org.warzonechest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.bukkit.Effect.Type.SOUND;

public class ChestSpawnCommand implements CommandExecutor {

    private final WarzoneChestPlugin plugin;

    public ChestSpawnCommand(WarzoneChestPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("WzChest")) return false;
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage("§eConfig rechargée.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "/WzChest spawn <rarete> [delay]");
            sender.sendMessage(ChatColor.RED + "/WzChest reload");
            return true;
        }
        if (args[0].equalsIgnoreCase("spawn")) {

            ChestRarity rarity = ChestRarity.fromString(args[1]);
            if (rarity == null) {
                sender.sendMessage(ChatColor.RED + "Rarete invalide. Choisissez: COMMUN, RARE, MYTHIQUE, LEGENDAIRE.");
                return true;
            }

            FileConfiguration config = plugin.getConfig();
            int delayMinutes = args.length == 3 ? Integer.parseInt(args[2]) : config.getInt("coffres.delay", 5);
            List<String> locationList = config.getStringList("coffres." + rarity.name().toLowerCase() + ".locations");

            if (locationList.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Aucune position configurée pour cette rareté.");
                return true;
            }

            // Choisir une position aléatoire
            String locationString = locationList.get(new Random().nextInt(locationList.size()));
            Location location = parseLocation(locationString);

            if (location == null) {
                sender.sendMessage(ChatColor.RED + "Position invalide : " + locationString);
                return true;
            }

            plugin.spawnChestWithDelay(location, rarity, delayMinutes);

            return true;
        }

        return false;
    }


    private Location parseLocation(String locString) {
        try {
            String[] parts = locString.split(":");
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatLocation(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }


}