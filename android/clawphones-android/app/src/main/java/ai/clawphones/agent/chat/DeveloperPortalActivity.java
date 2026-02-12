package ai.clawphones.agent.chat;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class DeveloperPortalActivity extends AppCompatActivity {

    private static final int TAB_API_KEYS = 0;
    private static final int TAB_WEBHOOKS = 1;
    private static final int TAB_PLUGINS = 2;

    private TabLayout tabLayout;
    private androidx.viewpager2.widget.ViewPager2 viewPager;
    private DeveloperPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_portal);

        setupToolbar();
        setupViewPager();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.developer_portal_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        pagerAdapter = new DeveloperPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case TAB_API_KEYS:
                    tab.setText(R.string.tab_api_keys);
                    break;
                case TAB_WEBHOOKS:
                    tab.setText(R.string.tab_webhooks);
                    break;
                case TAB_PLUGINS:
                    tab.setText(R.string.tab_plugins);
                    break;
            }
        }).attach();
    }

    // API Key Fragment
    public static class ApiKeyFragment extends androidx.fragment.app.Fragment {
        private ApiKeyAdapter adapter;
        private List<ApiKey> apiKeys = new ArrayList<>();

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_api_keys, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.api_key_recycler);
            FloatingActionButton fab = view.findViewById(R.id.fab_generate_key);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new ApiKeyAdapter(apiKeys, this::onCopyKey, this::onRevokeKey);
            recyclerView.setAdapter(adapter);

            fab.setOnClickListener(v -> showGenerateKeyDialog());

            loadApiKeys();
            return view;
        }

        private void showGenerateKeyDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_generate_key, null);
            EditText nameInput = dialogView.findViewById(R.id.key_name_input);

            builder.setView(dialogView)
                    .setTitle(R.string.dialog_generate_key_title)
                    .setPositiveButton(R.string.button_generate, (dialog, which) -> {
                        String name = nameInput.getText().toString().trim();
                        if (name.isEmpty()) {
                            name = getString(R.string.default_key_name);
                        }
                        generateApiKey(name);
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show();
        }

        private void generateApiKey(String name) {
            String key = "cp_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
            ApiKey newKey = new ApiKey(name, key, "Active");
            apiKeys.add(newKey);
            adapter.notifyItemInserted(apiKeys.size() - 1);
            Toast.makeText(requireContext(), R.string.key_generated, Toast.LENGTH_SHORT).show();
        }

        private void loadApiKeys() {
            apiKeys.clear();
            apiKeys.add(new ApiKey("Production", "cp_prod_abc123xyz", "Active"));
            apiKeys.add(new ApiKey("Testing", "cp_test_def456uvw", "Active"));
            apiKeys.add(new ApiKey("Old Key", "cp_old_ghi789rst", "Revoked"));
            adapter.notifyDataSetChanged();
        }

        private void onCopyKey(String key) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("API Key", key);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), R.string.key_copied, Toast.LENGTH_SHORT).show();
        }

        private void onRevokeKey(int position) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_revoke_key_title)
                    .setMessage(R.string.dialog_revoke_key_message)
                    .setPositiveButton(R.string.button_revoke, (dialog, which) -> {
                        apiKeys.get(position).status = "Revoked";
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), R.string.key_revoked, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show();
        }
    }

    // Webhook Fragment
    public static class WebhookFragment extends androidx.fragment.app.Fragment {
        private WebhookAdapter adapter;
        private List<Webhook> webhooks = new ArrayList<>();

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_webhooks, container, false);
            RecyclerView recyclerView = view.findViewById(R.id.webhook_recycler);
            FloatingActionButton fab = view.findViewById(R.id.fab_add_webhook);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new WebhookAdapter(webhooks, this::onTestWebhook, this::onDeleteWebhook);
            recyclerView.setAdapter(adapter);

            fab.setOnClickListener(v -> showAddWebhookDialog());

            loadWebhooks();
            return view;
        }

        private void showAddWebhookDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_webhook, null);
            EditText urlInput = dialogView.findViewById(R.id.webhook_url_input);
            EditText eventInput = dialogView.findViewById(R.id.webhook_event_input);

            builder.setView(dialogView)
                    .setTitle(R.string.dialog_add_webhook_title)
                    .setPositiveButton(R.string.button_add, (dialog, which) -> {
                        String url = urlInput.getText().toString().trim();
                        String event = eventInput.getText().toString().trim();
                        if (!url.isEmpty() && !event.isEmpty()) {
                            addWebhook(url, event);
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, null)
                    .show();
        }

        private void addWebhook(String url, String event) {
            Webhook webhook = new Webhook(url, event, "Active");
            webhooks.add(webhook);
            adapter.notifyItemInserted(webhooks.size() - 1);
            Toast.makeText(requireContext(), R.string.webhook_added, Toast.LENGTH_SHORT).show();
        }

        private void loadWebhooks() {
            webhooks.clear();
            webhooks.add(new Webhook("https://example.com/webhook", "alert.created", "Active"));
            adapter.notifyDataSetChanged();
        }

        private void onTestWebhook(Webhook webhook) {
            Toast.makeText(requireContext(), R.string.webhook_test_sent, Toast.LENGTH_SHORT).show();
        }

        private void onDeleteWebhook(int position) {
            webhooks.remove(position);
            adapter.notifyItemRemoved(position);
            Toast.makeText(requireContext(), R.string.webhook_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    // Plugins Fragment
    public static class PluginsFragment extends androidx.fragment.app.Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_plugins, container, false);
            TextView textView = view.findViewById(R.id.plugins_text);
            textView.setText(R.string.plugins_coming_soon);
            return view;
        }
    }

    // Pager Adapter
    private static class DeveloperPagerAdapter extends androidx.fragment.app.FragmentStateAdapter {
        public DeveloperPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            switch (position) {
                case TAB_API_KEYS:
                    return new ApiKeyFragment();
                case TAB_WEBHOOKS:
                    return new WebhookFragment();
                case TAB_PLUGINS:
                    return new PluginsFragment();
                default:
                    return new ApiKeyFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    // Data Models
    static class ApiKey {
        String name;
        String key;
        String status;

        ApiKey(String name, String key, String status) {
            this.name = name;
            this.key = key;
            this.status = status;
        }
    }

    static class Webhook {
        String url;
        String event;
        String status;

        Webhook(String url, String event, String status) {
            this.url = url;
            this.event = event;
            this.status = status;
        }
    }

    // API Key Adapter
    static class ApiKeyAdapter extends RecyclerView.Adapter<ApiKeyAdapter.ViewHolder> {
        private List<ApiKey> keys;
        private OnKeyClickListener onCopyListener;
        private OnKeyClickListener onRevokeListener;

        interface OnKeyClickListener {
            void onClick(String key, int position);
        }

        ApiKeyAdapter(List<ApiKey> keys, OnKeyClickListener onCopyListener, OnKeyClickListener onRevokeListener) {
            this.keys = keys;
            this.onCopyListener = onCopyListener;
            this.onRevokeListener = onRevokeListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_api_key, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ApiKey key = keys.get(position);
            holder.nameText.setText(key.name);
            holder.keyText.setText(maskKey(key.key));
            holder.statusText.setText(key.status);

            holder.copyBtn.setOnClickListener(v -> onCopyListener.onClick(key.key, position));
            holder.revokeBtn.setEnabled("Active".equals(key.status));
            holder.revokeBtn.setOnClickListener(v -> onRevokeListener.onClick(key.key, position));
        }

        private String maskKey(String key) {
            if (key.length() <= 8) return key;
            return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
        }

        @Override
        public int getItemCount() {
            return keys.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, keyText, statusText;
            ImageButton copyBtn, revokeBtn;

            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.key_name);
                keyText = itemView.findViewById(R.id.key_value);
                statusText = itemView.findViewById(R.id.key_status);
                copyBtn = itemView.findViewById(R.id.btn_copy);
                revokeBtn = itemView.findViewById(R.id.btn_revoke);
            }
        }
    }

    // Webhook Adapter
    static class WebhookAdapter extends RecyclerView.Adapter<WebhookAdapter.ViewHolder> {
        private List<Webhook> webhooks;
        private OnWebhookClickListener onTestListener;
        private OnWebhookClickListener onDeleteListener;

        interface OnWebhookClickListener {
            void onClick(Webhook webhook, int position);
        }

        WebhookAdapter(List<Webhook> webhooks, OnWebhookClickListener onTestListener, OnWebhookClickListener onDeleteListener) {
            this.webhooks = webhooks;
            this.onTestListener = onTestListener;
            this.onDeleteListener = onDeleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_webhook, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Webhook webhook = webhooks.get(position);
            holder.urlText.setText(webhook.url);
            holder.eventText.setText(webhook.event);

            holder.testBtn.setOnClickListener(v -> onTestListener.onClick(webhook, position));
            holder.deleteBtn.setOnClickListener(v -> onDeleteListener.onClick(webhook, position));
        }

        @Override
        public int getItemCount() {
            return webhooks.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView urlText, eventText;
            ImageButton testBtn, deleteBtn;

            ViewHolder(View itemView) {
                super(itemView);
                urlText = itemView.findViewById(R.id.webhook_url);
                eventText = itemView.findViewById(R.id.webhook_event);
                testBtn = itemView.findViewById(R.id.btn_test);
                deleteBtn = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}
