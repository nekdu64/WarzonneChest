package org.warzonechest;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WzChestTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = List.of("spawn", "reload");
    private static final List<String> RARITIES = List.of("commun", "rare", "mythique", "legendaire");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length == 2) {
                return RARITIES.stream()
                        .filter(r -> r.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                return List.of("1", "3", "5", "10", "20", "30", "60") // exemples de délais en minutes
                        .stream()
                        .filter(d -> d.startsWith(args[2]))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

}