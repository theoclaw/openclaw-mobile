package ai.clawphones.agent.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenEconomyService {

    private static final String TAG = "TokenEconomyService";
    private static final String BASE_URL = "https://api.clawphones.ai/tokens";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String PREFS_DAILY_LOGIN_KEY = "last_daily_login_date";

    private static TokenEconomyService instance;

    private final OkHttpClient httpClient;
    private final SharedPreferences encryptedPrefs;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private TokenEconomyService(Context context) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            this.encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "token_economy_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create encrypted preferences", e);
        }

        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized TokenEconomyService getInstance(Context context) {
        if (instance == null) {
            instance = new TokenEconomyService(context.getApplicationContext());
        }
        return instance;
    }

    public interface TokenCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public void fetchWallet(TokenCallback<TokenEconomy.WalletBalance> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/wallet";
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch wallet: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.WalletBalance wallet = TokenEconomy.WalletBalance.fromJson(json);
                    notifySuccess(callback, wallet);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching wallet", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void fetchTransactions(int page, int pageSize, TokenEconomy.TransactionType typeFilter,
                                 TokenCallback<List<TokenEconomy.Transaction>> callback) {
        executorService.execute(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                        .append("/transactions?page=")
                        .append(page)
                        .append("&page_size=")
                        .append(pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE);

                if (typeFilter != null) {
                    urlBuilder.append("&type=").append(typeFilter.name());
                }

                String url = urlBuilder.toString();
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch transactions: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray transactionsArray = json.getJSONArray("transactions");

                    List<TokenEconomy.Transaction> transactions = new ArrayList<>();
                    for (int i = 0; i < transactionsArray.length(); i++) {
                        transactions.add(TokenEconomy.Transaction.fromJson(
                                transactionsArray.getJSONObject(i)));
                    }

                    notifySuccess(callback, transactions);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching transactions", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void transferCredits(String recipientUserId, double amount, String message,
                              TokenCallback<TokenEconomy.Transaction> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/transfer";
                String authToken = getAuthToken();

                JSONObject requestBody = new JSONObject();
                requestBody.put("recipient_user_id", recipientUserId);
                requestBody.put("amount", amount);
                if (message != null && !message.isEmpty()) {
                    requestBody.put("message", message);
                }

                RequestBody body = RequestBody.create(
                        requestBody.toString(),
                        MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        notifyError(callback, "Transfer failed: " + response.code() + " - " + errorBody);
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.Transaction transaction = TokenEconomy.Transaction.fromJson(
                            json.getJSONObject("transaction"));
                    notifySuccess(callback, transaction);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error transferring credits", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void claimReward(String ruleId, TokenCallback<TokenEconomy.Transaction> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/rewards/" + ruleId + "/claim";
                String authToken = getAuthToken();

                RequestBody emptyBody = RequestBody.create("", MediaType.parse("application/json"));
                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .post(emptyBody)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        notifyError(callback, "Claim reward failed: " + response.code() + " - " + errorBody);
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.Transaction transaction = TokenEconomy.Transaction.fromJson(
                            json.getJSONObject("transaction"));
                    notifySuccess(callback, transaction);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error claiming reward", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void fetchLeaderboard(String periodType, int limit,
                                 TokenCallback<TokenEconomy.Leaderboard> callback) {
        executorService.execute(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(BASE_URL)
                        .append("/leaderboard?period=")
                        .append(periodType != null ? periodType : "weekly")
                        .append("&limit=")
                        .append(limit > 0 ? limit : 50);

                String url = urlBuilder.toString();
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch leaderboard: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.Leaderboard leaderboard = TokenEconomy.Leaderboard.fromJson(json);
                    notifySuccess(callback, leaderboard);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching leaderboard", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void fetchRewardRules(TokenCallback<List<TokenEconomy.RewardRule>> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/rewards/rules";
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch reward rules: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray rulesArray = json.getJSONArray("rules");

                    List<TokenEconomy.RewardRule> rules = new ArrayList<>();
                    for (int i = 0; i < rulesArray.length(); i++) {
                        rules.add(TokenEconomy.RewardRule.fromJson(rulesArray.getJSONObject(i)));
                    }

                    notifySuccess(callback, rules);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching reward rules", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void checkAndClaimDailyLogin(TokenCallback<TokenEconomy.Transaction> callback) {
        executorService.execute(() -> {
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                String lastClaimDate = encryptedPrefs.getString(PREFS_DAILY_LOGIN_KEY, "");

                if (today.equals(lastClaimDate)) {
                    notifyError(callback, "Daily login reward already claimed today");
                    return;
                }

                fetchRewardRules(new TokenCallback<List<TokenEconomy.RewardRule>>() {
                    @Override
                    public void onSuccess(List<TokenEconomy.RewardRule> rules) {
                        TokenEconomy.RewardRule dailyRule = null;
                        for (TokenEconomy.RewardRule rule : rules) {
                            if (rule.getTriggerType() == TokenEconomy.TriggerType.DAILY_LOGIN &&
                                rule.canClaim()) {
                                dailyRule = rule;
                                break;
                            }
                        }

                        if (dailyRule == null) {
                            notifyError(callback, "Daily login reward not available");
                            return;
                        }

                        claimReward(dailyRule.getRuleId(), new TokenCallback<TokenEconomy.Transaction>() {
                            @Override
                            public void onSuccess(TokenEconomy.Transaction transaction) {
                                encryptedPrefs.edit()
                                        .putString(PREFS_DAILY_LOGIN_KEY, today)
                                        .apply();
                                notifySuccess(callback, transaction);
                            }

                            @Override
                            public void onError(String error) {
                                notifyError(callback, error);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        notifyError(callback, error);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error checking daily login", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void checkDailyLoginAvailability(TokenCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                String lastClaimDate = encryptedPrefs.getString(PREFS_DAILY_LOGIN_KEY, "");

                boolean available = !today.equals(lastClaimDate);
                notifySuccess(callback, available);
            } catch (Exception e) {
                Log.e(TAG, "Error checking daily login availability", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void fetchTransactionById(String transactionId,
                                    TokenCallback<TokenEconomy.Transaction> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/transactions/" + transactionId;
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch transaction: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.Transaction transaction = TokenEconomy.Transaction.fromJson(json);
                    notifySuccess(callback, transaction);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching transaction by ID", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    public void fetchUserBadge(String userId, TokenCallback<TokenEconomy.Badge> callback) {
        executorService.execute(() -> {
            try {
                String url = BASE_URL + "/users/" + userId + "/badge";
                String authToken = getAuthToken();

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        notifyError(callback, "Failed to fetch user badge: " + response.code());
                        return;
                    }

                    String responseBody = response.body().string();
                    JSONObject json = new JSONObject(responseBody);

                    TokenEconomy.Badge badge = TokenEconomy.Badge.valueOf(
                            json.optString("badge", "NEWCOMER"));
                    notifySuccess(callback, badge);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching user badge", e);
                notifyError(callback, "Error: " + e.getMessage());
            }
        });
    }

    private String getAuthToken() {
        return encryptedPrefs.getString("auth_token", "");
    }

    private <T> void notifySuccess(TokenCallback<T> callback, T result) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private <T> void notifyError(TokenCallback<T> callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
