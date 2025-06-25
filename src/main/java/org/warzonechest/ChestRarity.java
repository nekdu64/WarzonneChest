package org.warzonechest;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.Map;
import java.util.Random;

public enum ChestRarity {
    COMMUN(ChatColor.GREEN + "Commun"),
    RARE(ChatColor.BLUE + "Rare"),
    MYTHIQUE(ChatColor.LIGHT_PURPLE + "Mythique"),
    LEGENDAIRE(ChatColor.RED + "Légendaire");

    private final String displayName;

    ChestRarity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ChestRarity fromString(String input) {
        for (ChestRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(input)) {
                return rarity;
            }
        }
        return null;
    }
}
