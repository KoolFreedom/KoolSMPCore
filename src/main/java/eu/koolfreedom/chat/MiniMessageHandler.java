package eu.koolfreedom.chat;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.regex.Pattern;

public class MiniMessageHandler implements Listener
{
    public MiniMessageHandler()
    {
        Bukkit.getServer().getPluginManager().registerEvents(this, KoolSMPCore.getInstance());
    }
    // Set of disallowed MiniMessage tags
    private static final Set<String> BLOCKED_TAGS = Set.of(
            "click", "hover", "insertion", "keybind", "translate", "obfuscated"
    );

    // Pattern to detect MiniMessage-style tags: <tag:...> or <tag>
    private static final Pattern TAG_PATTERN = Pattern.compile("<([a-zA-Z0-9_-]+)(:.*?)?>");

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        String rawMessage = FUtil.plainText(event.originalMessage());

        // Optionally filter disallowed tags before parsing
        String safeMessage = sanitizeBlockedTags(rawMessage);

        // Try parsing the safe message with MiniMessage
        Component parsed;
        try {
            parsed = FUtil.miniMessage(safeMessage, TagResolver.empty());
        }
        catch (Exception e) {
            // Fallback: plain message if syntax breaks
            parsed = Component.text(rawMessage);
        }

        event.message(parsed);
    }

    /**
     * Removes disallowed MiniMessage tags.
     */
    private String sanitizeBlockedTags(String message)
    {
        return TAG_PATTERN.matcher(message).replaceAll(match -> {
            String tag = match.group(1).toLowerCase();
            return BLOCKED_TAGS.contains(tag) ? "" : match.group();
        });
    }
}
