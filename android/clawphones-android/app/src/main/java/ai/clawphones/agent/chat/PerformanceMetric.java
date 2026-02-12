package ai.clawphones.agent.chat;

import androidx.annotation.NonNull;

/**
 * Performance metric data classes for monitoring application health.
 */
public class PerformanceMetric {

    /**
     * App-level performance metrics.
     */
    public static class AppMetrics {
        public final long startupTimeMs;
        public final double memoryUsageMB;
        public final double cpuUsagePercent;
        public final double batteryDrainPerHour;
        public final long networkBytesIn;
        public final long networkBytesOut;

        public AppMetrics(long startupTimeMs, double memoryUsageMB, double cpuUsagePercent,
                          double batteryDrainPerHour, long networkBytesIn, long networkBytesOut) {
            this.startupTimeMs = startupTimeMs;
            this.memoryUsageMB = memoryUsageMB;
            this.cpuUsagePercent = cpuUsagePercent;
            this.batteryDrainPerHour = batteryDrainPerHour;
            this.networkBytesIn = networkBytesIn;
            this.networkBytesOut = networkBytesOut;
        }

        @NonNull
        @Override
        public String toString() {
            return "AppMetrics{startup=" + startupTimeMs + "ms, mem=" + memoryUsageMB + "MB, " +
                    "cpu=" + cpuUsagePercent + "%, batt=" + batteryDrainPerHour + "%/h, " +
                    "netIn=" + networkBytesIn + ", netOut=" + networkBytesOut + "}";
        }
    }

    /**
     * Data synchronization status.
     */
    public static class SyncStatus {
        public final long lastSyncAt;
        public final int pendingUploads;
        public final int pendingDownloads;
        public final int conflictCount;

        public SyncStatus(long lastSyncAt, int pendingUploads, int pendingDownloads, int conflictCount) {
            this.lastSyncAt = lastSyncAt;
            this.pendingUploads = pendingUploads;
            this.pendingDownloads = pendingDownloads;
            this.conflictCount = conflictCount;
        }

        @NonNull
        @Override
        public String toString() {
            return "SyncStatus{lastSync=" + lastSyncAt + ", uploads=" + pendingUploads +
                    ", downloads=" + pendingDownloads + ", conflicts=" + conflictCount + "}";
        }
    }

    /**
     * Health check status for various services.
     */
    public static class HealthCheck {
        public final boolean relayReachable;
        public final boolean backendReachable;
        public final boolean wsConnected;
        public final boolean pushRegistered;
        public final long latencyMs;

        public HealthCheck(boolean relayReachable, boolean backendReachable,
                           boolean wsConnected, boolean pushRegistered, long latencyMs) {
            this.relayReachable = relayReachable;
            this.backendReachable = backendReachable;
            this.wsConnected = wsConnected;
            this.pushRegistered = pushRegistered;
            this.latencyMs = latencyMs;
        }

        public int getHealthyCount() {
            int count = 0;
            if (relayReachable) count++;
            if (backendReachable) count++;
            if (wsConnected) count++;
            if (pushRegistered) count++;
            return count;
        }

        @NonNull
        @Override
        public String toString() {
            return "HealthCheck{relay=" + relayReachable + ", backend=" + backendReachable +
                    ", ws=" + wsConnected + ", push=" + pushRegistered + ", latency=" + latencyMs + "ms}";
        }
    }
}
