package ai.clawphones.agent.chat;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

public class TokenEconomy {

    public enum Badge implements Serializable {
        NEWCOMER("新晋成员", 0),
        CONTRIBUTOR("贡献者", 100),
        GUARDIAN("守护者", 500),
        POWER_NODE("能量节点", 1000),
        COMMUNITY_LEADER("社区领袖", 2500),
        TOP_EARNER("顶级贡献者", 5000);

        private final String displayName;
        private final int threshold;

        Badge(String displayName, int threshold) {
            this.displayName = displayName;
            this.threshold = threshold;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getThreshold() {
            return threshold;
        }

        public static Badge fromTotalCredits(int totalCredits) {
            Badge highest = NEWCOMER;
            for (Badge badge : values()) {
                if (totalCredits >= badge.threshold) {
                    highest = badge;
                }
            }
            return highest;
        }
    }

    public static class WalletBalance implements Serializable {
        private final double totalCredits;
        private final double availableCredits;
        private final double lockedCredits;
        private final Badge currentBadge;
        private final int nextBadgeProgress;
        private final int nextBadgeThreshold;

        public WalletBalance(double totalCredits, double availableCredits, double lockedCredits,
                            Badge currentBadge, int nextBadgeProgress, int nextBadgeThreshold) {
            this.totalCredits = totalCredits;
            this.availableCredits = availableCredits;
            this.lockedCredits = lockedCredits;
            this.currentBadge = currentBadge;
            this.nextBadgeProgress = nextBadgeProgress;
            this.nextBadgeThreshold = nextBadgeThreshold;
        }

        public double getTotalCredits() {
            return totalCredits;
        }

        public double getAvailableCredits() {
            return availableCredits;
        }

        public double getLockedCredits() {
            return lockedCredits;
        }

        public Badge getCurrentBadge() {
            return currentBadge;
        }

        public int getNextBadgeProgress() {
            return nextBadgeProgress;
        }

        public int getNextBadgeThreshold() {
            return nextBadgeThreshold;
        }

        public static WalletBalance fromJson(JSONObject json) {
            double totalCredits = json.optDouble("total_credits", 0);
            double availableCredits = json.optDouble("available_credits", 0);
            double lockedCredits = json.optDouble("locked_credits", 0);

            Badge currentBadge = Badge.valueOf(json.optString("current_badge", "NEWCOMER"));
            int nextBadgeProgress = json.optInt("next_badge_progress", 0);
            int nextBadgeThreshold = json.optInt("next_badge_threshold", 0);

            return new WalletBalance(totalCredits, availableCredits, lockedCredits,
                                   currentBadge, nextBadgeProgress, nextBadgeThreshold);
        }
    }

    public enum TransactionType {
        EARNED,
        SPENT,
        TRANSFER_IN,
        TRANSFER_OUT,
        REWARD_CLAIMED,
        STAKE_LOCKED,
        STAKE_UNLOCKED,
        REFUND
    }

    public static class Transaction implements Serializable {
        private final String transactionId;
        private final TransactionType type;
        private final double amount;
        private final double balanceAfter;
        private final String description;
        private final Date createdAt;
        private final String relatedTaskId;
        private final String fromUserId;
        private final String toUserId;

        public Transaction(String transactionId, TransactionType type, double amount,
                          double balanceAfter, String description, Date createdAt,
                          String relatedTaskId, String fromUserId, String toUserId) {
            this.transactionId = transactionId;
            this.type = type;
            this.amount = amount;
            this.balanceAfter = balanceAfter;
            this.description = description;
            this.createdAt = createdAt;
            this.relatedTaskId = relatedTaskId;
            this.fromUserId = fromUserId;
            this.toUserId = toUserId;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public TransactionType getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }

        public double getBalanceAfter() {
            return balanceAfter;
        }

        public String getDescription() {
            return description;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public String getRelatedTaskId() {
            return relatedTaskId;
        }

        public String getFromUserId() {
            return fromUserId;
        }

        public String getToUserId() {
            return toUserId;
        }

        public static Transaction fromJson(JSONObject json) {
            String transactionId = json.optString("transaction_id", "");
            TransactionType type = TransactionType.valueOf(json.optString("type", "EARNED"));
            double amount = json.optDouble("amount", 0);
            double balanceAfter = json.optDouble("balance_after", 0);
            String description = json.optString("description", "");
            long createdAtMs = json.optLong("created_at", System.currentTimeMillis());
            String relatedTaskId = json.optString("related_task_id", null);
            String fromUserId = json.optString("from_user_id", null);
            String toUserId = json.optString("to_user_id", null);

            return new Transaction(transactionId, type, amount, balanceAfter, description,
                                  new Date(createdAtMs), relatedTaskId, fromUserId, toUserId);
        }
    }

    public enum TriggerType {
        DAILY_LOGIN,
        TASK_COMPLETED,
        STREAK_MILESTONE,
        REFERRAL_COMPLETED,
        COMMUNITY_CONTRIBUTION,
        POWER_NODE_HOURS,
        WEEKLY_LEADERBOARD_TOP_10
    }

    public static class RewardRule implements Serializable {
        private final String ruleId;
        private final String name;
        private final String description;
        private final TriggerType triggerType;
        private final double creditReward;
        private final boolean isActive;
        private final int maxClaimsPerDay;
        private final int claimsToday;
        private final Date nextClaimAt;

        public RewardRule(String ruleId, String name, String description, TriggerType triggerType,
                         double creditReward, boolean isActive, int maxClaimsPerDay,
                         int claimsToday, Date nextClaimAt) {
            this.ruleId = ruleId;
            this.name = name;
            this.description = description;
            this.triggerType = triggerType;
            this.creditReward = creditReward;
            this.isActive = isActive;
            this.maxClaimsPerDay = maxClaimsPerDay;
            this.claimsToday = claimsToday;
            this.nextClaimAt = nextClaimAt;
        }

