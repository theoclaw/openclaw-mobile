package ai.clawphones.agent.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import ai.clawphones.agent.R;

public class WalletActivity extends AppCompatActivity {

    private TextView tvAvailableBalance;
    private TextView tvPendingBalance;
    private MaterialButton btnSendCredits;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerViewTransactions;
    private Spinner spinnerFilter;
    private TextView tvFilterLabel;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private TextView tvEmptyStateText;

    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactions;
    private List<Transaction> filteredTransactions;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_EARNED = 1;
    private static final int FILTER_SPENT = 2;
    private static final int FILTER_TRANSFERS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        setupToolbar();
        initViews();
        setupRecyclerView();
        setupSwipeRefresh();
        setupSendButton();
        loadWalletData();
    }

    private void setupToolbar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.wallet_title);
        }
    }

    private void initViews() {
        tvAvailableBalance = findViewById(R.id.tv_available_balance);
        tvPendingBalance = findViewById(R.id.tv_pending_balance);
        btnSendCredits = findViewById(R.id.btn_send_credits);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerViewTransactions = findViewById(R.id.recycler_transactions);
        spinnerFilter = findViewById(R.id.spinner_filter);
        tvFilterLabel = findViewById(R.id.tv_filter_label);
        progressBar = findViewById(R.id.progress_bar);
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        tvEmptyStateText = findViewById(R.id.tv_empty_state_text);

        setupFilterSpinner();
    }

    private void setupFilterSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.wallet_filter_options));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterTransactions(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        transactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        transactionAdapter = new TransactionAdapter(filteredTransactions);
        recyclerViewTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTransactions.setAdapter(transactionAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(R.color.primary_color);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadWalletData();
        });
    }

    private void setupSendButton() {
        btnSendCredits.setOnClickListener(v -> showSendCreditsDialog());
    }

    private void showSendCreditsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_send_credits, null);
        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etRecipient = dialogView.findViewById(R.id.et_recipient);
        ProgressBar progressBarDialog = dialogView.findViewById(R.id.progress_bar_dialog);
        MaterialButton btnSend = dialogView.findViewById(R.id.btn_send);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_send_credits_title)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String recipient = etRecipient.getText().toString().trim();

            if (TextUtils.isEmpty(amountStr)) {
                etAmount.setError(getString(R.string.error_amount_required));
                return;
            }

            if (TextUtils.isEmpty(recipient)) {
                etRecipient.setError(getString(R.string.error_recipient_required));
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    etAmount.setError(getString(R.string.error_invalid_amount));
                    return;
                }

                // Check available balance
                double availableBalance = parseBalance(tvAvailableBalance.getText().toString());
                if (amount > availableBalance) {
                    etAmount.setError(getString(R.string.error_insufficient_balance));
                    return;
                }

                // Show loading
                progressBarDialog.setVisibility(View.VISIBLE);
                btnSend.setEnabled(false);
                btnCancel.setEnabled(false);

                // Simulate sending
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    progressBarDialog.setVisibility(View.GONE);
                    btnSend.setEnabled(true);
                    btnCancel.setEnabled(true);

                    if (sendCredits(recipient, amount)) {
                        Toast.makeText(this, R.string.toast_credits_sent, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadWalletData();
                    } else {
                        Toast.makeText(this, R.string.toast_send_failed, Toast.LENGTH_SHORT).show();
                    }
                }, 1500);

            } catch (NumberFormatException e) {
                etAmount.setError(getString(R.string.error_invalid_amount));
            }
        });

        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private double parseBalance(String balanceStr) {
        try {
            return Double.parseDouble(balanceStr.replace("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean sendCredits(String recipient, double amount) {
        // TODO: Implement actual API call
        return true;
    }

    private void loadWalletData() {
        showLoading(true);

        // Simulate API call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Update balances
            tvAvailableBalance.setText(getString(R.string.wallet_balance_format, 1250.50));
            tvPendingBalance.setText(getString(R.string.wallet_balance_format, 45.00));

            // Load transactions
            transactions.clear();
            transactions.add(new Transaction("Credit Earned", "+50.00", "Photo Survey Task", "2024-01-15 10:30", TransactionType.EARNED));
            transactions.add(new Transaction("Credit Spent", "-20.00", "Premium Feature", "2024-01-15 09:15", TransactionType.SPENT));
            transactions.add(new Transaction("Transfer Received", "+100.00", "From: user123", "2024-01-14 16:45", TransactionType.TRANSFER));
            transactions.add(new Transaction("Credit Earned", "+75.00", "Monitoring Task", "2024-01-14 14:20", TransactionType.EARNED));
            transactions.add(new Transaction("Transfer Sent", "-50.00", "To: user456", "2024-01-13 11:30", TransactionType.TRANSFER));
            transactions.add(new Transaction("Credit Spent", "-30.00", "Data Export", "2024-01-13 09:00", TransactionType.SPENT));
            transactions.add(new Transaction("Credit Earned", "+60.00", "Environmental Task", "2024-01-12 15:45", TransactionType.EARNED));

            // Apply current filter
            filterTransactions(spinnerFilter.getSelectedItemPosition());

            showLoading(false);
            swipeRefreshLayout.setRefreshing(false);
        }, 1000);
    }

    private void filterTransactions(int filterType) {
        filteredTransactions.clear();

        for (Transaction transaction : transactions) {
            switch (filterType) {
                case FILTER_ALL:
                    filteredTransactions.add(transaction);
                    break;
                case FILTER_EARNED:
                    if (transaction.type == TransactionType.EARNED) {
                        filteredTransactions.add(transaction);
                    }
                    break;
                case FILTER_SPENT:
                    if (transaction.type == TransactionType.SPENT) {
                        filteredTransactions.add(transaction);
                    }
                    break;
                case FILTER_TRANSFERS:
                    if (transaction.type == TransactionType.TRANSFER) {
                        filteredTransactions.add(transaction);
                    }
                    break;
            }
        }

        transactionAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredTransactions.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            tvEmptyStateText.setText(R.string.wallet_empty_transactions);
            recyclerViewTransactions.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerViewTransactions.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewTransactions.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            if (!filteredTransactions.isEmpty()) {
                recyclerViewTransactions.setVisibility(View.VISIBLE);
            } else {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
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

    enum TransactionType {
        EARNED, SPENT, TRANSFER
    }

    static class Transaction {
        String title;
        String amount;
        String description;
        String timestamp;
        TransactionType type;

        Transaction(String title, String amount, String description, String timestamp, TransactionType type) {
            this.title = title;
            this.amount = amount;
            this.description = description;
            this.timestamp = timestamp;
            this.type = type;
        }
    }

    static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<Transaction> transactions;

        TransactionAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Transaction transaction = transactions.get(position);

            holder.tvTitle.setText(transaction.title);
            holder.tvAmount.setText(transaction.amount);
            holder.tvDescription.setText(transaction.description);
            holder.tvTimestamp.setText(transaction.timestamp);

            // Set amount color based on positive/negative
            if (transaction.amount.startsWith("+")) {
                holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green));
            } else {
                holder.tvAmount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.red));
            }

            // Set icon based on type
            switch (transaction.type) {
                case EARNED:
                    holder.ivIcon.setImageResource(R.drawable.ic_earned);
                    break;
                case SPENT:
                    holder.ivIcon.setImageResource(R.drawable.ic_spent);
                    break;
                case TRANSFER:
                    holder.ivIcon.setImageResource(R.drawable.ic_transfer);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvAmount;
            TextView tvDescription;
            TextView tvTimestamp;
            ImageView ivIcon;
            MaterialCardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_transaction);
                ivIcon = itemView.findViewById(R.id.iv_icon);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvAmount = itemView.findViewById(R.id.tv_amount);
                tvDescription = itemView.findViewById(R.id.tv_description);
                tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            }
        }
    }
}
