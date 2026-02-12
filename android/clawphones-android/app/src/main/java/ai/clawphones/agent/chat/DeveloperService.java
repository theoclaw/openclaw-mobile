package ai.clawphones.agent.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Developer service for managing API keys, webhooks, usage analytics, and plugins.
 * Extends Observable pattern following existing CommunityService architecture.
 */
public final class DeveloperService {

    private static final String LOG_TAG = "DeveloperService";
    private static final String BASE_URL = ClawPhonesAPI.BASE_URL;
    private static final String DEVELOPER_BASE = "/v1/developer";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static volatile DeveloperService instance;

    private final Context appContext;
    private final OkHttpClient httpClient;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;

    /**
     * Custom exception for developer API errors.
     */
    public static final class DeveloperException extends Exception {
        public final int statusCode;

        DeveloperException(int statusCode, @NonNull String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    /**
     * Webhook test result containing delivery status and response details.
     */
    public static final class WebhookTestResult {
        public final boolean success;
        @Nullable public final String message;
        public final int httpStatusCode;
        public final long responseTimeMs;

        public WebhookTestResult(boolean success, @Nullable String message, int httpStatusCode, long responseTimeMs) {
            this.success = success;
            this.message = message;
            this.httpStatusCode = httpStatusCode;
            this.responseTimeMs = responseTimeMs;
        }
    }

    // Callback interfaces for async operations

    public interface APIKeyCallback {
        void onSuccess(DeveloperSDK.APIKey apiKey);
        void onError(Exception error);
    }

    public interface APIKeysCallback {
        void onSuccess(List<DeveloperSDK.APIKey> apiKeys);
        void onError(Exception error);
    }

    public interface VoidCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public interface WebhookConfigCallback {
        void onSuccess(DeveloperSDK.WebhookConfig webhookConfig);
        void onError(Exception error);
    }

    public interface WebhookConfigsCallback {
        void onSuccess(List<DeveloperSDK.WebhookConfig> webhookConfigs);
        void onError(Exception error);
    }

    public interface WebhookTestCallback {
        void onSuccess(WebhookTestResult result);
        void onError(Exception error);
    }

    public interface UsageRecordsCallback {
        void onSuccess(List<DeveloperSDK.UsageRecord> usageRecords);
        void onError(Exception error);
    }

    public interface PluginsCallback {
        void onSuccess(List<DeveloperSDK.SDKPlugin> plugins);
        void onError(Exception error);
    }

    public interface PluginCallback {
        void onSuccess(DeveloperSDK.SDKPlugin plugin);
        void onError(Exception error);
    }

    private DeveloperService(@NonNull Context context) {
        this(context.getApplicationContext(), ClawPhonesAPI.getOkHttpClient());
    }

    private DeveloperService(@NonNull Context context, @NonNull OkHttpClient okHttpClient) {
        this.appContext = context;
        this.httpClient = okHttpClient;
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    public static DeveloperService getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (DeveloperService.class) {
                if (instance == null) {
                    instance = new DeveloperService(context);
                }
            }
        }
        return instance;
    }

    // ── API Key Management ─────────────────────────────────────────────────────