        public String getRuleId() {
            return ruleId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public TriggerType getTriggerType() {
            return triggerType;
        }

        public double getCreditReward() {
            return creditReward;
        }

        public boolean isActive() {
            return isActive;
        }

        public int getMaxClaimsPerDay() {
            return maxClaimsPerDay;
        }

        public int getClaimsToday() {
            return claimsToday;
        }

        public Date getNextClaimAt() {
            return nextClaimAt;
        }

        public boolean canClaim() {
            return isActive && claimsToday < maxClaimsPerDay &&
                   (nextClaimAt == null || nextClaimAt.before(new Date()));
        }

        public static RewardRule fromJson(JSONObject json) {
            String ruleId = json.optString("rule_id", "");
            String name = json.optString("name", "");
            String description = json.optString("description", "");
            TriggerType triggerType = TriggerType.valueOf(json.optString("trigger_type", "DAILY_LOGIN"));
            double creditReward = json.optDouble("credit_reward", 0);
            boolean isActive = json.optBoolean("is_active", true);
            int maxClaimsPerDay = json.optInt("max_claims_per_day", 1);
            int claimsToday = json.optInt("claims_today", 0);
            long nextClaimAtMs = json.optLong("next_claim_at", 0);
            Date nextClaimAt = nextClaimAtMs > 0 ? new Date(nextClaimAtMs) : null;

            return new RewardRule(ruleId, name, description, triggerType, creditReward,
                                isActive, maxClaimsPerDay, claimsToday, nextClaimAt);
        }
    }

    public static class Leaderboard implements Serializable {
        private final String leaderboardId;
        private final String name;
        private final String periodType;
        private final Date periodStart;
        private final Date periodEnd;
        private final int totalCount;
        private final List<LeaderboardEntry> entries;

        public Leaderboard(String leaderboardId, String name, String periodType,
                          Date periodStart, Date periodEnd, int totalCount,
                          List<LeaderboardEntry> entries) {
            this.leaderboardId = leaderboardId;
            this.name = name;
            this.periodType = periodType;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.totalCount = totalCount;
            this.entries = entries;
        }

        public String getLeaderboardId() {
            return leaderboardId;
        }

        public String getName() {
            return name;
        }

        public String getPeriodType() {
            return periodType;
        }

        public Date getPeriodStart() {
            return periodStart;
        }

        public Date getPeriodEnd() {
            return periodEnd;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public List<LeaderboardEntry> getEntries() {
            return entries;
        }

        public LeaderboardEntry getCurrentUserEntry(String userId) {
            for (LeaderboardEntry entry : entries) {
                if (entry.getUserId().equals(userId)) {
                    return entry;
                }
            }
            return null;
        }

        public static Leaderboard fromJson(JSONObject json) {
            String leaderboardId = json.optString("leaderboard_id", "");
            String name = json.optString("name", "");
            String periodType = json.optString("period_type", "weekly");
            long periodStartMs = json.optLong("period_start", 0);
            long periodEndMs = json.optLong("period_end", 0);
            Date periodStart = periodStartMs > 0 ? new Date(periodStartMs) : null;
            Date periodEnd = periodEndMs > 0 ? new Date(periodEndMs) : null;
            int totalCount = json.optInt("total_count", 0);

            org.json.JSONArray entriesArray = json.optJSONArray("entries");
            List<LeaderboardEntry> entries = new ArrayList<>();
            if (entriesArray != null) {
                for (int i = 0; i < entriesArray.length(); i++) {
                    entries.add(LeaderboardEntry.fromJson(entriesArray.getJSONObject(i)));
                }
            }

            return new Leaderboard(leaderboardId, name, periodType, periodStart, periodEnd,
                                  totalCount, entries);
        }
    }

    public static class LeaderboardEntry implements Serializable {
        private final String userId;
        private final String username;
        private final String avatarUrl;
        private final int rank;
        private final double totalCredits;
        private final int tasksCompleted;
        private final Badge currentBadge;
        private final boolean isCurrentUser;

        public LeaderboardEntry(String userId, String username, String avatarUrl,
                              int rank, double totalCredits, int tasksCompleted,
                              Badge currentBadge, boolean isCurrentUser) {
            this.userId = userId;
            this.username = username;
            this.avatarUrl = avatarUrl;
            this.rank = rank;
            this.totalCredits = totalCredits;
            this.tasksCompleted = tasksCompleted;
            this.currentBadge = currentBadge;
            this.isCurrentUser = isCurrentUser;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public int getRank() {
            return rank;
        }

        public double getTotalCredits() {
            return totalCredits;
        }

        public int getTasksCompleted() {
            return tasksCompleted;
        }

        public Badge getCurrentBadge() {
            return currentBadge;
        }

        public boolean isCurrentUser() {
            return isCurrentUser;
        }

        public static LeaderboardEntry fromJson(JSONObject json) {
            String userId = json.optString("user_id", "");
            String username = json.optString("username", "");
            String avatarUrl = json.optString("avatar_url", null);
            int rank = json.optInt("rank", 0);
            double totalCredits = json.optDouble("total_credits", 0);
            int tasksCompleted = json.optInt("tasks_completed", 0);
            Badge currentBadge = Badge.valueOf(json.optString("current_badge", "NEWCOMER"));
            boolean isCurrentUser = json.optBoolean("is_current_user", false);

            return new LeaderboardEntry(userId, username, avatarUrl, rank, totalCredits,
                                       tasksCompleted, currentBadge, isCurrentUser);
        }
    }
}
