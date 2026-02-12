package ai.clawphones.agent.chat;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dashboard activity for monitoring performance metrics.
 */
public class PerformanceDashboardActivity extends AppCompatActivity {

    private PerformanceService performanceService;
    private PerformanceService.PerformanceListener metricListener;

    // Health indicators
    private ImageView relayIndicator;
    private ImageView backendIndicator;
    private ImageView wsIndicator;
    private ImageView pushIndicator;

    // Gauges
    private ProgressBar memoryProgressBar;
    private TextView memoryValueText;
    private ProgressBar cpuProgressBar;
    private TextView cpuValueText;
    private ProgressBar batteryProgressBar;
    private TextView batteryValueText;
    private TextView frameRateText;

    // Network stats
    private TextView networkBytesInText;
    private TextView networkBytesOutText;
    private TextView networkConnectionsText;

    // Sync status
    private TextView syncPendingUploadsText;
    private TextView syncPendingDownloadsText;
    private TextView syncConflictsText;
    private TextView lastSyncText;
    private Button forceSyncButton;

    // Cache info
    private TextView cacheHitRateText;
    private TextView cacheSizeText;
    private Button clearCacheButton;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        performanceService = PerformanceService.getInstance(this);
        setupMetricListener();
        refreshCacheInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();
        performanceService.enableMonitoring(true);
        performanceService.collectAllMetrics();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep monitoring enabled for background collection
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (metricListener != null) {
            // Don't shutdown service, it's singleton
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        // Health indicators
        relayIndicator = findViewById(R.id.indicator_relay);
        backendIndicator = findViewById(R.id.indicator_backend);
        wsIndicator = findViewById(R.id.indicator_ws);
        pushIndicator = findViewById(R.id.indicator_push);

        // Gauges
        memoryProgressBar = findViewById(R.id.progress_memory);
        memoryValueText = findViewById(R.id.text_memory_value);
        cpuProgressBar = findViewById(R.id.progress_cpu);
        cpuValueText = findViewById(R.id.text_cpu_value);
        batteryProgressBar = findViewById(R.id.progress_battery);
        batteryValueText = findViewById(R.id.text_battery_value);
        frameRateText = findViewById(R.id.text_frame_rate);

        // Network stats
        networkBytesInText = findViewById(R.id.text_bytes_in);
        networkBytesOutText = findViewById(R.id.text_bytes_out);
        networkConnectionsText = findViewById(R.id.text_connections);

        // Sync status
        syncPendingUploadsText = findViewById(R.id.text_pending_uploads);
        syncPendingDownloadsText = findViewById(R.id.text_pending_downloads);
        syncConflictsText = findViewById(R.id.text_conflicts);
        lastSyncText = findViewById(R.id.text_last_sync);
        forceSyncButton = findViewById(R.id.button_force_sync);
        forceSyncButton.setOnClickListener(v -> performForceSync());

        // Cache info
        cacheHitRateText = findViewById(R.id.text_cache_hit_rate);
        cacheSizeText = findViewById(R.id.text_cache_size);
        clearCacheButton = findViewById(R.id.button_clear_cache);
        clearCacheButton.setOnClickListener(v -> showClearCacheDialog());
    }

    private void setupMetricListener() {
        metricListener = (appMetrics, syncStatus, healthCheck) ->
                runOnUiThread(() -> updateUI(appMetrics, syncStatus, healthCheck));
        performanceService.setListener(metricListener);
    }

    private void updateUI(PerformanceMetric.AppMetrics appMetrics,
                          PerformanceMetric.SyncStatus syncStatus,
                          PerformanceMetric.HealthCheck healthCheck) {
        if (appMetrics != null) {
            updateAppMetrics(appMetrics);
        }
        if (syncStatus != null) {
            updateSyncStatus(syncStatus);
        }
        if (healthCheck != null) {
            updateHealthIndicators(healthCheck);
        }
    }

