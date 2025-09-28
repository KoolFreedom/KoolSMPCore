package eu.koolfreedom.chat;

import eu.koolfreedom.KoolSMPCore;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
            "obfuscated", "obf", "newline", "lang", "key", "translate",
            "black", "hover"
    );

    // Pattern to detect MiniMessage-style tags: <tag:...> or <tag>
    private static final Pattern TAG_PATTERN = Pattern.compile("<([a-zA-Z0-9_-]+)(:.*?)?>");

    // Legacy color serializer (handles § or & codes)
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&') // allow &a, &b, etc.
                    .hexCharacter('#') // allow hex like &#FFFFFF
                    .useUnusualXRepeatedCharacterHexFormat() // allow &x&f&f...
                    .build();

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        String rawMessage = FUtil.plainText(event.originalMessage());

        // First, sanitize blocked MiniMessage tags
        String safeMessage = sanitizeBlockedTags(rawMessage);

        Component parsed;
        try {
            // If the message contains legacy codes, parse them first
            if (safeMessage.contains("&") || safeMessage.contains("§")) {
                parsed = LEGACY_SERIALIZER.deserialize(safeMessage);
            } else {
                // Otherwise, try MiniMessage
                parsed = FUtil.miniMessage(safeMessage, TagResolver.empty());
            }
        }
        catch (Exception e) {
            // Fallback: plain message
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
