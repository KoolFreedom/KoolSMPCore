package eu.koolfreedom.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

public class UpdateChecker {
    private final Plugin plugin;
    private final String repoOwner;
    private final String repoName;

    public UpdateChecker(Plugin plugin, String repoOwner, String repoName) {
        this.plugin = plugin;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public void check() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URI uri = URI.create("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest");
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github+json");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String json = response.toString();
                    String latestTag = json.split("\"tag_name\":\"")[1].split("\"")[0];

                    String currentVersion = plugin.getPluginMeta().getVersion();

                    // Normalize: strip leading 'v' or 'V'
                    String normalizedLatest = latestTag.replaceFirst("(?i)^v", "");
                    String normalizedCurrent = currentVersion.replaceFirst("(?i)^v", "");

                    if (!normalizedLatest.equalsIgnoreCase(normalizedCurrent))
                    {
                        FLog.warning("------------------------------------------------");
                        FLog.warning("An update is available for " + plugin.getName() + "!");
                        FLog.warning("Current version: " + currentVersion);
                        FLog.warning("Latest version: " + latestTag);
                        FLog.warning("Download: https://github.com/" + repoOwner + "/" + repoName + "/releases/latest");
                        FLog.warning("------------------------------------------------");
                    }
                    else
                    {
                        FLog.info("You are running the latest version of KoolSMPCore");
                    }
                }
            } catch (Exception e) {
                FLog.warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }
}
