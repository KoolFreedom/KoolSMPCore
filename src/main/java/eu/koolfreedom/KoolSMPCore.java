package eu.koolfreedom;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import eu.koolfreedom.api.AltManager;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.bridge.DiscordIntegration;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.bridge.LuckPermsBridge;
import eu.koolfreedom.bridge.VanishIntegration;
import eu.koolfreedom.bridge.discord.DiscordSRVIntegration;
import eu.koolfreedom.bridge.discord.EssentialsXDiscordIntegration;
import eu.koolfreedom.bridge.vanish.EssentialsVanishIntegration;
import eu.koolfreedom.bridge.vanish.SuperVanishIntegration;
import eu.koolfreedom.chat.AntiSpamService;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.command.impl.AdminChatCommand;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.config.MainConfig;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.listener.impl.*;
import eu.koolfreedom.note.NoteManager;
import eu.koolfreedom.player.PlayerRegistry;
import eu.koolfreedom.punishment.RecordKeeper;
import eu.koolfreedom.reporting.ReportManager;
import eu.koolfreedom.util.*;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

@Getter
public class KoolSMPCore extends JavaPlugin
{
    @Getter
    private static KoolSMPCore instance;

    private BuildProperties buildMeta;
    private CommandLoader commandLoader;

    // Storage — must be first, everything else depends on it
    private PlayerRegistry playerRegistry;

    // Managers
    private BanManager banManager;
    private NoteManager noteManager;
    private AltManager altManager;
    private MuteManager muteManager;
    private RecordKeeper recordKeeper;
    private ReportManager reportManager;
    private LockupManager lockupManager;
    private FreezeManager freezeManager;
    private FreezeListener freezeListener;
    private AntiSpamService antiSpamListener;
    private AutoUndoManager autoUndoManager;
    private CosmeticManager cosmeticManager;
    private ExploitListener exploitListener;
    private ChatListener chatListener;
    private PlayerJoinListener pjListener;

    // Bridges
    private GroupManagement groupManager;
    private LuckPermsBridge luckPermsBridge;
    private DiscordIntegration<?> discordBridge;
    private VanishIntegration<?> vanishBridge;

    private BukkitTask announcer = null;
    private UpdateChecker updateChecker;

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

        playerRegistry = new PlayerRegistry();

        noteManager = new NoteManager(playerRegistry);
        altManager = new AltManager(playerRegistry);
        freezeManager = new FreezeManager(playerRegistry);

        MainConfig.load();
        FLog.info("Loaded main configuration");

        updateChecker = new UpdateChecker(
                this, "KoolFreedom", "KoolSMPCore",
                "https://www.spigotmc.org/resources/koolsmpcore.126127/",
                "https://modrinth.com/plugin/koolsmpcore");
        updateChecker.check();

        try
        {
            new Metrics(this, 26369);
            FLog.info("Enabled Metrics");
        }
        catch (Exception e)
        {
            FLog.error("Could not start Metrics", e);
        }

        loadManagers();
        FLog.info("Loaded managers");

        loadListeners();
        FLog.info("Loaded listeners");

        commandLoader = new CommandLoader(AdminChatCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded {} commands", commandLoader.getKoolCommands().size());

        groupManager = new GroupManagement();
        FLog.info("Loaded group manager");

        resetAnnouncer();
        loadBridges();
        FLog.info("Bridges built");

        if (Bukkit.getPluginManager().isPluginEnabled("packetevents"))
        {
            FLog.info("PacketEvents found, enabling exploit patches.");
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
            PacketEvents.getAPI().init();
            exploitListener = new ExploitListener();
            PacketEvents.getAPI().getEventManager().registerListener(exploitListener, PacketListenerPriority.HIGHEST);
        }
        else
        {
            FLog.warning("PacketEvents not found! Exploit patches will not be able to function!");
        }
    }

    @Override
    public void onDisable()
    {
        if (Bukkit.getPluginManager().isPluginEnabled("packetevents"))
            PacketEvents.getAPI().terminate();

        playerRegistry.close();
        FLog.info("KoolSMPCore has been disabled");
    }

    public void loadManagers()
    {
        banManager = new BanManager();
        banManager.load();
        recordKeeper = new RecordKeeper();
        reportManager = new ReportManager();
    }

    public void loadListeners()
    {
        muteManager = new MuteManager();
        cosmeticManager = new CosmeticManager();
        chatListener = new ChatListener();
        freezeListener = new FreezeListener();
        lockupManager = new LockupManager();
        pjListener = new PlayerJoinListener();
        antiSpamListener = new AntiSpamService();
        autoUndoManager = new AutoUndoManager(this, muteManager, freezeManager);
    }

    public void loadBridges()
    {
        PluginManager pluginManager = Bukkit.getPluginManager();

        // Discord
        if (pluginManager.isPluginEnabled("DiscordSRV"))
        {
            FLog.info("Using DiscordSRV bridge.");
            discordBridge = new DiscordSRVIntegration().register();
        }
        else if (pluginManager.isPluginEnabled("EssentialsDiscord") && pluginManager.isPluginEnabled("EssentialsDiscordLink"))
        {
            FLog.info("Using EssentialsXDiscord bridge.");
            discordBridge = new EssentialsXDiscordIntegration().register();
        }
        else
        {
            discordBridge = Bukkit.getServicesManager().load(DiscordIntegration.class);

            if (discordBridge != null)
            {
                FLog.info("Using external Discord integrator {}", discordBridge.getClass().getName());
            }
        }

        // Vanish plugins
        if (pluginManager.isPluginEnabled("Essentials"))
        {
            vanishBridge = new EssentialsVanishIntegration();
        }
        else if (pluginManager.isPluginEnabled("SuperVanish"))
        {
            vanishBridge = new SuperVanishIntegration();
        }

        // LuckPerms
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms"))
        {
            luckPermsBridge = new LuckPermsBridge();
        }
    }

    public void resetAnnouncer()
    {
        if (announcer != null)
        {
            announcer.cancel();
        }

        if (ConfigEntry.ANNOUNCER_ENABLED.getBoolean())
        {
            announcer = new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    List<String> messages = ConfigEntry.ANNOUNCER_MESSAGES.getStringList();

                    // Messages aren't configured, so we're not going to bother
                    if (messages.isEmpty())
                    {
                        return;
                    }

                    FUtil.broadcast(false, messages.get(FUtil.randomNumber(0, messages.size())));
                }
            }.runTaskTimer(this, 0, Math.max(1, ConfigEntry.ANNOUNCER_DELAY.getInteger()));
        }
    }
}
