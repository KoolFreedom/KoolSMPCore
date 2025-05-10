/*
/
/ Idea for the creation of this file comes from TotalFreedomMod and adjacent projects
/
/ We meant for this to be a 1:1 creation minus a few things,
/  but we wanted ot be as accurate as possible so that it functions the same way.
/
*/

package eu.koolfreedom.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter
public enum ConfigEntry
{
    SERVER_MOTD(String.class, "server.motd"),
    SERVER_TABLIST_HEADER(String.class, "server.tablist_header"),
    SERVER_TABLIST_FOOTER(String.class, "server.tablist_footer"),
    SERVER_WEBSITE_OR_FORUM(String.class, "server.website_or_forum"),
    ANNOUNCER_ENABLED(Boolean.class, "announcer.enable"),
    ANNOUNCER_DELAY(Integer.class, "announcer.delay"),
    ANNOUNCER_MESSAGES(List.class, "announcer.messages"),
    DISCORD_BOT_TOKEN(String.class, "discord.bot_token"),
    DISCORD_SERVER_ID(String.class, "discord.server_id"),
    DISCORD_REPORTS_CHANNEL_ID(String.class, "discord.reports.channel_id"),
    DISCORD_REPORTS_ROLES_THAT_CAN_CLOSE(List.class, "discord.reports.roles.close"),
    DISCORD_REPORTS_ROLES_THAT_CAN_PURGE(List.class, "discord.reports.roles.purge"),
    DISCORD_REPORTS_ROLES_THAT_CAN_REOPEN(List.class, "discord.reports.roles.reopen"),
    DISCORD_REPORTS_ROLES_THAT_CAN_RESOLVE(List.class, "discord.reports.roles.resolve"),
    CHAT_FILTER_ENABLED(Boolean.class, "chat-filter.enabled"),
    CHAT_FILTER_DELAY(Integer.class, "chat-filter.delay"),
    CHAT_FILTER_CATEGORIES(ConfigurationSection.class, "chat-filter.categories"),
    FORMATS_ADMIN_CHAT(String.class, "formats.admin_chat"),
    FORMATS_COMMANDSPY(String.class, "formats.commandspy"),
    FORMATS_REPORT(String.class, "formats.report"),
    FORMATS_REPORT_SUMMARY(String.class, "formats.report_summary"),
    FORMATS_REPORT_QUICK_SUMMARY(String.class, "formats.report_quick_summary"),
    FORMATS_REPORT_EMPTY_QUICK_SUMMARY(String.class, "formats.report_empty_quick_summary"),
    FORMATS_SAY(String.class, "formats.say"),
    GROUPS(ConfigurationSection.class, "groups");

    private final Class<?> type;
    private final String configName;

    ConfigEntry(Class<?> type, String configName) {
        this.type = type;
        this.configName = configName;
    }

	public String getString()
    {
        return MainConfig.getString(this);
    }

    public String setString(String value)
    {
        MainConfig.setString(this, value);
        return value;
    }

    public Double getDouble()
    {
        return MainConfig.getDouble(this);
    }

    public Double setDouble(Double value)
    {
        MainConfig.setDouble(this, value);
        return value;
    }

    public Boolean getBoolean()
    {
        return MainConfig.getBoolean(this);
    }

    public Boolean setBoolean(Boolean value)
    {
        MainConfig.setBoolean(this, value);
        return value;
    }

    public Integer getInteger()
    {
        return MainConfig.getInteger(this);
    }

    public Integer setInteger(Integer value)
    {
        MainConfig.setInteger(this, value);
        return value;
    }

    public Long getLong()
    {
        return MainConfig.getLong(this);
    }

    public Long setInteger(Long value)
    {
        MainConfig.setLong(this, value);
        return value;
    }

    public List<?> getList()
    {
        return MainConfig.getList(this);
    }

    public List<String> getStringList()
    {
        return (List<String>) MainConfig.getList(this);
    }

    public ConfigurationSection getConfigurationSection()
    {
        return MainConfig.getConfigurationSection(this);
    }

    public static ConfigEntry findConfigEntry(String name)
    {
        name = name.toLowerCase().replace("_", "");
        for (ConfigEntry entry : values())
        {
            if (entry.toString().toLowerCase().replace("_", "").equals(name))
            {
                return entry;
            }
        }
        return null;
    }
}

