package ai.clawphones.agent.chat;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Developer SDK data models for API key management, webhooks, usage tracking,
 * and plugin management for ClawPhones Developer Portal.
 */
public final class DeveloperSDK {

    /**
     * API key representing developer credentials for accessing ClawPhones APIs.
     */
    public static final class APIKey {
        @NonNull public final String id;
        @NonNull public final String name;
        @NonNull public final String key;
        @NonNull public final List<String> permissions;
        public final int rateLimit;
        public final long createdAt;
        public final long expiresAt;
        public final boolean isActive;

        public APIKey(
            @NonNull String id,
            @NonNull String name,
            @NonNull String key,
            @NonNull List<String> permissions,
            int rateLimit,
            long createdAt,
            long expiresAt,
            boolean isActive
        ) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.permissions = permissions != null
                ? Collections.unmodifiableList(new ArrayList<>(permissions))
                : Collections.emptyList();
            this.rateLimit = rateLimit;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.isActive = isActive;
        }

        @NonNull
        public static APIKey fromJson(@NonNull JSONObject json) throws JSONException {
            String id = json.optString("id", "");
            if (TextUtils.isEmpty(id)) {
                throw new JSONException("Missing id field");
            }

            String name = json.optString("name", "");
            if (TextUtils.isEmpty(name)) {
                throw new JSONException("Missing name field");
            }

            String key = json.optString("key", "");
            if (TextUtils.isEmpty(key)) {
                throw new JSONException("Missing key field");
            }

            List<String> permissions = new ArrayList<>();
            JSONArray permsArray = json.optJSONArray("permissions");
            if (permsArray != null) {
                for (int i = 0; i < permsArray.length(); i++) {
                    String perm = permsArray.optString(i, null);
                    if (!TextUtils.isEmpty(perm)) {
                        permissions.add(perm);
                    }
                }
            }

            int rateLimit = json.optInt("rate_limit", 100);
            long createdAt = json.optLong("created_at", 0L);
            long expiresAt = json.optLong("expires_at", 0L);
            boolean isActive = json.optBoolean("is_active", true);

            return new APIKey(id, name, key, permissions, rateLimit, createdAt, expiresAt, isActive);
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("key", key);

            JSONArray permsArray = new JSONArray();
            for (String perm : permissions) {
                permsArray.put(perm);
            }
            json.put("permissions", permsArray);

            json.put("rate_limit", rateLimit);
            json.put("created_at", createdAt);
            json.put("expires_at", expiresAt);
            json.put("is_active", isActive);

            return json;
        }
    }

    /**
     * Webhook configuration for receiving API event notifications.
     */
    public static final class WebhookConfig {
        @NonNull public final String id;
        @NonNull public final String url;
        @NonNull public final List<String> events;
        @Nullable public final String secret;
        public final boolean isActive;

        public WebhookConfig(
            @NonNull String id,
            @NonNull String url,
            @NonNull List<String> events,
            @Nullable String secret,
            boolean isActive
        ) {
            this.id = id;
            this.url = url;
            this.events = events != null
                ? Collections.unmodifiableList(new ArrayList<>(events))
                : Collections.emptyList();
            this.secret = secret;
            this.isActive = isActive;
        }

        @NonNull
        public static WebhookConfig fromJson(@NonNull JSONObject json) throws JSONException {
            String id = json.optString("id", "");
            if (TextUtils.isEmpty(id)) {
                throw new JSONException("Missing id field");
            }

            String url = json.optString("url", "");
            if (TextUtils.isEmpty(url)) {
                throw new JSONException("Missing url field");
            }

            List<String> events = new ArrayList<>();
            JSONArray eventsArray = json.optJSONArray("events");
            if (eventsArray != null) {
                for (int i = 0; i < eventsArray.length(); i++) {
                    String event = eventsArray.optString(i, null);
                    if (!TextUtils.isEmpty(event)) {
                        events.add(event);
                    }
                }
            }

            String secret = json.optString("secret", null);
            if (json.isNull("secret") || TextUtils.isEmpty(secret)) {
                secret = null;
            }

            boolean isActive = json.optBoolean("is_active", true);

            return new WebhookConfig(id, url, events, secret, isActive);
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("url", url);

            JSONArray eventsArray = new JSONArray();
            for (String event : events) {
                eventsArray.put(event);
            }
            json.put("events", eventsArray);

            if (secret != null) {
                json.put("secret", secret);
            } else {
                json.put("secret", JSONObject.NULL);
            }

            json.put("is_active", isActive);

            return json;
        }
    }

    /**
     * Usage record tracking API endpoint calls and latency metrics.
     */
    public static final class UsageRecord {
        @NonNull public final String date;
        @NonNull public final String endpoint;
        public final int count;
        public final long latencyMs;

        public UsageRecord(
            @NonNull String date,
            @NonNull String endpoint,
            int count,
            long latencyMs
        ) {
            this.date = date;
            this.endpoint = endpoint;
            this.count = count;
            this.latencyMs = latencyMs;
        }

        @NonNull
        public static UsageRecord fromJson(@NonNull JSONObject json) throws JSONException {
            String date = json.optString("date", "");
            if (TextUtils.isEmpty(date)) {
                throw new JSONException("Missing date field");
            }

            String endpoint = json.optString("endpoint", "");
            if (TextUtils.isEmpty(endpoint)) {
                throw new JSONException("Missing endpoint field");
            }

            int count = json.optInt("count", 0);
            long latencyMs = json.optLong("latency_ms", 0L);

            return new UsageRecord(date, endpoint, count, latencyMs);
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("date", date);
            json.put("endpoint", endpoint);
            json.put("count", count);
            json.put("latency_ms", latencyMs);
            return json;
        }
    }

    /**
     * SDK plugin representing extendable modules for the ClawPhones platform.
     */
    public static final class SDKPlugin {
        @NonNull public final String id;
        @NonNull public final String name;
        @NonNull public final String version;
        @Nullable public final String description;
        @Nullable public final String author;
        @Nullable public final String downloadUrl;
        public final boolean isInstalled;

        public SDKPlugin(
            @NonNull String id,
            @NonNull String name,
            @NonNull String version,
            @Nullable String description,
            @Nullable String author,
            @Nullable String downloadUrl,
            boolean isInstalled
        ) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.author = author;
            this.downloadUrl = downloadUrl;
            this.isInstalled = isInstalled;
        }

        @NonNull
        public static SDKPlugin fromJson(@NonNull JSONObject json) throws JSONException {
            String id = json.optString("id", "");
            if (TextUtils.isEmpty(id)) {
                throw new JSONException("Missing id field");
            }

            String name = json.optString("name", "");
            if (TextUtils.isEmpty(name)) {
                throw new JSONException("Missing name field");
            }

            String version = json.optString("version", "");
            if (TextUtils.isEmpty(version)) {
                throw new JSONException("Missing version field");
            }

            String description = json.optString("description", null);
            if (json.isNull("description") || TextUtils.isEmpty(description)) {
                description = null;
            }

            String author = json.optString("author", null);
            if (json.isNull("author") || TextUtils.isEmpty(author)) {
                author = null;
            }

            String downloadUrl = json.optString("download_url", null);
            if (json.isNull("download_url") || TextUtils.isEmpty(downloadUrl)) {
                downloadUrl = null;
            }

            boolean isInstalled = json.optBoolean("is_installed", false);

            return new SDKPlugin(id, name, version, description, author, downloadUrl, isInstalled);
        }

        @NonNull
        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("version", version);

            if (description != null) {
                json.put("description", description);
            } else {
                json.put("description", JSONObject.NULL);
            }

            if (author != null) {
                json.put("author", author);
            } else {
                json.put("author", JSONObject.NULL);
            }

            if (downloadUrl != null) {
                json.put("download_url", downloadUrl);
            } else {
                json.put("download_url", JSONObject.NULL);
            }

            json.put("is_installed", isInstalled);

            return json;
        }
    }

    /**
     * API permission levels for key-based access control.
     */
    public enum APIPermission {
        READ,
        WRITE,
        ADMIN,
        WEBHOOKS,
        ANALYTICS,
        PLUGINS,
        BILLING
    }

    private DeveloperSDK() {
        throw new AssertionError("DeveloperSDK is a utility class and cannot be instantiated");
    }
}
