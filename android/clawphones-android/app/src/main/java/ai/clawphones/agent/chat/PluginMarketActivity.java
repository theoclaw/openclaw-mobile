package ai.clawphones.agent.chat;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PluginMarketActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView pluginsRecycler;
    private PluginAdapter adapter;
    private List<Plugin> allPlugins = new ArrayList<>();
    private List<Plugin> filteredPlugins = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_market);

        setupToolbar();
        setupSearch();
        setupPluginList();
        loadPlugins();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.plugin_market_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlugins(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupPluginList() {
        pluginsRecycler = findViewById(R.id.plugins_recycler);
        pluginsRecycler.setLayoutManager(new GridLayoutManager(this, 2));

        adapter = new PluginAdapter(filteredPlugins, this::onPluginToggle);
        pluginsRecycler.setAdapter(adapter);
    }

    private void loadPlugins() {
        allPlugins.clear();
        allPlugins.add(new Plugin("Code Formatter", "Auto-format your code", "Formatter", true));
        allPlugins.add(new Plugin("Git Integration", "Version control support", "Development", false));
        allPlugins.add(new Plugin("API Explorer", "Test API endpoints", "Development", true));
        allPlugins.add(new Plugin("Database Helper", "SQL query assistant", "Database", false));
        allPlugins.add(new Plugin("UI Designer", "Create layouts visually", "UI/UX", true));
        allPlugins.add(new Plugin("Logger Pro", "Advanced logging", "Tools", false));
        allPlugins.add(new Plugin("Translator", "Multi-language support", "Utility", true));
        allPlugins.add(new Plugin("Image Editor", "Photo manipulation", "Media", false));

        filteredPlugins.addAll(allPlugins);
        adapter.notifyDataSetChanged();
    }

    private void filterPlugins(String query) {
        filteredPlugins.clear();

        if (query.isEmpty()) {
            filteredPlugins.addAll(allPlugins);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Plugin plugin : allPlugins) {
                if (plugin.name.toLowerCase().contains(lowerQuery) ||
                        plugin.description.toLowerCase().contains(lowerQuery) ||
                        plugin.category.toLowerCase().contains(lowerQuery)) {
                    filteredPlugins.add(plugin);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void onPluginToggle(Plugin plugin, int position) {
        plugin.installed = !plugin.installed;
        adapter.notifyItemChanged(position);

        String message = plugin.installed ?
                getString(R.string.plugin_installed, plugin.name) :
                getString(R.string.plugin_uninstalled, plugin.name);
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }

    // Plugin Data Model
    static class Plugin {
        String name;
        String description;
        String category;
        boolean installed;

        Plugin(String name, String description, String category, boolean installed) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.installed = installed;
        }
    }

    // Plugin Adapter
    static class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {
        private List<Plugin> plugins;
        private OnPluginToggleListener toggleListener;

        interface OnPluginToggleListener {
            void onToggle(Plugin plugin, int position);
        }

        PluginAdapter(List<Plugin> plugins, OnPluginToggleListener toggleListener) {
            this.plugins = plugins;
            this.toggleListener = toggleListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_plugin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Plugin plugin = plugins.get(position);

            holder.nameText.setText(plugin.name);
            holder.descText.setText(plugin.description);
            holder.categoryText.setText(plugin.category);

            updateInstallButton(holder, plugin.installed);

            holder.installBtn.setOnClickListener(v -> {
                toggleListener.onToggle(plugin, position);
                updateInstallButton(holder, plugin.installed);
            });
        }

        private void updateInstallButton(ViewHolder holder, boolean installed) {
            Context context = holder.installBtn.getContext();
            if (installed) {
                holder.installBtn.setText(context.getString(R.string.button_uninstall));
                holder.installBtn.setBackgroundColor(
                        context.getColor(android.R.color.holo_red_dark));
            } else {
                holder.installBtn.setText(context.getString(R.string.button_install));
                holder.installBtn.setBackgroundColor(
                        context.getColor(android.R.color.holo_blue_dark));
            }
        }

        @Override
        public int getItemCount() {
            return plugins.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconView;
            TextView nameText;
            TextView descText;
            TextView categoryText;
            android.widget.Button installBtn;

            ViewHolder(View itemView) {
                super(itemView);
                iconView = itemView.findViewById(R.id.plugin_icon);
                nameText = itemView.findViewById(R.id.plugin_name);
                descText = itemView.findViewById(R.id.plugin_desc);
                categoryText = itemView.findViewById(R.id.plugin_category);
                installBtn = itemView.findViewById(R.id.btn_install);
            }
        }
    }
}
