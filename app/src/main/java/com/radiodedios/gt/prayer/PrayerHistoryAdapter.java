package com.radiodedios.gt.prayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.radiodedios.gt.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrayerHistoryAdapter extends RecyclerView.Adapter<PrayerHistoryAdapter.ViewHolder> {

    private final List<Prayer> prayers;
    private final OnPrayerClickListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnPrayerClickListener {
        void onListenClicked(Prayer prayer);
        void onShareClicked(Prayer prayer);
    }

    public PrayerHistoryAdapter(List<Prayer> prayers, OnPrayerClickListener listener) {
        this.prayers = prayers;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prayer_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prayer prayer = prayers.get(position);

        holder.tvCategory.setText(prayer.getCategory());
        holder.tvDate.setText(dateFormat.format(new Date(prayer.getTimestamp())));
        holder.tvPrayerText.setText(prayer.getText());
        holder.tvVerse.setText(prayer.getVerse());

        holder.btnListen.setOnClickListener(v -> listener.onListenClicked(prayer));
        holder.btnShare.setOnClickListener(v -> listener.onShareClicked(prayer));
    }

    @Override
    public int getItemCount() {
        return prayers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDate, tvPrayerText, tvVerse;
        ImageView btnListen, btnShare;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrayerText = itemView.findViewById(R.id.tvPrayerText);
            tvVerse = itemView.findViewById(R.id.tvVerse);
            btnListen = itemView.findViewById(R.id.btnListen);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}
