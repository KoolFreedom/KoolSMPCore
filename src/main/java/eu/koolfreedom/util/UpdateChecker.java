package eu.koolfreedom.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import eu.koolfreedom.KoolSMPCore;

public record UpdateChecker(KoolSMPCore plugin, String repoOwner, String repoName,
                            @Nullable String spigotUrl, @Nullable String modrinthUrl) {

    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DOWNLOAD_URL_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

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

                String currentVersion = getCurrentVersion();
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
                    notify(sender, "<red>No JAR file found in the latest release.");
                    notify(sender, buildManualDownloadMessage());
                    return;
                }

                notify(sender, "<gray>Downloading <white>" + plugin.getName() + " " + release.tag() + "<gray>...");
                downloadUpdate(release.downloadUrl(), release.tag(), sender);

            } catch (Exception e) {
                FLog.error("Failed to check for updates: " + e.getMessage(), e);
                notify(sender, "<red>Failed to check for updates: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private String getCurrentVersion() {
        String buildVersion = plugin.getBuildMeta() != null ? plugin.getBuildMeta().getVersion() : null;
        if (isMeaningful(buildVersion)) {
            return buildVersion;
        }

        String metaVersion = plugin.getPluginMeta() != null ? plugin.getPluginMeta().getVersion() : null;
        return isMeaningful(metaVersion) ? metaVersion : "unknown";
    }

    private boolean isMeaningful(@Nullable String value) {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }

    private void downloadUpdate(String downloadUrl, String latestTag, @Nullable CommandSender sender) {
        try {
            Path pluginJar = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            Path pluginsFolder = pluginJar.getParent();
            String jarName = pluginJar.getFileName().toString();
            Path newJar = pluginsFolder.resolve(plugin.getName() + "-" + latestTag + ".jar");
            Path oldJar = pluginsFolder.resolve(jarName.replaceFirst("\\.jar$", ".jar.old"));

            HttpURLConnection connection = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, newJar, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(pluginJar, oldJar, StandardCopyOption.REPLACE_EXISTING);
            Files.move(newJar, pluginsFolder.resolve(plugin.getName() + "-" + latestTag + ".jar"), StandardCopyOption.REPLACE_EXISTING);

            notify(sender, "<green>Update downloaded successfully!");
            notify(sender, "<gray>The update will take effect after the next server restart.");
            notify(sender, "<gray>Old JAR backed up as: <white>" + oldJar.getFileName());
            FLog.info(String.format("Update to %s downloaded. Restart the server to apply.", latestTag));

        } catch (Exception e) {
            FLog.error("Failed to download update: " + e.getMessage(), e);
            notify(sender, "<red>Failed to download update: " + e.getMessage());
        }
    }

    private ReleaseInfo fetchLatestRelease() throws Exception {
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName);
        HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            String error = connection.getResponseMessage();
            throw new IllegalStateException("GitHub API responded with HTTP " + responseCode + ": " + error);
        }

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

    static boolean isOutdated(String current, String latest) {
        return !VersionUtil.normalizeVersion(latest).equalsIgnoreCase(VersionUtil.normalizeVersion(current));
    }

    private void notifyUpdateAvailable(@Nullable CommandSender sender, String current, String latest) {
        String githubUrl = String.format("https://github.com/%s/%s/releases/latest", repoOwner, repoName);
        String[] lines = {
                "<yellow>------------------------------------------------",
                String.format("<yellow>An update is available for <white>%s<yellow>!", plugin.getName()),
                String.format("<gray>Current version: <white>%s", current),
                String.format("<gray>Latest version:  <white>%s", latest),
                "<gray>Use `/koolsmpcore update` to install it automatically.",
                "<gray>If that didn't work, then you can download the plugin here and update manually:",
                String.format("<gray>• GitHub: <white>%s", githubUrl),
                spigotUrl != null && !spigotUrl.isEmpty() ? "<gray>• SpigotMC: <white>" + spigotUrl : null,
                modrinthUrl != null && !modrinthUrl.isEmpty() ? "<gray>• Modrinth: <white>" + modrinthUrl : null,
                "<yellow>------------------------------------------------"
        };

        for (String line : lines) {
            if (line != null) notify(sender, line);
        }
    }

    private String buildManualDownloadMessage() {
        String githubUrl = String.format("https://github.com/%s/%s/releases/latest", repoOwner, repoName);
        StringBuilder builder = new StringBuilder("<gray>If that didn't work, then you can download the plugin here and update manually:\n");
        builder.append(String.format("<gray>• GitHub: <white>%s\n", githubUrl));
        if (spigotUrl != null && !spigotUrl.isEmpty()) {
            builder.append(String.format("<gray>• SpigotMC: <white>%s\n", spigotUrl));
        }
        if (modrinthUrl != null && !modrinthUrl.isEmpty()) {
            builder.append(String.format("<gray>• Modrinth: <white>%s", modrinthUrl));
        }
        return builder.toString();
    }

    private void notify(@Nullable CommandSender sender, String miniMessage) {
        if (sender != null) {
            sender.sendMessage(FUtil.miniMessage(miniMessage));
        } else {
            FLog.info(miniMessage.replaceAll("<[^>]+>", ""));
        }
    }

    private String extractTagName(String json) {
        Matcher matcher = TAG_NAME_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractDownloadUrl(String json) {
        Matcher matcher = DOWNLOAD_URL_PATTERN.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record ReleaseInfo(String tag, @Nullable String downloadUrl) {}
}