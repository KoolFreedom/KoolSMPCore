package eu.koolfreedom.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

@Getter @SuppressWarnings("unchecked")
public enum ConfigEntry
{
    SERVER_MOTD(String.class, "server.motd"),
    SERVER_WHITELIST_MOTD(String.class, "server.whitelist_motd"),
    SERVER_FULL_MOTD(String.class, "server.full_motd"),
    SERVER_TABLIST_HEADER(String.class, "server.tablist_header"),
    SERVER_TABLIST_FOOTER(String.class, "server.tablist_footer"),
    SERVER_APPEAL_URL(String.class, "server.ban_url"),
    ANNOUNCER_ENABLED(Boolean.class, "announcer.enable"),
    ANNOUNCER_DELAY(Integer.class, "announcer.delay"),
    ANNOUNCER_MESSAGES(List.class, "announcer.messages"),
    ADMININFO(List.class, "admininfo"),
    DISCORD_GROUP_MODE_SWITCH(Integer.class, "discord.group_role_mode"),
    DISCORD_USER_FORMAT(String.class, "discord.user_format"),
    DISCORD_REPLYING_TO_FORMAT(String.class, "discord.replying_to_format"),
    CHAT_FILTER_ENABLED(Boolean.class, "chat-filter.enabled"),
    CHAT_FILTER_CATEGORIES(ConfigurationSection.class, "chat-filter.categories"),
    FORMATS_ADMIN_CHAT(String.class, "formats.admin_chat"),
    FORMATS_COMMANDSPY(String.class, "formats.commandspy"),
    FORMATS_REPORT(String.class, "formats.report"),
    FORMATS_REPORT_SUMMARY(String.class, "formats.report_summary"),
    FORMATS_REPORT_QUICK_SUMMARY(String.class, "formats.report_quick_summary"),
    FORMATS_REPORT_EMPTY_QUICK_SUMMARY(String.class, "formats.report_empty_quick_summary"),
    FORMATS_SAY(String.class, "formats.say"),
    GROUPS(ConfigurationSection.class, "groups"),
    EXPLOITS_ENABLED(Boolean.class, "exploits.enabled"),
    EXPLOITS_CRASH(ConfigurationSection.class, "exploits.crash"),
    EXPLOIT_TAB_COMPLETE(ConfigurationSection.class, "exploits.crash.tab_complete"),
    EXPLOIT_TAB_COMPLETE_ENABLED(Boolean.class, "exploits.crash.tab_complete.enabled"),
    EXPLOIT_TAB_COMPLETE_PUNISH(String.class, "exploits.crash.tab_complete.punishment"),
    EXPLOIT_BUNDLE(ConfigurationSection.class, "exploits.crash.bundle"),
    EXPLOIT_BUNDLE_ENABLED(Boolean.class, "exploits.crash.bundle.enabled"),
    EXPLOIT_BUNDLE_PUNISH(String.class, "exploits.crash.bundle.punishment"),
    EXPLOITS_DUPE(ConfigurationSection.class, "exploits.dupe"),
    EXPLOIT_BOOK_AND_QUILL(ConfigurationSection.class, "exploits.dupe.book_and_quill"),
    EXPLOIT_BOOK_AND_QUILL_ENABLED(Boolean.class, "exploits.dupe.book_and_quill.enabled"),
    EXPLOIT_BOOK_AND_QUILL_PUNISH(String.class, "exploits.dupe.book_and_quill.punishment"),
    EXPLOIT_TRIDENT(ConfigurationSection.class, "exploits.dupe.trident"),
    EXPLOIT_TRIDENT_ENABLED(Boolean.class, "exploits.dupe.trident.enabled"),
    EXPLOIT_TRIDENT_PUNISH(String.class, "exploits.dupe.trident.punishment"),
    EXPLOITS_MISC(ConfigurationSection.class, "exploits.misc"),
    EXPLOIT_MISC_ABILITIES(ConfigurationSection.class, "exploits.misc.abilities"),
    EXPLOIT_MISC_ABILITIES_ENABLED(Boolean.class, "exploits.misc.abilities.enabled"),
    EXPLOIT_MISC_ABILITIES_PUNISH(String.class, "exploits.misc.abilities.punishment");





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
}

