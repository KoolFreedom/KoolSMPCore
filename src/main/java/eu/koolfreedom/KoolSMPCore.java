package eu.koolfreedom;

import eu.koolfreedom.api.Permissions;
import eu.koolfreedom.command.CommandLoader;
import eu.koolfreedom.config.Config;
import eu.koolfreedom.log.FLog;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.LuckPerms;
import org.bukkit.plugin.java.*;
import org.bukkit.plugin.*;
import net.kyori.adventure.text.*;
import eu.koolfreedom.command.impl.*;
import eu.koolfreedom.listener.*;
import com.earth2me.essentials.*;
import org.bukkit.event.*;
import java.io.InputStream;
import java.util.concurrent.*;
import net.kyori.adventure.text.serializer.legacy.*;
import org.bukkit.*;
import org.bukkit.command.*;
import java.util.*;

import org.bukkit.event.player.*;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class KoolSMPCore extends JavaPlugin implements Listener {
    public static KoolSMPCore main;
    public static Server server;
    public static final Random random = new Random();
    public static String pluginVersion;
    public static String pluginName;
    public static final BuildProperties build = new BuildProperties();
    private final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public Config config;
    public ServerListener sl;
    public Permissions perms;
    public ExploitListener el;
    public TabListener tl;
    public LoginListener lol; // lol

    public static KoolSMPCore getPlugin()
    {
        return main;
    }

    public Component mmDeserialize(String message)
    {
        return MINI_MESSAGE.deserialize(message).clickEvent(null).hoverEvent(null);
    }

    public static LuckPerms getLuckPermsAPI()
    {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null)
        {
            FLog.info("Successfully loaded the LuckPerms API.");
            return provider.getProvider();
        }
        FLog.severe("The LuckPerms API was not loaded successfully. The plugin will not function properly.");
        return null;
    }


    public void onLoad()
    {
        main = this;
        server = main.getServer();
        KoolSMPCore.pluginName = main.getDescription().getName();
        KoolSMPCore.pluginVersion = main.getDescription().getVersion();

        FLog.setPluginLogger(main.getLogger());
        FLog.setServerLogger(getServer().getLogger());

        build.load(main);
    }

    @Override
    public void onEnable()
    {
        FLog.info("Created by gamingto12 and 0x7694C9");
        FLog.info("Version " + build.version);
        FLog.info("Compiled " + build.date + " by " + build.author);
        server.getPluginManager().registerEvents(this, this);
        CommandLoader commandLoader = new CommandLoader(this);
        commandLoader.registerCommands("eu.koolfreedom.command.impl");
        loadListeners();
        perms = new Permissions();

        config = new Config("config.yml");

        if (getConfig().getBoolean("enable-announcer")) announcerRunnable();
    }

    @Override
    public void onDisable()
    {
        FLog.info("KoolSMPCore has been disabled");
        config.save();
    }

    public void loadListeners()
    {
        sl = new ServerListener(this);
        el = new ExploitListener(this);
        lol = new LoginListener(this);
        this.getServer().getPluginManager().registerEvents(new PunishmentListener(), this);
        tl = new TabListener(this);
    }

    public static class BuildProperties
    {
        public String author;
        public String version;
        public String number;
        public String date;

        void load(KoolSMPCore plugin)
        {
            try
            {
                final Properties props;

                try (InputStream in = plugin.getResource("build.properties"))
                {
                    props = new Properties();
                    props.load(in);
                }

                author = props.getProperty("buildAuthor", "gamingto12");
                version = props.getProperty("buildVersion", pluginVersion);
                number = props.getProperty("buildNumber", "1");
                date = props.getProperty("buildDate", "unknown");
            }
            catch (Exception ex)
            {
                FLog.severe("Could not load build properties! Did you compile with NetBeans/Maven?");
                FLog.severe(ex.toString());
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        return false;
    }

    public boolean isVanished(Player player) {
        Plugin essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");

        if (essentials == null) {
            return false;
        }

        return essentials.isEnabled() && ((Essentials) essentials).getUser(player).isVanished();
    }

    private void announcerRunnable() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            List<String> messageKeys = new ArrayList<>(getConfig().getConfigurationSection("messages").getKeys(false));
            if (messageKeys.isEmpty()) {
                getLogger().warning("No messages found in configuration.");
                return;
            }
            String randomKey = messageKeys.get(ThreadLocalRandom.current().nextInt(messageKeys.size()));
            List<String> lines = getConfig().getStringList("messages." + randomKey);
            if (lines.isEmpty()) {
                getLogger().warning("Message '" + randomKey + "' has no lines.");
                return;
            }
            Bukkit.broadcast(Component.newline().append(LegacyComponentSerializer.legacyAmpersand().deserialize(String.join("\n", lines))).appendNewline());
        }, 0L, getConfig().getLong("announcer-time"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        if (message.startsWith("/") && !player.isOp()) {
            event.setCancelled(true);
            return;
        }

        if (player.hasPermission("kf.admin")) {
            if (message.contains("@everyone")) {
                String lastColor = ChatColor.getLastColors(message);
                message = message.replace("@everyone", "§b@everyone" + (lastColor.isEmpty() ? "§r" : lastColor));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
                }

                event.setMessage(message);
                return;
            }
        } else {
            if (message.trim().toLowerCase().contains("nigger") ||
                    (message.trim().toLowerCase().contains("nigga") ||
                            (message.trim().toLowerCase().contains("faggot")))) {
                event.setCancelled(true);

                player.sendMessage(String.format(event.getFormat(), player.getDisplayName(), message));

                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), "obliterate " + player.getName());
                    } else {
                        getServer().dispatchCommand(Bukkit.getConsoleSender(), "banip " + player.getName() + " §c§lMay your worst nightmare come true, and may you suffer by the hands of your ruler, " + player.getName() + ". (Slurs)");
                    }
                }, 50L);

                return;
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isVanished(p)) continue;

            if (message.contains("@" + p.getName())) {
                String lastColor = ChatColor.getLastColors(message);
                message = message.replace("@" + p.getName(), "§b@" + p.getName() + (lastColor.isEmpty() ? "§r" : lastColor));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
            }
        }
    }
}
