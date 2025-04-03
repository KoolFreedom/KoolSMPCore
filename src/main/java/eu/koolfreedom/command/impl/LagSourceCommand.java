package eu.koolfreedom.command.impl;

import eu.koolfreedom.command.FreedomCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LagSourceCommand extends FreedomCommand
{
    @Override
    public boolean run(CommandSender sender, Player playerSender, Command cmd, String commandLabel, String[] args, boolean senderIsConsole)
    {
        HashMap<Chunk, Integer> chunks = getChunkEntities();

        sender.sendMessage(Component.text("Chunk Information:", NamedTextColor.RED)
                .appendNewline()
                .appendNewline()
                .append(Component.text("Loaded Chunks: ", NamedTextColor.RED))
                .append(Component.text(getLoadedChunks().size(), NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("Potential lag sources:", NamedTextColor.RED))
                .appendNewline());

        int badChunks = 0;
        for (Map.Entry<Chunk, Integer> entry : chunks.entrySet()) {
            Chunk chunk = entry.getKey();
            int entities = entry.getValue();

            if (entry.getValue() > 32) {
                int x = chunk.getX() << 4;
                int z = chunk.getZ() << 4;
                sender.sendMessage(Component.text("Potential lag source at ", NamedTextColor.RED)
                        .append(Component.text("X: " + x + " Z: " + z + " ", NamedTextColor.GOLD))
                        .append(Component.text("containing ", NamedTextColor.RED))
                        .append(Component.text(entities + " ", NamedTextColor.GOLD))
                        .append(Component.text("(Tile) entities.", NamedTextColor.RED))
                        .clickEvent(ClickEvent.runCommand("/tp " + x + " " + (chunk.getWorld().getHighestBlockYAt(x, z) + 1) + " " + z))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport.", NamedTextColor.GREEN))));
                badChunks++;
            }
        }

        if (badChunks == 0) {
            sender.sendMessage(Component.text("No potential lag source detected.", NamedTextColor.GREEN));
        }
        return true;
    }

    private HashMap<Chunk, Integer> getChunkEntities() {
        HashMap<Chunk, Integer> chunks = new HashMap<>();
        for (Chunk chunk : getLoadedChunks()) {
            chunks.put(chunk, (chunk.getEntities().length + chunk.getTileEntities().length));
        }
        return chunks;
    }

    private ArrayList<Chunk> getLoadedChunks() {
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            chunks.addAll(Arrays.asList(world.getLoadedChunks()));
        }
        return chunks;
    }
}