package eu.koolfreedom.bridge;

import java.awt.*;
import java.util.*;

import eu.koolfreedom.config.ConfigEntry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class GroupManagement
{
    private final Map<String, Group> groups = new HashMap<>();
    private final Group defaultFallbackGroup = new Group("default", "Member", NamedTextColor.GRAY);
    private final Group consoleGroup = new Group("console", "Console", NamedTextColor.GREEN);

    private Permission permission = null;
    private boolean checked = false;

    public GroupManagement()
    {
        loadGroups();
    }

    public void loadGroups()
    {
        groups.clear();

        ConfigurationSection section = ConfigEntry.GROUPS.getConfigurationSection();
        section.getKeys(false).stream().filter(section::isConfigurationSection).forEach(entry ->
				groups.put(entry, Group.fromConfigurationSection(entry, Objects.requireNonNull(section.getConfigurationSection(entry)))));
    }

    public Group getGroupByName(String name)
    {
        return groups.getOrDefault(name, defaultFallbackGroup);
    }

    public Group getGroupByNameOr(String name, Group group)
    {
        return groups.getOrDefault(name, group);
    }

    public boolean isGroupPresent(String name)
    {
        return groups.containsKey(name);
    }

    public Group getSenderGroup(CommandSender sender)
    {
        if (sender instanceof Player player)
        {
            // Get the player's primary group
            try
            {
                return getGroupByName(Objects.requireNonNull(getVaultPermissions().getPrimaryGroup(player)));
            }
            // Fallback in case the permission plugin installed does not support groups
            catch (UnsupportedOperationException wtf)
            {
                return defaultFallbackGroup;
            }
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            return consoleGroup;
        }
        else
        {
            return getGroupByName("default");
        }
    }

    public Permission getVaultPermissions()
    {
        if (permission == null && !checked)
        {
            RegisteredServiceProvider<Permission> provider = Bukkit.getServicesManager().getRegistration(Permission.class);

            if (provider != null)
            {
                permission = provider.getProvider();
            }

            this.checked = true;
        }

        return permission;
    }

    public Component getColoredName(CommandSender sender)
    {
        Objects.requireNonNull(sender);
        return Component.text(sender.getName()).color(getSenderGroup(sender).getColor());
    }

    @Getter
    public static class Group
    {
        private final String internalName;
        private final String name;
        private final Component displayName;
        private final TextColor color;

        public static Group fromConfigurationSection(String internalName, ConfigurationSection section)
        {
            String name = section.getString("name", internalName);
            TextColor color;
            if (section.isString("color"))
            {
                TextColor temp = NamedTextColor.NAMES.value(Objects.requireNonNull(section.getString("color")).toLowerCase());

                // UGLY HACK COMING RIGHT UP
                if (temp == null)
                {
                    try
                    {
                        color = TextColor.color(Color.decode(Objects.requireNonNull(section.getString("color"))).getRGB());
                    }
                    catch (NumberFormatException ex)
                    {
                        color = NamedTextColor.GRAY;
                    }
                }
                else
                {
                    color = temp;
                }
            }
            else
            {
                color = TextColor.color(Math.max(Math.min(section.getInt("color", NamedTextColor.GRAY.value()), 0xFFFFFF), 0));
            }

            Component displayName = section.getRichMessage("displayName", Component.text(name).color(color));

            return new Group(internalName, name, displayName, color);
        }

        public static Group createGroup(String internalName, String name, Component displayName, TextColor color)
        {
            return new Group(internalName, name, displayName, color);
        }

        @SuppressWarnings("deprecation")
        public ChatColor toChatColor()
        {
            if (color instanceof NamedTextColor named)
            {
                return ChatColor.valueOf(named.toString().toUpperCase(Locale.ROOT));
            }
            // fallback (no hex codes)
            return ChatColor.GRAY;
        }

        private Group(String internalName, String name, TextColor textColor)
        {
            this.internalName = internalName;
            this.name = name;
            this.displayName = Component.text(name).color(textColor);
            this.color = textColor;
        }

        private Group(String internalName, String name, Component displayName, TextColor color)
        {
            this.internalName = internalName;
            this.name = name;
            this.displayName = displayName;
            this.color = color;
        }
    }
}