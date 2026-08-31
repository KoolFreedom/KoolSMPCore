package eu.koolfreedom.util;

public final class VersionUtil {
    private VersionUtil() {}

    public static String normalizeVersion(String version) {
        return version == null ? "" : version.replaceFirst("(?i)^v", "").trim();
    }
}
