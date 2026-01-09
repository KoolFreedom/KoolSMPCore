package eu.koolfreedom.util;

import eu.koolfreedom.KoolSMPCore;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

@SuppressWarnings("UnstableApiUsage")
public record UpdateChecker(KoolSMPCore plugin, String repoOwner, String repoName, String spigotUrl,
                            String modrinthUrl) {

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest", repoOwner, repoName);
                HttpURLConnection connection = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
                connection.setRequestProperty("Accept", "application/vnd.github+json");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
                {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null)
                    {
                        response.append(line);
                    }

                    String json = response.toString();
                    String latestTag = extractJsonValue(json);
                    if (latestTag == null)
                    {
                        FLog.warning("Could not parse the latest release tag from GitHub API.");
                        return;
                    }

                    String currentVersion = plugin.getPluginMeta().getVersion();
                    String normalizedLatest = normalizeVersion(latestTag);
                    String normalizedCurrent = normalizeVersion(currentVersion);

                    if (!normalizedLatest.equalsIgnoreCase(normalizedCurrent))
                    {
                        logUpdateAvailable(currentVersion, latestTag);
                    }
                    else
                    {
                        FLog.info(String.format("You are running the latest version of %s (%s)", plugin.getName(), currentVersion));
                    }
                }
            }
            catch (Exception e)
            {
                FLog.warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private void logUpdateAvailable(String currentVersion, String latestTag) {
        FLog.warning("------------------------------------------------");
        FLog.warning(String.format("An update is available for %s!", plugin.getName()));
        FLog.warning("Current version: " + currentVersion);
        FLog.warning("Latest version: " + latestTag);
        FLog.warning("");
        FLog.warning("Download links:");
        FLog.warning("• GitHub:   https://github.com/" + repoOwner + "/" + repoName + "/releases/latest");
        if (spigotUrl != null && !spigotUrl.isEmpty()) FLog.warning("• SpigotMC: " + spigotUrl);
        if (modrinthUrl != null && !modrinthUrl.isEmpty()) FLog.warning("• Modrinth: " + modrinthUrl);
        FLog.warning("------------------------------------------------");
    }

    private String normalizeVersion(String version) {
        return version.replaceFirst("(?i)^v", "").trim();
    }

    private String extractJsonValue(String json) {
        try {
            return json.split("\"" + "tag_name" + "\":\"")[1].split("\"")[0];
        } catch (Exception e) {
            return null;
        }
    }
}
