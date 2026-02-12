package ai.clawphones.agent.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class UsageDashboardActivity extends AppCompatActivity {

    private Spinner dateRangeSpinner;
    private UsageChartView chartView;
    private RecyclerView endpointsRecycler;
    private TextView latencyText;
    private LinearProgressIndicator rateLimitProgress;
    private TextView rateLimitText;

    private String[] dateRanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_dashboard);

        setupToolbar();
        setupDateRangeSpinner();
        setupChart();
        setupEndpointsList();
        setupRateLimit();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.usage_dashboard_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDateRangeSpinner() {
        dateRangeSpinner = findViewById(R.id.date_range_spinner);
        dateRanges = getResources().getStringArray(R.array.date_range_options);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, dateRanges);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateRangeSpinner.setAdapter(adapter);

        dateRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChartData(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupChart() {
        chartView = findViewById(R.id.usage_chart);
        chartView.setData(generateSampleData());
    }

    private void setupEndpointsList() {
        endpointsRecycler = findViewById(R.id.endpoints_recycler);
        endpointsRecycler.setLayoutManager(new LinearLayoutManager(this));

        List<EndpointStats> endpoints = generateEndpointStats();
        EndpointAdapter adapter = new EndpointAdapter(endpoints);
        endpointsRecycler.setAdapter(adapter);
    }

    private void setupRateLimit() {
        rateLimitProgress = findViewById(R.id.rate_limit_progress);
        rateLimitText = findViewById(R.id.rate_limit_text);
        latencyText = findViewById(R.id.latency_text);

        int currentUsage = 75;
        int maxLimit = 100;
        rateLimitProgress.setProgress(currentUsage);
        rateLimitText.setText(getString(R.string.rate_limit_format, currentUsage, maxLimit));

        latencyText.setText(getString(R.string.latency_value, 145));
    }

    private void updateChartData(int rangeIndex) {
        chartView.setData(generateSampleData());
    }

    private int[] generateSampleData() {
        return new int[]{120, 190, 150, 220, 180, 250, 200};
    }

    private List<EndpointStats> generateEndpointStats() {
        List<EndpointStats> list = new ArrayList<>();
        list.add(new EndpointStats("/api/v1/chat", 15420, 120));
        list.add(new EndpointStats("/api/v1/completions", 12350, 98));
        list.add(new EndpointStats("/api/v1/embeddings", 8920, 85));
        list.add(new EndpointStats("/api/v1/stream", 7650, 110));
        list.add(new EndpointStats("/api/v1/vision", 5430, 150));
        return list;
    }

    // Custom Chart View
    public static class UsageChartView extends View {
        private int[] data;
        private Paint barPaint;
        private Paint textPaint;
        private Paint gridPaint;

        private static final int PADDING = 40;
        private static final int BAR_WIDTH = 50;
        private static final int BAR_GAP = 30;

        public UsageChartView(Context context) {
            super(context);
            init();
        }

        private void init() {
            barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            barPaint.setColor(Color.parseColor("#6200EE"));
            barPaint.setStyle(Paint.Style.FILL);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.parseColor("#666666"));
            textPaint.setTextSize(36);

            gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            gridPaint.setColor(Color.parseColor("#EEEEEE"));
            gridPaint.setStyle(Paint.Style.STROKE);
            gridPaint.setStrokeWidth(2);
        }

        public void setData(int[] data) {
            this.data = data;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (data == null || data.length == 0) return;

            int width = getWidth();
            int height = getHeight();
            int chartHeight = height - PADDING * 2;
            int chartWidth = width - PADDING * 2;

            int maxValue = 300;

            drawGrid(canvas, width, chartHeight);

            int totalBarWidth = data.length * BAR_WIDTH + (data.length - 1) * BAR_GAP;
            int startX = (chartWidth - totalBarWidth) / 2 + PADDING;

            for (int i = 0; i < data.length; i++) {
                int barHeight = (int) ((data[i] / (float) maxValue) * chartHeight);
                int x = startX + i * (BAR_WIDTH + BAR_GAP);
                int y = height - PADDING - barHeight;

                canvas.drawRect(x, y, x + BAR_WIDTH, height - PADDING, barPaint);

                String label = String.valueOf(data[i]);
                float textWidth = textPaint.measureText(label);
                canvas.drawText(label, x + (BAR_WIDTH - textWidth) / 2, y - 10, textPaint);
            }
        }

        private void drawGrid(Canvas canvas, int width, int chartHeight) {
            int bottomY = getHeight() - PADDING;
            canvas.drawLine(PADDING, bottomY, width - PADDING, bottomY, gridPaint);
        }
    }

    // Endpoint Stats Model
    static class EndpointStats {
        String endpoint;
        int requestCount;
        int avgLatency;

        EndpointStats(String endpoint, int requestCount, int avgLatency) {
            this.endpoint = endpoint;
            this.requestCount = requestCount;
            this.avgLatency = avgLatency;
        }
    }

    // Endpoint Adapter
    static class EndpointAdapter extends RecyclerView.Adapter<EndpointAdapter.ViewHolder> {
        private List<EndpointStats> endpoints;

        EndpointAdapter(List<EndpointStats> endpoints) {
            this.endpoints = endpoints;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_endpoint, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            EndpointStats stat = endpoints.get(position);
            holder.endpointText.setText(stat.endpoint);
            holder.requestCountText.setText(String.valueOf(stat.requestCount));
            holder.latencyText.setText(stat.avgLatency + "ms");
        }

        @Override
        public int getItemCount() {
            return endpoints.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView endpointText, requestCountText, latencyText;

            ViewHolder(View itemView) {
                super(itemView);
                endpointText = itemView.findViewById(R.id.endpoint_name);
                requestCountText = itemView.findViewById(R.id.request_count);
                latencyText = itemView.findViewById(R.id.endpoint_latency);
            }
        }
    }
}
