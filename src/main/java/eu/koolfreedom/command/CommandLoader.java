package eu.koolfreedom.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CommandLoader {
    private final JavaPlugin plugin;
    private CommandMap commandMap;

    public CommandLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        setupCommandMap();
    }

    private void setupCommandMap() {
        try {
            var commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterExistingCommand(String commandName, CommandMap commandMap) {
        // Remove vanilla and Bukkit commands
        String vanillaCommand = "minecraft:" + commandName;

        Command command = commandMap.getCommand(commandName);
        if (command != null) {
            command.unregister(commandMap);
            Bukkit.getLogger().info("Unregistered Bukkit command: /" + commandName);
        }

        Command vanilla = commandMap.getCommand(vanillaCommand);
        if (vanilla != null) {
            vanilla.unregister(commandMap);
            Bukkit.getLogger().info("Unregistered Vanilla command: /" + vanillaCommand);
        }

        // Remove Essentials' commands (or other plugin commands)
        String essentialsCommand = "essentials:" + commandName;
        Command essentials = commandMap.getCommand(essentialsCommand);
        if (essentials != null) {
            essentials.unregister(commandMap);
            Bukkit.getLogger().info("Unregistered Essentials command: /" + essentialsCommand);
        }
    }


    private void unregisterVanillaCommand(String commandName, CommandMap commandMap) {
        // Remove both Bukkit and Vanilla commands
        String vanillaCommand = "minecraft:" + commandName;

        Command command = commandMap.getCommand(commandName);
        if (command != null) {
            command.unregister(commandMap);
            Bukkit.getLogger().info("Unregistered Bukkit command: /" + commandName);
        }

        Command vanilla = commandMap.getCommand(vanillaCommand);
        if (vanilla != null) {
            vanilla.unregister(commandMap);
            Bukkit.getLogger().info("Unregistered Vanilla command: /" + vanillaCommand);
        }
    }

    public void registerCommands(String packageName) {
        try {
            // Access Bukkit's internal CommandMap
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // Find all command classes
            Set<Class<?>> commandClasses = findCommandClasses(packageName);
            for (Class<?> clazz : commandClasses) {
                if (FreedomCommand.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                    FreedomCommand command = (FreedomCommand) clazz.getDeclaredConstructor().newInstance();
                    String commandName = command.getClass().getSimpleName().toLowerCase().replace("command", "");

                    // Unregister vanilla and plugin (Essentials) commands
                    unregisterExistingCommand(commandName, commandMap);

                    // Register your command
                    PluginCommand pluginCommand = Bukkit.getPluginCommand(commandName);
                    if (pluginCommand == null) {
                        PluginCommandWrapper newCommand = new PluginCommandWrapper(commandName, command);
                        commandMap.register(plugin.getDescription().getName(), newCommand);
                        Bukkit.getLogger().info("Registered new command: /" + commandName);
                    } else {
                        pluginCommand.setExecutor(command);
                        Bukkit.getLogger().info("Replaced existing command: /" + commandName);
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to register commands!");
            e.printStackTrace();
        }
    }


    private Set<Class<?>> findCommandClasses(String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        String path = packageName.replace('.', '/');

        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("jar")) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    try (JarFile jarFile = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();
                            if (entryName.startsWith(path) && entryName.endsWith(".class")) {
                                String className = entryName.replace('/', '.').replace(".class", "");
                                Class<?> clazz = Class.forName(className);
                                if (FreedomCommand.class.isAssignableFrom(clazz)) {
                                    classes.add(clazz);
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return classes;
    }
}
