package eu.koolfreedom;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import eu.koolfreedom.api.AltListener;
import eu.koolfreedom.api.AltManager;
import eu.koolfreedom.bridge.GroupManagement;
import eu.koolfreedom.banning.BanManager;
import eu.koolfreedom.bridge.LuckPermsBridge;
import eu.koolfreedom.bridge.VanishIntegration;
import eu.koolfreedom.bridge.vanish.EssentialsVanishIntegration;
import eu.koolfreedom.bridge.vanish.SuperVanishIntegration;
import eu.koolfreedom.chat.AntiSpamService;
//import eu.koolfreedom.combat.CombatManager;
import eu.koolfreedom.chat.MiniMessageHandler;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.config.ConfigEntry;
import eu.koolfreedom.bridge.discord.DiscordSRVIntegration;
import eu.koolfreedom.bridge.DiscordIntegration;
import eu.koolfreedom.bridge.discord.EssentialsXDiscordIntegration;
import eu.koolfreedom.freeze.FreezeListener;
import eu.koolfreedom.freeze.FreezeManager;
import eu.koolfreedom.note.NoteManager;
import eu.koolfreedom.stats.PlaytimeListener;
import eu.koolfreedom.stats.PlaytimeManager;
import eu.koolfreedom.util.*;
import eu.koolfreedom.punishment.RecordKeeper;
import eu.koolfreedom.reporting.ReportManager;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.*;
import org.bukkit.plugin.*;
import eu.koolfreedom.command.impl.*;
import eu.koolfreedom.listener.*;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@Getter
public class KoolSMPCore extends JavaPlugin
{
    @Getter
    private static KoolSMPCore instance;

    private BuildProperties buildMeta;

    private CommandLoader commandLoader;

    private BanManager banManager;
    private MuteManager muteManager;
    private RecordKeeper recordKeeper;
    private ReportManager reportManager;
    @Getter
    private LockupManager lockupManager;
    private FreezeManager freezeManager;
    @Getter
    private NoteManager noteManager;
    @Getter
    private AltManager altManager;
    @Getter
    private PlaytimeManager playtimeManager;
    private FreezeListener freezeListener;
    private AntiSpamService antiSpamListener;
    @Getter
    private AutoUndoManager autoUndoManager;

    private CosmeticManager cosmeticManager;
    private ExploitListener exploitListener;
    private ChatListener chatListener;
    private PlayerJoinListener pjListener;
    private AltListener altListener;
    private PlaytimeListener ptListener;
    private MiniMessageHandler mmHandler;

    private GroupManagement groupManager;
    private LuckPermsBridge luckPermsBridge;
    private DiscordIntegration<?> discordBridge;
    private VanishIntegration<?> vanishBridge;

    private BukkitTask announcer = null;

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

        new UpdateChecker(this, "KoolFreedom", "KoolSMPCore").check();

        int pluginId = 26369;
        Metrics metrics = new Metrics(this, pluginId);
        FLog.info("Enabled Metrics");

        altManager = new AltManager();
        playtimeManager = new PlaytimeManager();
        playtimeManager.load();
        noteManager = new NoteManager();
        freezeManager = new FreezeManager();

        loadBansConfig();
        FLog.info("Loaded configurations");

        commandLoader = new CommandLoader(AdminChatCommand.class);
        commandLoader.loadCommands();
        FLog.info("Loaded {} commands", commandLoader.getKoolCommands().size());

        loadListeners();
        FLog.info("Loaded listeners");

        groupManager = new GroupManagement();
        FLog.info("Loaded group manager");

        // Set up announcer
        resetAnnouncer();

        loadBridges();
        FLog.info("Bridges built");

        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            FLog.info("PacketEvents found, enabling exploit patches.");
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
            PacketEvents.getAPI().init();
            exploitListener = new ExploitListener();
            PacketEvents.getAPI().getEventManager().registerListener(exploitListener, PacketListenerPriority.HIGHEST);

        } else {
            FLog.warning("PacketEvents not found! Exploit patches will not be able to function!");
        }
    }

    @Override
    public void onDisable()
    {
        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) {
            PacketEvents.getAPI().terminate();
        }
        FLog.info("KoolSMPCore has been disabled");

        banManager.save();
        reportManager.save();
        playtimeManager.save();
    }

    public void loadBansConfig()
    {
        banManager = new BanManager();
        banManager.load();
        recordKeeper = new RecordKeeper();
    }

    public void loadListeners()
    {
        muteManager = new MuteManager(this);
        reportManager = new ReportManager();
        cosmeticManager = new CosmeticManager();
        chatListener = new ChatListener();
        freezeListener = new FreezeListener();
        lockupManager = new LockupManager(this);
        pjListener = new PlayerJoinListener();
        altListener = new AltListener();
        ptListener = new PlaytimeListener();
        antiSpamListener = new AntiSpamService(this);
        mmHandler = new MiniMessageHandler();
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
