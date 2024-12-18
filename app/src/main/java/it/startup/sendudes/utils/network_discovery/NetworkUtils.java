package it.startup.sendudes.utils.network_discovery;

public class NetworkUtils {

    /**
     * Converts the integer representing bytes to a readable string.
     *
     * @param bytes the size in bytes
     * @return a human-readable file size string
     */
    public static String readableFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
