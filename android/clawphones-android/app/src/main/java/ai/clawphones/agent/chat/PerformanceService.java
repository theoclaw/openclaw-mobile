package ai.clawphones.agent.chat;

import android.app.ActivityManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service for collecting and reporting performance metrics.
 */
public class PerformanceService {
    private static final String TAG = "PerformanceService";

    private static final long MONITOR_INTERVAL_MS = 60_000; // 60 seconds
    private static final int HEALTH_CHECK_TIMEOUT_MS = 5_000;

    private static volatile PerformanceService instance;
    private final Context context;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private boolean monitoringEnabled = false;

    private PerformanceMetric.AppMetrics currentAppMetrics;
    private PerformanceMetric.SyncStatus currentSyncStatus;
    private PerformanceMetric.HealthCheck currentHealthCheck;
    private PerformanceListener listener;

    private long startTime = SystemClock.elapsedRealtime();
    private long lastNetworkBytesIn = 0;
    private long lastNetworkBytesOut = 0;
    private int batteryLevelLast = 100;
    private long batteryLevelTimeLast = System.currentTimeMillis();

    private PerformanceService(Context context) {
        this.context = context.getApplicationContext();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceMonitor");
            t.setDaemon(true);
            return t;
        });
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "PerformanceWorker");
            t.setDaemon(true);
            return t;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get the singleton instance.
     */
    public static PerformanceService getInstance(Context context) {
        if (instance == null) {
            synchronized (PerformanceService.class) {
                if (instance == null) {
                    instance = new PerformanceService(context);
                }
            }
        }
        return instance;
    }

    /**
     * Set a listener for metric updates.
     */
    public void setListener(PerformanceListener listener) {
        this.listener = listener;
    }

    /**
     * Start monitoring with periodic collection.
     */
    public void enableMonitoring(boolean enable) {
        if (monitoringEnabled == enable) return;

        monitoringEnabled = enable;
        if (enable) {
            scheduler.scheduleAtFixedRate(this::collectAllMetrics,
                    0, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
            Log.d(TAG, "Performance monitoring enabled");
        } else {
            scheduler.shutdownNow();
            Log.d(TAG, "Performance monitoring disabled");
        }
    }

    /**
     * Collect all metrics at once.
     */
    public void collectAllMetrics() {
        executor.execute(() -> {
            currentAppMetrics = collectMetrics();
            currentHealthCheck = runHealthCheck();
            // Sync status would come from actual sync service
            currentSyncStatus = new PerformanceMetric.SyncStatus(
                    System.currentTimeMillis(), 0, 0, 0);

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onMetricsUpdated(currentAppMetrics,
                            currentSyncStatus, currentHealthCheck);
                }
            });
        });
    }

    /**
     * Collect app performance metrics using system APIs.
     */
    public PerformanceMetric.AppMetrics collectMetrics() {
        Runtime runtime = Runtime.getRuntime();
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        BatteryManager batteryManager =
                (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

        // Memory usage in MB
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsageMB = usedMemory / (1024.0 * 1024.0);

        // CPU usage estimation
        double cpuUsagePercent = 0.0;
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            // Approximate CPU usage from available memory pressure
            cpuUsagePercent = (1.0 - (double) memoryInfo.availMem / memoryInfo.totalMem) * 100;
        }

        // Battery drain per hour estimation
        double batteryDrainPerHour = 0.0;
        if (batteryManager != null) {
            int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            long now = System.currentTimeMillis();
            long hoursElapsed = (now - batteryLevelTimeLast) / 3600000.0;
            if (hoursElapsed > 0 && batteryLevel < batteryLevelLast) {
                batteryDrainPerHour = (batteryLevelLast - batteryLevel) / hoursElapsed;
            }
            batteryLevelLast = batteryLevel;
            batteryLevelTimeLast = now;
        }

        // Startup time
        long startupTimeMs = SystemClock.elapsedRealtime() - startTime;

        // Network stats ( TrafficStats would be used here for real implementation)
        long networkBytesIn = android.net.TrafficStats.getTotalRxBytes();
        long networkBytesOut = android.net.TrafficStats.getTotalTxBytes();

        return new PerformanceMetric.AppMetrics(startupTimeMs, memoryUsageMB,
                cpuUsagePercent, batteryDrainPerHour, networkBytesIn, networkBytesOut);
    }

    /**
     * Run health check on all services.
     */
    public PerformanceMetric.HealthCheck runHealthCheck() {
        boolean relayReachable = checkEndpointReachable("https://relay.clawphones.com");
        boolean backendReachable = checkEndpointReachable("https://api.clawphones.com");
        // WebSocket and push status would come from actual connection managers
        boolean wsConnected = false;
        boolean pushRegistered = false;

        // Measure latency to backend
        long latencyMs = measureLatency("https://api.clawphones.com");

        return new PerformanceMetric.HealthCheck(relayReachable, backendReachable,
                wsConnected, pushRegistered, latencyMs);
    }

    /**
     * Check if an endpoint is reachable.
     */
    private boolean checkEndpointReachable(String endpoint) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
            connection.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode >= 200 && responseCode < 400;
        } catch (IOException e) {
            Log.w(TAG, "Health check failed for " + endpoint, e);
            return false;
        }
    }

    /**
     * Measure latency to an endpoint.
     */
    private long measureLatency(String endpoint) {
        long start = System.currentTimeMillis();
        try {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(HEALTH_CHECK_TIMEOUT_MS);
            connection.setReadTimeout(HEALTH_CHECK_TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            connection.getResponseCode();
            connection.disconnect();
            return System.currentTimeMillis() - start;
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Get current sync status.
     */
    public PerformanceMetric.SyncStatus getSyncStatus() {
        return currentSyncStatus;
    }

    /**
     * Get current app metrics.
     */
    public PerformanceMetric.AppMetrics getAppMetrics() {
        return currentAppMetrics;
    }

    /**
     * Get current health check.
     */
    public PerformanceMetric.HealthCheck getHealthCheck() {
        return currentHealthCheck;
    }

    /**
     * Report metrics to backend.
     */
    public void reportMetrics(@NonNull String apiUrl,
                              @Nullable PerformanceMetric.AppMetrics metrics,
                              @Nullable PerformanceMetric.HealthCheck health) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                if (metrics != null) {
                    payload.put("startupTimeMs", metrics.startupTimeMs);
                    payload.put("memoryUsageMB", metrics.memoryUsageMB);
                    payload.put("cpuUsagePercent", metrics.cpuUsagePercent);
                    payload.put("batteryDrainPerHour", metrics.batteryDrainPerHour);
                    payload.put("networkBytesIn", metrics.networkBytesIn);
                    payload.put("networkBytesOut", metrics.networkBytesOut);
                }
                if (health != null) {
                    JSONObject healthObj = new JSONObject();
                    healthObj.put("relayReachable", health.relayReachable);
                    healthObj.put("backendReachable", health.backendReachable);
                    healthObj.put("wsConnected", health.wsConnected);
                    healthObj.put("pushRegistered", health.pushRegistered);
                    healthObj.put("latencyMs", health.latencyMs);
                    payload.put("health", healthObj);
                }
                payload.put("timestamp", System.currentTimeMillis());

                // POST to backend - would use actual HTTP client in production
                Log.d(TAG, "Reporting metrics: " + payload.toString());

            } catch (JSONException e) {
                Log.e(TAG, "Failed to build metrics payload", e);
            }
        });
    }

    /**
     * Shutdown the service.
     */
    public void shutdown() {
        enableMonitoring(false);
        executor.shutdownNow();
        instance = null;
    }

    /**
     * Listener interface for metric updates.
     */
    public interface PerformanceListener {
        void onMetricsUpdated(PerformanceMetric.AppMetrics appMetrics,
                             PerformanceMetric.SyncStatus syncStatus,
                             PerformanceMetric.HealthCheck healthCheck);
    }
}
