package org.warzonechest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class ChestListener implements Listener {

    private final WarzoneChestPlugin plugin;

    public ChestListener(WarzoneChestPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.TRAPPED_CHEST) return;

        Location loc = block.getLocation();

        if (!plugin.getActiveChests().containsKey(loc)) return;

        ChestRarity rarity = plugin.getActiveChests().get(loc);
        Player player = event.getPlayer();

        giveRandomReward(player , rarity);

        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " a ouvert un coffre " + rarity + " !");
        loc.getBlock().setType(Material.AIR);
        plugin.getActiveChests().remove(loc);

        event.setCancelled(true);
    }
    public void giveRandomReward(Player player, ChestRarity rarity) {
        List<String> rewards = plugin.getConfig().getStringList("coffres." + rarity.name().toLowerCase() + ".rewards");

        if (rewards.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Aucune récompense configurée pour cette rareté.");
            return;
        }

        String rewardCommand = rewards.get(new Random().nextInt(rewards.size()));
        rewardCommand = rewardCommand.replace("%player%", player.getName());

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);
    }
}