package eu.koolfreedom.config;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.log.FLog;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config extends YamlConfiguration
{
    private KoolSMPCore plugin;
    private File file;
    private String name;
    private boolean added = false;

    public Config (KoolSMPCore plugin, String name)
    {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), name);
        this.name = name;

        if (!file.exists())
        {
            saveDefault();
        }
    }

    public void load()
    {
        this.load(true);
    }

    public void load(boolean loadFromFile)
    {
        try
        {
            if (loadFromFile)
            {
                YamlConfiguration externalYamlConfig = YamlConfiguration.loadConfiguration(file);
                InputStreamReader internalConfigFileStream = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8);
                YamlConfiguration internalYamlConfig = YamlConfiguration.loadConfiguration(internalConfigFileStream);

                // Gets all the keys inside the internal file and iterates through all of it's key pairs
                for (String string : internalYamlConfig.getKeys(true))
                {
                    // Checks if the external file contains the key already.
                    if (!externalYamlConfig.contains(string))
                    {
                        // If it doesn't contain the key, we set the key based off what was found inside the plugin jar
                        externalYamlConfig.setComments(string, internalYamlConfig.getComments(string));
                        externalYamlConfig.setInlineComments(string, internalYamlConfig.getInlineComments(string));
                        externalYamlConfig.set(string, internalYamlConfig.get(string));
                        FLog.info("Setting key: " + string + " in " + this.name + " to the default value(s) since it does not exist!");
                        added = true;
                    }
                }
                if (added)
                {
                    externalYamlConfig.save(file);
                    FLog.info("Saving new file...");
                    added = false;
                }
            }
            super.load(file);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void save()
    {
        try
        {
            super.save(file);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void saveDefault()
    {
        plugin.saveResource(name, false);
    }
}
