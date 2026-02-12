package ai.clawphones.agent.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ai.clawphones.agent.R;

public class LeaderboardActivity extends AppCompatActivity {

    private static final int PERIOD_WEEKLY = 0;
    private static final int PERIOD_MONTHLY = 1;
    private static final int PERIOD_ALL_TIME = 2;

    private Spinner spinnerPeriod;
    private TextView tvPodiumFirstRank;
    private TextView tvPodiumFirstName;
    private TextView tvPodiumFirstScore;
    private ImageView ivPodiumFirstAvatar;
    private MaterialCardView cardFirst;

    private TextView tvPodiumSecondRank;
    private TextView tvPodiumSecondName;
    private TextView tvPodiumSecondScore;
    private ImageView ivPodiumSecondAvatar;
    private MaterialCardView cardSecond;

    private TextView tvPodiumThirdRank;
    private TextView tvPodiumThirdName;
    private TextView tvPodiumThirdScore;
    private ImageView ivPodiumThirdAvatar;
    private MaterialCardView cardThird;

    private RecyclerView recyclerViewLeaderboard;
    private MaterialCardView cardMyRank;
    private TextView tvMyRank;
    private TextView tvMyName;
    private TextView tvMyScore;
    private ImageView ivMyAvatar;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    private LeaderboardAdapter leaderboardAdapter;
    private List<LeaderboardEntry> leaderboardEntries;
    private LeaderboardEntry myEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        setupToolbar();
        initViews();
        setupRecyclerView();
        setupPeriodSpinner();
        loadLeaderboardData();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.leaderboard_title);
        }
    }

    private void initViews() {
        spinnerPeriod = findViewById(R.id.spinner_period);

        // Podium first (center, highest)
        tvPodiumFirstRank = findViewById(R.id.tv_podium_first_rank);
        tvPodiumFirstName = findViewById(R.id.tv_podium_first_name);
        tvPodiumFirstScore = findViewById(R.id.tv_podium_first_score);
        ivPodiumFirstAvatar = findViewById(R.id.iv_podium_first_avatar);
        cardFirst = findViewById(R.id.card_podium_first);

        // Podium second (left)
        tvPodiumSecondRank = findViewById(R.id.tv_podium_second_rank);
        tvPodiumSecondName = findViewById(R.id.tv_podium_second_name);
        tvPodiumSecondScore = findViewById(R.id.tv_podium_second_score);
        ivPodiumSecondAvatar = findViewById(R.id.iv_podium_second_avatar);
        cardSecond = findViewById(R.id.card_podium_second);

        // Podium third (right)
        tvPodiumThirdRank = findViewById(R.id.tv_podium_third_rank);
        tvPodiumThirdName = findViewById(R.id.tv_podium_third_name);
        tvPodiumThirdScore = findViewById(R.id.tv_podium_third_score);
        ivPodiumThirdAvatar = findViewById(R.id.iv_podium_third_avatar);
        cardThird = findViewById(R.id.card_podium_third);

        recyclerViewLeaderboard = findViewById(R.id.recycler_leaderboard);

        // My rank card (pinned at bottom)
        cardMyRank = findViewById(R.id.card_my_rank);
        tvMyRank = findViewById(R.id.tv_my_rank);
        tvMyName = findViewById(R.id.tv_my_name);
        tvMyScore = findViewById(R.id.tv_my_score);
        ivMyAvatar = findViewById(R.id.iv_my_avatar);

        progressBar = findViewById(R.id.progress_bar);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupRecyclerView() {
        leaderboardEntries = new ArrayList<>();
        leaderboardAdapter = new LeaderboardAdapter(leaderboardEntries);
        recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewLeaderboard.setAdapter(leaderboardAdapter);
    }

    private void setupPeriodSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.leaderboard_period_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadLeaderboardData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadLeaderboardData() {
        showLoading(true);

        // Simulate API call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            leaderboardEntries.clear();

            // Generate sample leaderboard data
            leaderboardEntries.add(new LeaderboardEntry(1, "Alice", 15420, true, "https://example.com/avatar1.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(2, "Bob", 12850, false, "https://example.com/avatar2.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(3, "Charlie", 11300, false, "https://example.com/avatar3.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(4, "Diana", 9875, false, "https://example.com/avatar4.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(5, "Eve", 8640, false, "https://example.com/avatar5.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(6, "Frank", 7920, false, "https://example.com/avatar6.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(7, "Grace", 6850, false, "https://example.com/avatar7.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(8, "Henry", 5430, false, "https://example.com/avatar8.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(9, "Ivy", 4890, false, "https://example.com/avatar9.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(10, "Jack", 4250, false, "https://example.com/avatar10.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(11, "Kate", 3870, false, "https://example.com/avatar11.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(12, "Leo", 3560, false, "https://example.com/avatar12.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(13, "Mia", 3240, false, "https://example.com/avatar13.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(14, "Noah", 2980, false, "https://example.com/avatar14.jpg"));
            leaderboardEntries.add(new LeaderboardEntry(15, "Olivia", 2750, false, "https://example.com/avatar15.jpg"));

            // Create my entry
            myEntry = new LeaderboardEntry(7, "You", 6850, true, "");
            leaderboardEntries.add(6, myEntry);

            updatePodium();
            updateMyRankCard();
            updateRankedList();

            showLoading(false);
        }, 1000);
    }

    private void updatePodium() {
        if (leaderboardEntries.size() >= 3) {
            // First place (center, highest)
            LeaderboardEntry first = leaderboardEntries.get(0);
            tvPodiumFirstRank.setText(getString(R.string.leaderboard_rank_format, first.rank));
            tvPodiumFirstName.setText(first.name);
            tvPodiumFirstScore.setText(getString(R.string.leaderboard_score_format, first.score));
            cardFirst.setCardBackgroundColor(getColor(R.color.podium_first_bg));

            // Second place (left)
            LeaderboardEntry second = leaderboardEntries.get(1);
            tvPodiumSecondRank.setText(getString(R.string.leaderboard_rank_format, second.rank));
            tvPodiumSecondName.setText(second.name);
            tvPodiumSecondScore.setText(getString(R.string.leaderboard_score_format, second.score));
            cardSecond.setCardBackgroundColor(getColor(R.color.podium_second_bg));

            // Third place (right)
            LeaderboardEntry third = leaderboardEntries.get(2);
            tvPodiumThirdRank.setText(getString(R.string.leaderboard_rank_format, third.rank));
            tvPodiumThirdName.setText(third.name);
            tvPodiumThirdScore.setText(getString(R.string.leaderboard_score_format, third.score));
            cardThird.setCardBackgroundColor(getColor(R.color.podium_third_bg));
        }
    }

    private void updateMyRankCard() {
        if (myEntry != null) {
            tvMyRank.setText(getString(R.string.leaderboard_rank_format, myEntry.rank));
            tvMyName.setText(myEntry.name);
            tvMyScore.setText(getString(R.string.leaderboard_score_format, myEntry.score));

            if (myEntry.rank <= 3) {
                // Highlight if in top 3
                cardMyRank.setCardBackgroundColor(getColor(R.color.podium_first_bg));
            }
        }
    }

    private void updateRankedList() {
        // Create list excluding top 3
        List<LeaderboardEntry> rankedList = new ArrayList<>();
        for (int i = 3; i < leaderboardEntries.size(); i++) {
            rankedList.add(leaderboardEntries.get(i));
        }

        leaderboardAdapter.setData(rankedList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (leaderboardAdapter.getItemCount() == 0) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewLeaderboard.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewLeaderboard.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewLeaderboard.setVisibility(View.GONE);
            cardFirst.setVisibility(View.GONE);
            cardSecond.setVisibility(View.GONE);
            cardThird.setVisibility(View.GONE);
            cardMyRank.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            cardFirst.setVisibility(View.VISIBLE);
            cardSecond.setVisibility(View.VISIBLE);
            cardThird.setVisibility(View.VISIBLE);
            cardMyRank.setVisibility(View.VISIBLE);
            updateEmptyState();
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

    static class LeaderboardEntry {
        int rank;
        String name;
        int score;
        boolean isCurrentUser;
        String avatarUrl;

        LeaderboardEntry(int rank, String name, int score, boolean isCurrentUser, String avatarUrl) {
            this.rank = rank;
            this.name = name;
            this.score = score;
            this.isCurrentUser = isCurrentUser;
            this.avatarUrl = avatarUrl;
        }
    }

    static class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
        private List<LeaderboardEntry> entries;

        LeaderboardAdapter(List<LeaderboardEntry> entries) {
            this.entries = entries;
        }

        void setData(List<LeaderboardEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_leaderboard_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            LeaderboardEntry entry = entries.get(position);

            holder.tvRank.setText(String.valueOf(entry.rank));
            holder.tvName.setText(entry.name);
            holder.tvScore.setText(holder.itemView.getContext()
                    .getString(R.string.leaderboard_score_format, entry.score));

            // Highlight current user
            if (entry.isCurrentUser) {
                holder.cardEntry.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.leaderboard_highlight));
                holder.tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            } else {
                holder.cardEntry.setCardBackgroundColor(
                        holder.itemView.getContext().getColor(R.color.card_background));
                holder.tvName.setTypeface(android.graphics.Typeface.DEFAULT);
            }

            // Set rank badge color
            switch (entry.rank) {
                case 1:
                    holder.ivRankBadge.setImageResource(R.drawable.ic_rank_gold);
                    break;
                case 2:
                    holder.ivRankBadge.setImageResource(R.drawable.ic_rank_silver);
                    break;
                case 3:
                    holder.ivRankBadge.setImageResource(R.drawable.ic_rank_bronze);
                    break;
                default:
                    holder.ivRankBadge.setImageResource(R.drawable.ic_rank_default);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank;
            TextView tvName;
            TextView tvScore;
            ImageView ivRankBadge;
            ImageView ivAvatar;
            MaterialCardView cardEntry;

            ViewHolder(View itemView) {
                super(itemView);
                cardEntry = itemView.findViewById(R.id.card_leaderboard_entry);
                ivRankBadge = itemView.findViewById(R.id.iv_rank_badge);
                ivAvatar = itemView.findViewById(R.id.iv_avatar);
                tvRank = itemView.findViewById(R.id.tv_rank);
                tvName = itemView.findViewById(R.id.tv_name);
                tvScore = itemView.findViewById(R.id.tv_score);
            }
        }
    }
}
