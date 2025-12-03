package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.annotation.CommandParameters;
import eu.koolfreedom.command.KoolCommand;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.stream.Collectors;

@CommandParameters(name = "lagcheck", description = "Check for potential server lag sources", usage = "/lagcheck", aliases = {"lagsource"})
public class LagCheckCommand extends KoolCommand
{
    @Override
    public boolean run(CommandSender sender, Player player, Command cmd, String label, String[] args)
    {
        msg(sender, "<gold><bold>Lag Diagnostics Report:</bold></gold>");

        // TPS Info
        double[] tps = Bukkit.getServer().getTPS();
        msg(sender, "<gray>TPS:</gray> <green><tps1></green> <yellow><tps2></yellow> <red><tps3></red>",
                Placeholder.unparsed("tps1", formatTps(tps[0])),
                Placeholder.unparsed("tps2", formatTps(tps[1])),
                Placeholder.unparsed("tps3", formatTps(tps[2])));

        // Memory Info
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long usedMB = heap.getUsed() / 1024 / 1024;
        long maxMB = heap.getMax() / 1024 / 1024;
        msg(sender, "<gray>Memory:</gray> <yellow>" + usedMB + "MB</yellow> / <gray>" + maxMB + "MB</gray>");

        // Chunk and Entity Info per World
        for (World world : Bukkit.getWorlds()) {
            int chunkCount = world.getLoadedChunks().length;
            int entityCount = world.getEntities().size();
            msg(sender, "<aqua>World:</aqua> <green>" + world.getName() + "</green> - <yellow>Chunks:</yellow> " + chunkCount + ", <yellow>Entities:</yellow> " + entityCount);
        }

        // Heaviest Chunks (by entities)
        World overworld = Bukkit.getWorlds().get(0); // typically overworld
        List<Chunk> heavyChunks = Arrays.stream(overworld.getLoadedChunks())
                .sorted(Comparator.comparingInt(c -> -c.getEntities().length))
                .limit(5)
                .collect(Collectors.toList());

        msg(sender, "<red>Top 5 Entity-Heavy Chunks (" + overworld.getName() + "):</red>");
        for (Chunk c : heavyChunks) {
            msg(sender, " - <gray>" + c.getX() + ", " + c.getZ() + ":</gray> <yellow>" + c.getEntities().length + " entities</yellow>");
        }

        return true;
    }

    private String formatTps(double tps) {
        return String.format(Locale.US, "%.2f", Math.min(tps, 20.0));
    }
}
