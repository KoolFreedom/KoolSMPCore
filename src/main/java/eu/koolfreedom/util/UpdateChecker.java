package eu.koolfreedom.util;

import eu.koolfreedom.KoolSMPCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record UpdateChecker(KoolSMPCore plugin, String repoOwner, String repoName,
                            @Nullable String spigotUrl, @Nullable String modrinthUrl) {

    public void check() {
        check(null, false);
    }

    public void check(@Nullable CommandSender sender) {
        check(sender, false);
    }

    public void checkAndUpdate(@Nullable CommandSender sender) {
        check(sender, true);
    }

    private void check(@Nullable CommandSender sender, boolean download) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ReleaseInfo release = fetchLatestRelease();
                if (release == null) {
                    notify(sender, "<red>Could not fetch the latest version from GitHub.");
                    return;
                }

                String currentVersion = plugin.getPluginMeta().getVersion();
                if (!isOutdated(currentVersion, release.tag())) {
                    notify(sender, String.format("<green>You are already running the latest version of <white>%s <green>(<white>%s<green>).",
                            plugin.getName(), currentVersion));
                    return;
                }

                if (!download) {
                    notifyUpdateAvailable(sender, currentVersion, release.tag());
                    return;
                }

                if (release.downloadUrl() == null) {
                    notify(sender, "<red>No JAR file found in the latest release. Download manually:");
                    notify(sender, String.format("<gray>• GitHub: <white>https://github.com/%s/%s/releases/latest", repoOwner, repoName));
                    return;
                }

                notify(sender, "<gray>Downloading <white>" + plugin.getName() + " " + release.tag() + "<gray>...");
                downloadUpdate(release.downloadUrl(), release.tag(), sender);

            } catch (Exception e) {
                FLog.error("Failed to check for updates: " + e.getMessage());
                notify(sender, "<red>Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private void downloadUpdate(String downloadUrl, String latestTag, @Nullable CommandSender sender) {
        try {
            // Find the current plugin JAR
            Path pluginJar = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            Path pluginsFolder = pluginJar.getParent();
            Path newJar = pluginsFolder.resolve(plugin.getName() + "-" + latestTag + ".jar");
            Path oldJar = pluginsFolder.resolve(pluginJar.getFileName() + ".old");

            // Download the new JAR
            HttpURLConnection connection = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, newJar, StandardCopyOption.REPLACE_EXISTING);
            }

            // Rename old JAR and put new one in place
            Files.move(pluginJar, oldJar, StandardCopyOption.REPLACE_EXISTING);
            Files.move(newJar, pluginJar, StandardCopyOption.REPLACE_EXISTING);

            notify(sender, "<green>Update downloaded successfully!");
            notify(sender, "<gray>The update will take effect after the next server restart.");
            notify(sender, "<gray>Old JAR backed up as: <white>" + oldJar.getFileName());
            FLog.info(String.format("Update to %s downloaded. Restart the server to apply.", latestTag));

        } catch (Exception e) {
            FLog.error("Failed to download update: " + e.getMessage());
            notify(sender, "<red>Failed to download update: " + e.getMessage());
        }
    }

    private ReleaseInfo fetchLatestRelease() throws Exception {
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName);
        HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            String json = response.toString();
            String tag = extractTagName(json);
            String downloadUrl = extractDownloadUrl(json);
            return tag != null ? new ReleaseInfo(tag, downloadUrl) : null;
        }
    }

    private boolean isOutdated(String current, String latest) {
        return !normalize(latest).equalsIgnoreCase(normalize(current));
    }

    private void notifyUpdateAvailable(@Nullable CommandSender sender, String current, String latest) {
        String[] lines = {
                "<yellow>------------------------------------------------",
                String.format("<yellow>An update is available for <white>%s<yellow>!", plugin.getName()),
                String.format("<gray>Current version: <white>%s", current),
                String.format("<gray>Latest version:  <white>%s", latest),
                "<gray>Download links:",
                String.format("<gray>• GitHub:   <white>https://github.com/%s/%s/releases/latest", repoOwner, repoName),
                spigotUrl != null && !spigotUrl.isEmpty() ? "<gray>• SpigotMC: <white>" + spigotUrl : null,
                modrinthUrl != null && !modrinthUrl.isEmpty() ? "<gray>• Modrinth: <white>" + modrinthUrl : null,
                "<gray>Alternatively, you can run `/koolsmpcore update` to automatically install the update",
                "<yellow>------------------------------------------------"
        };

        for (String line : lines) {
            if (line != null) notify(sender, line);
        }
    }

    private void notify(@Nullable CommandSender sender, String miniMessage) {
        if (sender != null) {
            sender.sendMessage(FUtil.miniMessage(miniMessage));
        } else {
            FLog.info(miniMessage.replaceAll("<[^>]+>", ""));
        }
    }

    private String normalize(String version) {
        return version.replaceFirst("(?i)^v", "").trim();
    }

    private String extractTagName(String json) {
        try {
            return json.split("\"tag_name\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDownloadUrl(String json) {
        try {
            // Find the first .jar asset's browser_download_url
            String[] parts = json.split("\"browser_download_url\":\"");
            for (int i = 1; i < parts.length; i++) {
                String url = parts[i].split("\"")[0];
                if (url.endsWith(".jar")) return url;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private record ReleaseInfo(String tag, @Nullable String downloadUrl) {}
}