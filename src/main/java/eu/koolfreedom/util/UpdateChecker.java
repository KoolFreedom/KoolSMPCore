package eu.koolfreedom.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import eu.koolfreedom.KoolSMPCore;

/**
 * Checks GitHub Releases for a newer version and can stage it in Bukkit's update folder.
 * <p>
 * The version is taken from the release tag (never from the jar filename), and the installed
 * jar is replaced on next restart by dropping the new file into {@code plugins/update/} under
 * the same filename as the running jar.
 */
public record UpdateChecker(KoolSMPCore plugin, String repoOwner, String repoName,
                            @Nullable String spigotUrl, @Nullable String modrinthUrl)
{
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final String DIVIDER = "<yellow>------------------------------------------------";

    public void check()
    {
        run(null, false);
    }

    public void check(@Nullable CommandSender sender)
    {
        run(sender, false);
    }

    public void checkAndUpdate(@Nullable CommandSender sender)
    {
        run(sender, true);
    }

    private void run(@Nullable CommandSender sender, boolean install)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                Release latest = fetchLatest();
                String current = currentVersion();

                if (!isOutdated(current, latest.tag()))
                {
                    notify(sender, "<green>You are already running the latest version of <white>"
                            + plugin.getName() + " <green>(<white>" + current + "<green>).");
                    return;
                }

                if (!install)
                {
                    announce(sender, current, latest.tag());
                    return;
                }

                if (latest.downloadUrl() == null)
                {
                    notify(sender, "<red>The latest release has no JAR attached.");
                    notify(sender, manualDownloadMessage());
                    return;
                }

                notify(sender, "<gray>Downloading <white>" + plugin.getName() + " " + latest.tag() + "<gray>...");
                stage(sender, latest);
            }
            catch (Exception e)
            {
                FLog.error("Update check failed: " + e.getMessage(), e);
                notify(sender, "<red>Update check failed: " + e.getMessage());
            }
        });
    }

    private Release fetchLatest() throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest"))
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("GitHub API responded with HTTP " + response.statusCode());

        JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
        if (!release.has("tag_name"))
            throw new IOException("GitHub response had no tag_name");

        String tag = release.get("tag_name").getAsString();
        String preferred = plugin.getName() + ".jar";

        String downloadUrl = null;
        String sha256 = null;

        if (release.has("assets"))
        {
            for (JsonElement element : release.getAsJsonArray("assets"))
            {
                JsonObject asset = element.getAsJsonObject();
                String name = asset.get("name").getAsString();
                if (!name.endsWith(".jar")) continue;

                boolean exact = name.equalsIgnoreCase(preferred);
                if (downloadUrl == null || exact)
                {
                    downloadUrl = asset.get("browser_download_url").getAsString();
                    sha256 = parseDigest(asset);
                }
                if (exact) break;
            }
        }

        return new Release(tag, downloadUrl, sha256);
    }

    /** GitHub exposes asset digests as "sha256:<hex>"; older releases may not have one. */
    private static @Nullable String parseDigest(JsonObject asset)
    {
        if (!asset.has("digest") || asset.get("digest").isJsonNull()) return null;
        String digest = asset.get("digest").getAsString();
        return digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : null;
    }

    /**
     * Downloads the release jar into plugins/update/ using the running jar's filename, which
     * is what makes Bukkit swap it in on the next restart.
     */
    private void stage(@Nullable CommandSender sender, Release release)
    {
        try
        {
            Path runningJar = Path.of(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            Path updateFolder = Bukkit.getUpdateFolderFile().toPath();
            Files.createDirectories(updateFolder);

            Path target = updateFolder.resolve(runningJar.getFileName());
            Path partial = updateFolder.resolve(runningJar.getFileName() + ".part");

            HttpRequest request = HttpRequest.newBuilder(URI.create(release.downloadUrl()))
                    .header("Accept", "application/octet-stream")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();

            HttpResponse<Path> response = HTTP.send(request, HttpResponse.BodyHandlers.ofFile(partial));
            if (response.statusCode() != 200)
            {
                Files.deleteIfExists(partial);
                throw new IOException("Download responded with HTTP " + response.statusCode());
            }

            if (release.sha256() != null && !release.sha256().equalsIgnoreCase(sha256Of(partial)))
            {
                Files.deleteIfExists(partial);
                throw new IOException("Checksum mismatch, download discarded");
            }

            Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);

            notify(sender, "<green>Update downloaded successfully!");
            notify(sender, "<gray>It will be applied on the next server restart.");
            FLog.info("Update to " + release.tag() + " staged in " + updateFolder + ". Restart to apply.");
        }
        catch (Exception e)
        {
            FLog.error("Failed to download update: " + e.getMessage(), e);
            notify(sender, "<red>Failed to download update: " + e.getMessage());
        }
    }

    private static String sha256Of(Path file) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
    }

    @SuppressWarnings("ConstantConditions")
    private String currentVersion()
    {
        String build = plugin.getBuildMeta() != null ? plugin.getBuildMeta().getVersion() : null;
        if (isMeaningful(build)) return build;

        String meta = plugin.getPluginMeta() != null ? plugin.getPluginMeta().getVersion() : null;
        return isMeaningful(meta) ? meta : "unknown";
    }

    private static boolean isMeaningful(@Nullable String value)
    {
        return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value);
    }

    static boolean isOutdated(String current, String latest)
    {
        return !VersionUtil.normalizeVersion(latest).equalsIgnoreCase(VersionUtil.normalizeVersion(current));
    }

    private void announce(@Nullable CommandSender sender, String current, String latest)
    {
        notify(sender, DIVIDER);
        notify(sender, "<yellow>An update is available for <white>" + plugin.getName() + "<yellow>!");
        notify(sender, "<gray>Current version: <white>" + current);
        notify(sender, "<gray>Latest version:  <white>" + latest);
        notify(sender, "<gray>Use `/koolsmpcore update` to download it automatically, or grab it manually:");
        notify(sender, downloadLinks());
        notify(sender, DIVIDER);
    }

    private String manualDownloadMessage()
    {
        return "<gray>You can download the plugin manually here:\n" + downloadLinks();
    }

    private String downloadLinks()
    {
        StringBuilder links = new StringBuilder("<gray>• GitHub: <white>https://github.com/")
                .append(repoOwner).append('/').append(repoName).append("/releases/latest");
        if (spigotUrl != null && !spigotUrl.isEmpty())
            links.append("\n<gray>• SpigotMC: <white>").append(spigotUrl);
        if (modrinthUrl != null && !modrinthUrl.isEmpty())
            links.append("\n<gray>• Modrinth: <white>").append(modrinthUrl);
        return links.toString();
    }

    private void notify(@Nullable CommandSender sender, String miniMessage)
    {
        if (sender != null)
            sender.sendMessage(FUtil.miniMessage(miniMessage));
        else
            FLog.info(miniMessage.replaceAll("<[^>]+>", ""));
    }

    private record Release(String tag, @Nullable String downloadUrl, @Nullable String sha256) {}
}