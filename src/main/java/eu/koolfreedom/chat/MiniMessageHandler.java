package eu.koolfreedom.chat;

import eu.koolfreedom.listener.KoolListener;
import eu.koolfreedom.util.FUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import java.util.Set;
import java.util.regex.Pattern;

public class MiniMessageHandler extends KoolListener
{
    // Set of disabled/ignored MiniMessage tags
    private static final Set<String> BLOCKED_TAGS = Set.of(
            "obfuscated", "obf", "newline", "lang", "key", "translate",
            "black", "hover"
    );

    // Set of disabled/ignored legacy formatting codes
    private static final Set<Character> IGNORED_LEGACY_FORMATS = Set.of(
            //
            '0', 'k', '7', '8', 'r' // blocking reset only because no one uses it
    );

    // Pattern to detect MiniMessage tags: <tag:...> or <tag>
    private static final Pattern TAG_PATTERN =
            Pattern.compile("<([a-zA-Z0-9_-]+)(:.*?)?>");

    // Pattern to detect legacy formatting codes (&a, §a, &l, etc.)
    private static final Pattern LEGACY_PATTERN =
            Pattern.compile("(?i)[&§]([0-9a-fk-or])");

    // Legacy color serializer
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexCharacter('#')
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        String rawMessage = FUtil.plainText(event.originalMessage());

        // Sanitize blocked MiniMessage tags
        String safeMessage = sanitizeBlockedTags(rawMessage);

        // Sanitize blocked legacy formats if no permission
        if (!player.hasPermission("venomcore.chat.legacy")) {
            safeMessage = sanitizeLegacyFormats(safeMessage);
        }

        if (!player.hasPermission("venomcore.chat.minimessage"))
        {
            event.message(Component.text(safeMessage));
            return;
        }

        Component parsed;
        try {
            if (safeMessage.contains("&") || safeMessage.contains("§")) {
                parsed = LEGACY_SERIALIZER.deserialize(safeMessage);
            } else {
                parsed = FUtil.miniMessage(safeMessage, TagResolver.empty());
            }
        }
        catch (Exception e) {
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

    /**
     * Removes disallowed legacy formatting codes.
     */
    private String sanitizeLegacyFormats(String message)
    {
        return LEGACY_PATTERN.matcher(message).replaceAll(match -> {
            char code = Character.toLowerCase(match.group(1).charAt(0));
            return IGNORED_LEGACY_FORMATS.contains(code) ? "" : match.group();
        });
    }
}