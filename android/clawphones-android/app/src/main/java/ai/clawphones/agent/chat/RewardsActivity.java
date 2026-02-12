package ai.clawphones.agent.chat;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ai.clawphones.agent.R;

public class RewardsActivity extends AppCompatActivity {

    private static final int MAX_STREAK_DAYS = 7;
    private static final int DAILY_CLAIM_LIMIT = 3;

    private TextView tvStreakCount;
    private LinearLayout streakIndicatorsLayout;
    private MaterialCardView streakCard;
    private RecyclerView recyclerViewRewards;
    private TextView tvTotalRewards;
    private TextView tvTodayClaimed;
    private TextView tvDailyLimit;
    private ProgressBar progressBarDailyLimit;
    private TextView tvEmptyState;
    private ProgressBar progressBar;

    private RewardsAdapter rewardsAdapter;
    private List<Reward> rewards;

    private int currentStreak = 3;
    private int todayClaimed = 1;
    private CountDownTimer[] countdownTimers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

        setupToolbar();
        initViews();
        setupRecyclerView();
        loadRewardsData();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.rewards_title);
        }
    }

    private void initViews() {
        tvStreakCount = findViewById(R.id.tv_streak_count);
        streakIndicatorsLayout = findViewById(R.id.streak_indicators_layout);
        streakCard = findViewById(R.id.card_streak);
        recyclerViewRewards = findViewById(R.id.recycler_rewards);
        tvTotalRewards = findViewById(R.id.tv_total_rewards);
        tvTodayClaimed = findViewById(R.id.tv_today_claimed);
        tvDailyLimit = findViewById(R.id.tv_daily_limit);
        progressBarDailyLimit = findViewById(R.id.progress_daily_limit);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        progressBar = findViewById(R.id.progress_bar);

        updateStreakCard();
    }

    private void setupRecyclerView() {
        rewards = new ArrayList<>();
        rewardsAdapter = new RewardsAdapter(rewards, this::onClaimReward);
        recyclerViewRewards.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRewards.setAdapter(rewardsAdapter);
    }

    private void updateStreakCard() {
        tvStreakCount.setText(String.format(Locale.US, "%d", currentStreak));

        streakIndicatorsLayout.removeAllViews();

        for (int i = 0; i < MAX_STREAK_DAYS; i++) {
            View indicator = new View(this);
            int size = getResources().getDimensionPixelSize(R.dimen.streak_indicator_size);
            int margin = getResources().getDimensionPixelSize(R.dimen.streak_indicator_margin);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            indicator.setLayoutParams(params);

            if (i < currentStreak) {
                // Completed day
                indicator.setBackgroundResource(R.drawable.bg_streak_completed);
            } else {
                // Future day
                indicator.setBackgroundResource(R.drawable.bg_streak_pending);
            }

            streakIndicatorsLayout.addView(indicator);
        }
    }

    private void loadRewardsData() {
        showLoading(true);

        // Simulate API call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            rewards.clear();

            long now = System.currentTimeMillis();
            long oneHour = 60 * 60 * 1000L;
            long threeHours = 3 * oneHour;
            long sixHours = 6 * oneHour;

            rewards.add(new Reward(
                    "Daily Login Bonus",
                    "50 Credits",
                    50,
                    RewardType.DAILY,
                    now + oneHour,
                    true
            ));
            rewards.add(new Reward(
                    "Task Completion Bonus",
                    "100 Credits",
                    100,
                    RewardType.ACHIEVEMENT,
                    now + threeHours,
                    false
            ));
            rewards.add(new Reward(
                    "Weekend Multiplier",
                    "200 Credits",
                    200,
                    RewardType.SPECIAL,
                    now + sixHours,
                    true
            ));
            rewards.add(new Reward(
                    "Referral Bonus",
                    "150 Credits",
                    150,
                    RewardType.REFERRAL,
                    0,
                    false
            ));
            rewards.add(new Reward(
                    "Community Champion",
                    "300 Credits",
                    300,
                    RewardType.SPECIAL,
                    now + oneHour * 12,
                    false
            ));

            // Update summary
            tvTotalRewards.setText(getString(R.string.rewards_total_format, 800));
            tvTodayClaimed.setText(getString(R.string.rewards_today_claimed, todayClaimed, DAILY_CLAIM_LIMIT));
            tvDailyLimit.setText(String.format(Locale.US, "%d/%d", todayClaimed, DAILY_CLAIM_LIMIT));

            int progress = (int) ((double) todayClaimed / DAILY_CLAIM_LIMIT * 100);
            progressBarDailyLimit.setProgress(progress);

            rewardsAdapter.notifyDataSetChanged();
            updateEmptyState();
            showLoading(false);

            startCountdownTimers();
        }, 800);
    }

    private void startCountdownTimers() {
        if (countdownTimers != null) {
            for (CountDownTimer timer : countdownTimers) {
                if (timer != null) {
                    timer.cancel();
                }
            }
        }

        countdownTimers = new CountDownTimer[rewards.size()];

        for (int i = 0; i < rewards.size(); i++) {
            final Reward reward = rewards.get(i);
            final int position = i;

            if (reward.expiryTime > 0) {
                final ViewHolder holder = rewardsAdapter.getViewHolder(position);
                if (holder != null) {
                    countdownTimers[i] = new CountDownTimer(
                            reward.expiryTime - System.currentTimeMillis(),
                            1000
                    ) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            if (holder.tvCountdown != null) {
                                String countdown = formatCountdown(millisUntilFinished);
                                holder.tvCountdown.setText(getString(R.string.rewards_countdown, countdown));
                            }
                        }

                        @Override
                        public void onFinish() {
                            if (holder.tvCountdown != null) {
                                holder.tvCountdown.setText(R.string.rewards_expired);
                                holder.btnClaim.setEnabled(false);
                            }
                        }
                    };
                    countdownTimers[i].start();
                }
            }
        }
    }

    private String formatCountdown(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format(Locale.US, "%dh %02dm %02ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds % 60);
        } else {
            return String.format(Locale.US, "%ds", seconds);
        }
    }

    private void onClaimReward(Reward reward, int position, ViewHolder holder) {
        if (todayClaimed >= DAILY_CLAIM_LIMIT) {
            Toast.makeText(this, R.string.rewards_daily_limit_reached, Toast.LENGTH_SHORT).show();
            return;
        }

        if (reward.expiryTime > 0 && System.currentTimeMillis() >= reward.expiryTime) {
            Toast.makeText(this, R.string.rewards_expired, Toast.LENGTH_SHORT).show();
            return;
        }

        holder.progressBar.setVisibility(View.VISIBLE);
        holder.btnClaim.setEnabled(false);

        // Simulate API call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            holder.progressBar.setVisibility(View.GONE);
            holder.btnClaim.setEnabled(true);

            if (claimReward(reward)) {
                Toast.makeText(this, R.string.rewards_claim_success, Toast.LENGTH_SHORT).show();
                reward.claimed = true;
                rewardsAdapter.notifyItemChanged(position);

                // Update daily limit
                todayClaimed++;
                tvTodayClaimed.setText(getString(R.string.rewards_today_claimed, todayClaimed, DAILY_CLAIM_LIMIT));
                tvDailyLimit.setText(String.format(Locale.US, "%d/%d", todayClaimed, DAILY_CLAIM_LIMIT));
                int progress = (int) ((double) todayClaimed / DAILY_CLAIM_LIMIT * 100);
                progressBarDailyLimit.setProgress(progress);
            } else {
                Toast.makeText(this, R.string.rewards_claim_failed, Toast.LENGTH_SHORT).show();
            }
        }, 1000);
    }

    private boolean claimReward(Reward reward) {
        // TODO: Implement actual API call
        return true;
    }

    private void updateEmptyState() {
        if (rewards.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewRewards.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewRewards.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewRewards.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.GONE);
            streakCard.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            streakCard.setVisibility(View.VISIBLE);
            if (!rewards.isEmpty()) {
                recyclerViewRewards.setVisibility(View.VISIBLE);
            } else {
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownTimers != null) {
            for (CountDownTimer timer : countdownTimers) {
                if (timer != null) {
                    timer.cancel();
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    enum RewardType {
        DAILY, ACHIEVEMENT, SPECIAL, REFERRAL
    }

    static class Reward {
        String title;
        String amount;
        int value;
        RewardType type;
        long expiryTime;
        boolean claimed;

        Reward(String title, String amount, int value, RewardType type, long expiryTime, boolean claimed) {
            this.title = title;
            this.amount = amount;
            this.value = value;
            this.type = type;
            this.expiryTime = expiryTime;
            this.claimed = claimed;
        }
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvAmount;
        TextView tvCountdown;
        MaterialButton btnClaim;
        ProgressBar progressBar;
        ImageView ivType;
    }

    static class RewardsAdapter extends RecyclerView.Adapter<RewardsAdapter.ViewHolder> {
        private final List<Reward> rewards;
        private final OnClaimListener onClaimListener;
        private final List<ViewHolder> viewHolders = new ArrayList<>();

        RewardsAdapter(List<Reward> rewards, OnClaimListener onClaimListener) {
            this.rewards = rewards;
            this.onClaimListener = onClaimListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reward, parent, false);
            ViewHolder holder = new ViewHolder(view);
            viewHolders.add(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Reward reward = rewards.get(position);

            holder.tvTitle.setText(reward.title);
            holder.tvAmount.setText(reward.amount);

            // Set icon based on type
            switch (reward.type) {
                case DAILY:
                    holder.ivType.setImageResource(R.drawable.ic_reward_daily);
                    break;
                case ACHIEVEMENT:
                    holder.ivType.setImageResource(R.drawable.ic_reward_achievement);
                    break;
                case SPECIAL:
                    holder.ivType.setImageResource(R.drawable.ic_reward_special);
                    break;
                case REFERRAL:
                    holder.ivType.setImageResource(R.drawable.ic_reward_referral);
                    break;
            }

            // Setup countdown
            if (reward.expiryTime > 0 && !reward.claimed) {
                long remaining = reward.expiryTime - System.currentTimeMillis();
                if (remaining > 0) {
                    String countdown = formatCountdown(remaining);
                    holder.tvCountdown.setText(holder.itemView.getContext().getString(R.string.rewards_countdown, countdown));
                    holder.tvCountdown.setVisibility(View.VISIBLE);
                } else {
                    holder.tvCountdown.setText(R.string.rewards_expired);
                    holder.tvCountdown.setVisibility(View.VISIBLE);
                    holder.btnClaim.setEnabled(false);
                }
            } else {
                holder.tvCountdown.setVisibility(View.GONE);
            }

            // Setup claim button
            if (reward.claimed) {
                holder.btnClaim.setText(R.string.rewards_claimed);
                holder.btnClaim.setEnabled(false);
            } else {
                holder.btnClaim.setText(R.string.rewards_claim);
                holder.btnClaim.setEnabled(true);
                holder.btnClaim.setOnClickListener(v -> onClaimListener.onClaim(reward, position, holder));
            }

            holder.progressBar.setVisibility(View.GONE);
        }

        private String formatCountdown(long millis) {
            long seconds = millis / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (hours > 0) {
                return String.format(java.util.Locale.US, "%dh %02dm", hours, minutes % 60);
            } else if (minutes > 0) {
                return String.format(java.util.Locale.US, "%dm %02ds", minutes, seconds % 60);
            } else {
                return String.format(java.util.Locale.US, "%ds", seconds);
            }
        }

        @Override
        public int getItemCount() {
            return rewards.size();
        }

        ViewHolder getViewHolder(int position) {
            if (position >= 0 && position < viewHolders.size()) {
                return viewHolders.get(position);
            }
            return null;
        }

        interface OnClaimListener {
            void onClaim(Reward reward, int position, ViewHolder holder);
        }
    }
}