    /**
     * Generate a new API key asynchronously.
     */
    public void generateAPIKey(
        @NonNull String name,
        @NonNull List<String> permissions,
        @Nullable Integer rateLimit,
        @Nullable Long expiresAt,
        @NonNull APIKeyCallback callback
    ) {
        backgroundExecutor.execute(() -> {
            try {
                DeveloperSDK.APIKey apiKey = generateAPIKeySync(name, permissions, rateLimit, expiresAt);
                runOnMainThread(() -> callback.onSuccess(apiKey));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Generate a new API key synchronously.
     */
    @NonNull
    public DeveloperSDK.APIKey generateAPIKeySync(
        @NonNull String name,
        @NonNull List<String> permissions,
        @Nullable Integer rateLimit,
        @Nullable Long expiresAt
    ) throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        JSONObject body = new JSONObject();
        body.put("name", name);

        JSONArray permsArray = new JSONArray();
        for (String perm : permissions) {
            permsArray.put(perm);
        }
        body.put("permissions", permsArray);

        if (rateLimit != null) {
            body.put("rate_limit", rateLimit);
        }

        if (expiresAt != null) {
            body.put("expires_at", expiresAt);
        }

        JSONObject response = executeJson(postRequest(DEVELOPER_BASE + "/api-keys", body, token));
        return DeveloperSDK.APIKey.fromJson(response);
    }

    /**
     * Revoke an API key asynchronously.
     */
    public void revokeKey(@NonNull String keyId, @NonNull VoidCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                revokeKeySync(keyId);
                runOnMainThread(callback::onSuccess);
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Revoke an API key synchronously.
     */
    public void revokeKeySync(@NonNull String keyId) throws IOException, DeveloperException {
        String token = resolveAuthToken();
        String url = DEVELOPER_BASE + "/api-keys/" + keyId;
        Request request = new Request.Builder()
            .url(BASE_URL + url)
            .delete()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        executeJson(request);
    }

    /**
     * List all API keys asynchronously.
     */
    public void listKeys(@NonNull APIKeysCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<DeveloperSDK.APIKey> keys = listKeysSync();
                runOnMainThread(() -> callback.onSuccess(keys));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * List all API keys synchronously.
     */
    @NonNull
    public List<DeveloperSDK.APIKey> listKeysSync() throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        Request request = new Request.Builder()
            .url(BASE_URL + DEVELOPER_BASE + "/api-keys")
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        JSONObject response = executeJson(request);

        List<DeveloperSDK.APIKey> keys = new ArrayList<>();
        JSONArray array = response.optJSONArray("api_keys");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    try {
                        keys.add(DeveloperSDK.APIKey.fromJson(item));
                    } catch (JSONException e) {
                        Logger.logWarn(LOG_TAG, "Failed to parse API key: " + e.getMessage());
                    }
                }
            }
        }
        return keys;
    }

    // ── Webhook Management ─────────────────────────────────────────────────────

    /**
     * Register a new webhook asynchronously.
     */
    public void registerWebhook(
        @NonNull String url,
        @NonNull List<String> events,
        @Nullable String secret,
        @NonNull WebhookConfigCallback callback
    ) {
        backgroundExecutor.execute(() -> {
            try {
                DeveloperSDK.WebhookConfig webhook = registerWebhookSync(url, events, secret);
                runOnMainThread(() -> callback.onSuccess(webhook));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Register a new webhook synchronously.
     */
    @NonNull
    public DeveloperSDK.WebhookConfig registerWebhookSync(
        @NonNull String url,
        @NonNull List<String> events,
        @Nullable String secret
    ) throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        JSONObject body = new JSONObject();
        body.put("url", url);

        JSONArray eventsArray = new JSONArray();
        for (String event : events) {
            eventsArray.put(event);
        }
        body.put("events", eventsArray);

        if (secret != null) {
            body.put("secret", secret);
        }

        JSONObject response = executeJson(postRequest(DEVELOPER_BASE + "/webhooks", body, token));
        return DeveloperSDK.WebhookConfig.fromJson(response);
    }

    /**
     * Delete a webhook asynchronously.
     */
    public void deleteWebhook(@NonNull String webhookId, @NonNull VoidCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                deleteWebhookSync(webhookId);
                runOnMainThread(callback::onSuccess);
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Delete a webhook synchronously.
     */
    public void deleteWebhookSync(@NonNull String webhookId) throws IOException, DeveloperException {
        String token = resolveAuthToken();
        String url = DEVELOPER_BASE + "/webhooks/" + webhookId;
        Request request = new Request.Builder()
            .url(BASE_URL + url)
            .delete()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        executeJson(request);
    }

    /**
     * Test a webhook by sending a test event asynchronously.
     */
    public void testWebhook(@NonNull String webhookId, @NonNull WebhookTestCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                WebhookTestResult result = testWebhookSync(webhookId);
                runOnMainThread(() -> callback.onSuccess(result));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Test a webhook by sending a test event synchronously.
     */
    @NonNull
    public WebhookTestResult testWebhookSync(@NonNull String webhookId) throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        JSONObject body = new JSONObject();
        body.put("event_type", "test");

        long startTime = System.currentTimeMillis();
        JSONObject response = executeJson(postRequest(DEVELOPER_BASE + "/webhooks/" + webhookId + "/test", body, token));
        long responseTime = System.currentTimeMillis() - startTime;

        boolean success = response.optBoolean("success", false);
        String message = response.optString("message", null);
        int httpStatusCode = response.optInt("http_status_code", 0);

        return new WebhookTestResult(success, message, httpStatusCode, responseTime);
    }

    // ── Usage Analytics ───────────────────────────────────────────────────────

    /**
     * Fetch usage records asynchronously.
     */
    public void fetchUsage(
        @Nullable String startDate,
        @Nullable String endDate,
        @Nullable String endpoint,
        @NonNull UsageRecordsCallback callback
    ) {
        backgroundExecutor.execute(() -> {
            try {
                List<DeveloperSDK.UsageRecord> records = fetchUsageSync(startDate, endDate, endpoint);
                runOnMainThread(() -> callback.onSuccess(records));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Fetch usage records synchronously.
     */
    @NonNull
    public List<DeveloperSDK.UsageRecord> fetchUsageSync(
        @Nullable String startDate,
        @Nullable String endDate,
        @Nullable String endpoint
    ) throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + DEVELOPER_BASE + "/usage").newBuilder();

        if (startDate != null && !startDate.isEmpty()) {
            urlBuilder.addQueryParameter("start_date", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            urlBuilder.addQueryParameter("end_date", endDate);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            urlBuilder.addQueryParameter("endpoint", endpoint);
        }

        Request request = new Request.Builder()
            .url(urlBuilder.build())
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        JSONObject response = executeJson(request);

        List<DeveloperSDK.UsageRecord> records = new ArrayList<>();
        JSONArray array = response.optJSONArray("usage_records");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    try {
                        records.add(DeveloperSDK.UsageRecord.fromJson(item));
                    } catch (JSONException e) {
                        Logger.logWarn(LOG_TAG, "Failed to parse usage record: " + e.getMessage());
                    }
                }
            }
        }
        return records;
    }

    // ── Plugin Management ──────────────────────────────────────────────────────

    /**
     * Fetch available plugins asynchronously.
     */
    public void fetchPlugins(@NonNull PluginsCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                List<DeveloperSDK.SDKPlugin> plugins = fetchPluginsSync();
                runOnMainThread(() -> callback.onSuccess(plugins));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Fetch available plugins synchronously.
     */
    @NonNull
    public List<DeveloperSDK.SDKPlugin> fetchPluginsSync() throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        Request request = new Request.Builder()
            .url(BASE_URL + DEVELOPER_BASE + "/plugins")
            .get()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        JSONObject response = executeJson(request);

        List<DeveloperSDK.SDKPlugin> plugins = new ArrayList<>();
        JSONArray array = response.optJSONArray("plugins");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    try {
                        plugins.add(DeveloperSDK.SDKPlugin.fromJson(item));
                    } catch (JSONException e) {
                        Logger.logWarn(LOG_TAG, "Failed to parse plugin: " + e.getMessage());
                    }
                }
            }
        }
        return plugins;
    }

    /**
     * Install a plugin asynchronously.
     */
    public void installPlugin(@NonNull String pluginId, @NonNull PluginCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                DeveloperSDK.SDKPlugin plugin = installPluginSync(pluginId);
                runOnMainThread(() -> callback.onSuccess(plugin));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Install a plugin synchronously.
     */
    @NonNull
    public DeveloperSDK.SDKPlugin installPluginSync(@NonNull String pluginId) throws IOException, DeveloperException, JSONException {
        String token = resolveAuthToken();
        JSONObject body = new JSONObject();
        body.put("plugin_id", pluginId);

        JSONObject response = executeJson(postRequest(DEVELOPER_BASE + "/plugins/install", body, token));
        return DeveloperSDK.SDKPlugin.fromJson(response);
    }

    /**
     * Uninstall a plugin asynchronously.
     */
    public void uninstallPlugin(@NonNull String pluginId, @NonNull VoidCallback callback) {
        backgroundExecutor.execute(() -> {
            try {
                uninstallPluginSync(pluginId);
                runOnMainThread(callback::onSuccess);
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError(e));
            }
        });
    }

    /**
     * Uninstall a plugin synchronously.
     */
    public void uninstallPluginSync(@NonNull String pluginId) throws IOException, DeveloperException {
        String token = resolveAuthToken();
        String url = DEVELOPER_BASE + "/plugins/" + pluginId;
        Request request = new Request.Builder()
            .url(BASE_URL + url)
            .delete()
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
        executeJson(request);
    }

    // ── Internal Helpers ─────────────────────────────────────────────────────

    private String resolveAuthToken() throws DeveloperException {
        String token = ClawPhonesAPI.getToken(appContext);
        if (token == null || token.trim().isEmpty()) {
            throw new DeveloperException(401, "missing bearer token");
        }
        return token.trim();
    }

    @NonNull
    private Request postRequest(@NonNull String path, @NonNull JSONObject body, @NonNull String token) {
        String url = BASE_URL + path;
        return new Request.Builder()
            .url(url)
            .post(RequestBody.create(JSON_MEDIA_TYPE, body.toString()))
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer " + token)
            .build();
    }

    @NonNull
    private JSONObject executeJson(@NonNull Request request) throws IOException, DeveloperException, JSONException {
        Response response = null;
        try {
            response = httpClient.newCall(request).execute();
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) {
                throw new DeveloperException(response.code(), body.isEmpty() ? response.message() : body);
            }
            if (body.trim().isEmpty()) {
                return new JSONObject();
            }
            return new JSONObject(body);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private void runOnMainThread(@NonNull Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            mainHandler.post(action);
        }
    }
}