    private void updateAppMetrics(PerformanceMetric.AppMetrics metrics) {
        // Memory (assume 512MB max for gauge)
        int memoryProgress = Math.min(100, (int) ((metrics.memoryUsageMB / 512.0) * 100));
        memoryProgressBar.setProgress(memoryProgress);
        memoryValueText.setText(String.format(Locale.getDefault(), "%.1f MB",
                metrics.memoryUsageMB));
        setProgressBarColor(memoryProgressBar, memoryProgress);

        // CPU
        int cpuProgress = Math.min(100, (int) metrics.cpuUsagePercent);
        cpuProgressBar.setProgress(cpuProgress);
        cpuValueText.setText(String.format(Locale.getDefault(), "%.1f%%",
                metrics.cpuUsagePercent));
        setProgressBarColor(cpuProgressBar, cpuProgress);

        // Battery drain
        int batteryProgress = Math.min(100, (int) metrics.batteryDrainPerHour);
        batteryProgressBar.setProgress(batteryProgress);
        batteryValueText.setText(String.format(Locale.getDefault(), "%.1f%%/h",
                metrics.batteryDrainPerHour));
        setProgressBarColor(batteryProgressBar, batteryProgress);

        // Frame rate (would come from actual FPS monitoring)
        frameRateText.setText(getString(R.string.performance_frame_rate_value, "60"));

        // Network
        networkBytesInText.setText(formatBytes(metrics.networkBytesIn));
        networkBytesOutText.setText(formatBytes(metrics.networkBytesOut));
        networkConnectionsText.setText(getString(R.string.performance_connections_value, 3));
    }

    private void updateSyncStatus(PerformanceMetric.SyncStatus status) {
        syncPendingUploadsText.setText(String.valueOf(status.pendingUploads));
        syncPendingDownloadsText.setText(String.valueOf(status.pendingDownloads));
        syncConflictsText.setText(String.valueOf(status.conflictCount));

        if (status.lastSyncAt > 0) {
            String syncTime = timeFormat.format(new Date(status.lastSyncAt));
            lastSyncText.setText(syncTime);
        } else {
            lastSyncText.setText(R.string.performance_sync_never);
        }
    }

    private void updateHealthIndicators(PerformanceMetric.HealthCheck health) {
        setIndicatorColor(relayIndicator, health.relayReachable);
        setIndicatorColor(backendIndicator, health.backendReachable);
        setIndicatorColor(wsIndicator, health.wsConnected);
        setIndicatorColor(pushIndicator, health.pushRegistered);

        // Update latency text
        TextView latencyText = findViewById(R.id.text_latency);
        if (health.latencyMs >= 0) {
            latencyText.setText(getString(R.string.performance_latency_value, health.latencyMs));
        } else {
            latencyText.setText(R.string.performance_latency_unavailable);
        }
    }

    private void setIndicatorColor(ImageView indicator, boolean healthy) {
        GradientDrawable drawable = (GradientDrawable) indicator.getBackground();
        int color = healthy ? 0xFF4CAF50 : healthy == false ? 0xFFF44336 : 0xFFFF9800;
        drawable.setColor(color);
        indicator.setBackground(drawable);
    }

    private void setProgressBarColor(ProgressBar progressBar, int progress) {
        GradientDrawable drawable = (GradientDrawable) progressBar.getProgressDrawable();
        if (progress < 50) {
            drawable.setColor(0xFF4CAF50); // Green
        } else if (progress < 80) {
            drawable.setColor(0xFFFF9800); // Orange
        } else {
            drawable.setColor(0xFFF44336); // Red
        }
        progressBar.setProgressDrawable(drawable);
    }

    private void performForceSync() {
        forceSyncButton.setEnabled(false);
        forceSyncButton.setText(R.string.performance_syncing);

        // Simulate sync
        forceSyncButton.postDelayed(() -> {
            forceSyncButton.setEnabled(true);
            forceSyncButton.setText(R.string.performance_force_sync);
            Toast.makeText(this, R.string.performance_sync_complete, Toast.LENGTH_SHORT).show();
            performanceService.collectAllMetrics();
        }, 2000);
    }

    private void refreshCacheInfo() {
        // Simulated cache data
        cacheHitRateText.setText(getString(R.string.performance_cache_hit_rate, "85%"));
        cacheSizeText.setText(formatBytes(256 * 1024 * 1024)); // 256MB
    }

    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.performance_clear_cache_title)
                .setMessage(R.string.performance_clear_cache_message)
                .setPositiveButton(R.string.performance_clear, (dialog, which) -> {
                    // Simulate clearing cache
                    Toast.makeText(this, R.string.performance_cache_cleared,
                            Toast.LENGTH_SHORT).show();
                    refreshCacheInfo();
                })
                .setNegativeButton(R.string.performance_cancel, null)
                .show();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB",
                bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.getDefault(),
                "%.1f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.2f GB",
                bytes / (1024.0 * 1024 * 1024));
    }
}
