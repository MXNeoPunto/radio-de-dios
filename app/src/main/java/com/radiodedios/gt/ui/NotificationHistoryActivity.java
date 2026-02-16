package com.radiodedios.gt.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.radiodedios.gt.R;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;
    private com.radiodedios.gt.manager.BibleManager bibleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appBarLayout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // I'll use the back arrow instead of drag handle if it's a separate activity
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material); 
        }

        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Load Daily Verse
        bibleManager = new com.radiodedios.gt.manager.BibleManager(this);
        TextView dailyVerseText = findViewById(R.id.dailyVerseText);
        if (dailyVerseText != null) {
            dailyVerseText.setText(bibleManager.getDailyVerse());
        }

        // Mark as seen
        SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        appPrefs.edit().putString("verse_seen_date", currentDate).apply();

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        loadNotifications();
    }

    private void loadNotifications() {
        SharedPreferences prefs = getSharedPreferences("notifications_prefs", MODE_PRIVATE);
        String json = prefs.getString("notification_list", "[]");
        Type type = new TypeToken<List<NotificationItem>>() {}.getType();
        notificationList = new Gson().fromJson(json, type);

        if (notificationList == null) {
            notificationList = new ArrayList<>();
        }

        // Sort by date desc
        notificationList.sort((o1, o2) -> Long.compare(o2.timestamp, o1.timestamp));

        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        updateEmptyView();
    }
    
    private void updateEmptyView() {
        if (notificationList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllNotifications() {
        notificationList.clear();
        adapter.notifyDataSetChanged();
        updateEmptyView();
        
        getSharedPreferences("notifications_prefs", MODE_PRIVATE)
            .edit()
            .remove("notification_list")
            .apply();
            
        Toast.makeText(this, R.string.notifications_cleared, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notification_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_clear_all) {
            clearAllNotifications();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Model
    public static class NotificationItem {
        public String title;
        public String body;
        public long timestamp;

        public NotificationItem(String title, String body, long timestamp) {
            this.title = title;
            this.body = body;
            this.timestamp = timestamp;
        }
    }

    // Adapter
    class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationItem> list;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

        NotificationAdapter(List<NotificationItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Re-using a simple layout or I can define one inside the XML step. 
            // I'll use a simple layout inflating programmatic or a new layout file.
            // Let's create a layout item_notification.xml in the next step or inline it?
            // I'll write item_notification.xml separately to be clean.
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = list.get(position);
            holder.title.setText(item.title);
            holder.body.setText(item.body);
            holder.date.setText(dateFormat.format(new Date(item.timestamp)));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, body, date;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.textTitle);
                body = itemView.findViewById(R.id.textBody);
                date = itemView.findViewById(R.id.textDate);
            }
        }
    }
}
