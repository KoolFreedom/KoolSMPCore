package eu.koolfreedom;

import eu.koolfreedom.api.GroupCosmetics;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.discord.Discord;
import eu.koolfreedom.log.FLog;
import eu.koolfreedom.punishment.RecordKeeper;
import eu.koolfreedom.reporting.ReportManager;
import eu.koolfreedom.util.BuildProperties;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import org.bukkit.plugin.java.*;
import org.bukkit.plugin.*;
import eu.koolfreedom.command.impl.*;
import eu.koolfreedom.listener.*;
import com.earth2me.essentials.*;
import org.bukkit.event.*;
import org.bukkit.*;
import java.util.*;

import org.bukkit.entity.*;

public class KoolSMPCore extends JavaPlugin implements Listener
{
    @Getter
    private static KoolSMPCore instance;

    public BuildProperties buildMeta;

    public CommandLoader commandLoader;

    public BanManager banManager;
    public MuteManager muteManager;
    public RecordKeeper recordKeeper;
    public ReportManager reportManager;
    public Discord discord;

    public LoginListener loginListener;
    public ServerListener serverListener;
    public GroupCosmetics groupCosmetics;
    public ExploitListener exploitListener;
    public LuckPermsListener luckPermsListener;
    public ChatFilter chatFilter;

    @Override
    public void onLoad()
    {
        instance = this;
        buildMeta = new BuildProperties();
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by gamingto12 and 0x7694C9");
        FLog.info("Version {}.{}", buildMeta.getVersion(), buildMeta.getNumber());
        FLog.info("Compiled {} by {}", buildMeta.getDate(), buildMeta.getAuthor());
        Bukkit.getPluginManager().registerEvents(this, this);

        commandLoader = new CommandLoader(AdminChatCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded commands");

        loadListeners();
        FLog.info("Loaded listeners");
        groupCosmetics = new GroupCosmetics();
        loadBansConfig();
        FLog.info("Loaded configurations");

        if (ConfigEntry.ANNOUNCER_ENABLED.getBoolean())
        {
            announcerRunnable();
        }

        discord = new Discord();
    }

    @Override
    public void onDisable()
    {
        FLog.info("KoolSMPCore has been disabled");

        discord.shutdown();
        banManager.save();
        reportManager.save();
    }

    public void loadBansConfig()
    {
        banManager = new BanManager();
        banManager.load();
        recordKeeper = new RecordKeeper();
    }

    public void loadListeners()
    {
        muteManager = new MuteManager();
        reportManager = new ReportManager();
        serverListener = new ServerListener();
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) exploitListener = new ExploitListener();
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) luckPermsListener = new LuckPermsListener();
        loginListener = new LoginListener();
        chatFilter = new ChatFilter();
    }

    public boolean isVanished(Player player)
    {
        Plugin essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");

        if (essentials == null) {
            return false;
        }

        return essentials.isEnabled() && ((Essentials) essentials).getUser(player).isVanished();
    }

    private void announcerRunnable()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () ->
        {
            List<String> messages = ConfigEntry.ANNOUNCER_MESSAGES.getStringList();

            // Messages aren't configured, so we're not going to bother
            if (messages.isEmpty())
            {
                return;
            }

            FUtil.broadcast(messages.get(FUtil.randomNumber(0, messages.size())));
        }, 0L, ConfigEntry.ANNOUNCER_DELAY.getInteger());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onChat(AsyncChatEvent event)
    {
        final Player player = event.getPlayer();
        final String message = event.signedMessage().message();

        // In-game pinging
        if (message.contains("@"))
        {
            Arrays.stream(message.split(" ")).filter(word -> word.startsWith("@")).filter(word ->
                    !word.equalsIgnoreCase("@everyone") || player.hasPermission("kfc.ping_everyone"))
                    .map(ping -> ping.replaceAll("@", "")).map(Bukkit::getPlayer)
                    .filter(Objects::nonNull).distinct().forEach(target -> target.playSound(target.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F));
        }
    }
}
