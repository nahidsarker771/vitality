package com.nahid.vitality;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.data.HistoryDb;
import com.nahid.vitality.ui.HistoryChartView;
import com.nahid.vitality.ui.VitalityBarView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends BaseActivity {

    private TextView txtAvg7;
    private TextView txtDays;
    private TextView txtEmpty;
    private ListView list;
    private HistoryChartView chart;
    private List<HistoryDb.Day> days = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        txtAvg7 = findViewById(R.id.txtAvg7);
        txtDays = findViewById(R.id.txtDays);
        txtEmpty = findViewById(R.id.txtEmpty);
        list = findViewById(R.id.listHistory);
        chart = findViewById(R.id.chart);

        applyChrome(2);
        ((TextView) findViewById(R.id.txtRange)).setText(R.string.recent_range);
        list.setEmptyView(txtEmpty);
        load();

        list.setOnItemClickListener((parent, view, position, id) -> showDetail(days.get(position)));
    }

    private void load() {
        days = HistoryDb.get(this).days(90);
        LocalDate today = LocalDate.now();

        int tracked = days.size();
        txtDays.setText(getString(R.string.days_tracked, tracked));

        LocalDate cutoff = today.minusDays(6);
        float total = 0f;
        int counted = 0;
        for (HistoryDb.Day day : days) {
            if (!day.date.isBefore(cutoff)) {
                total += day.average();
                counted++;
            }
        }
        if (counted > 0) {
            txtAvg7.setText(Math.round(total / counted) + "%");
        } else {
            txtAvg7.setText(R.string.dash);
        }

        Map<String, HistoryDb.Day> byDate = new HashMap<>();
        for (HistoryDb.Day day : days) {
            byDate.put(day.dayKey, day);
        }
        List<HistoryChartView.Point> points = new ArrayList<>();
        for (int i = 13; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            HistoryDb.Day day = byDate.get(date.toString());
            points.add(new HistoryChartView.Point(date,
                    day == null ? 0f : day.average(), day != null));
        }
        chart.setPoints(points);

        list.setAdapter(new HistoryAdapter());
    }

    private void showDetail(HistoryDb.Day day) {
        StringBuilder body = new StringBuilder();
        body.append(getString(R.string.detail_line, getString(R.string.willpower),
                Math.round(day.avgWill))).append('\n');
        body.append(getString(R.string.detail_line, getString(R.string.wealth_power),
                Math.round(day.avgWealth))).append('\n');
        body.append(getString(R.string.detail_line, getString(R.string.energy_power),
                Math.round(day.avgEnergy))).append('\n').append('\n');
        body.append(getString(R.string.detail_samples, day.samples));

        new AlertDialog.Builder(dialogContext())
                .setTitle(PowerTime.fullDate(day.date))
                .setMessage(body.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private final class HistoryAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(HistoryActivity.this);

        @Override
        public int getCount() {
            return days.size();
        }

        @Override
        public HistoryDb.Day getItem(int position) {
            return days.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_history, parent, false);
            }
            HistoryDb.Day day = getItem(position);
            LocalDate today = LocalDate.now();
            String label;
            if (day.date.equals(today)) {
                label = getString(R.string.today);
            } else if (day.date.equals(today.minusDays(1))) {
                label = getString(R.string.yesterday);
            } else {
                label = PowerTime.shortDate(day.date);
            }

            TextView date = convertView.findViewById(R.id.txtRowDate);
            TextView meta = convertView.findViewById(R.id.txtRowMeta);
            TextView percent = convertView.findViewById(R.id.txtRowPct);
            VitalityBarView bar = convertView.findViewById(R.id.rowBar);

            date.setText(label);
            date.setTextColor(theme.text);
            meta.setText(getString(R.string.row_meta, day.samples,
                    Math.round(day.lastAverage())));
            meta.setTextColor(theme.muted);
            int average = Math.round(day.average());
            percent.setText(average + "%");
            percent.setTextColor(theme.will);
            bar.setColors(theme.will, theme.energy);
            bar.setValueInstant(average);

            int stroke = day.date.equals(today) ? theme.will : theme.border;
            convertView.setBackground(ThemeManager.rounded(HistoryActivity.this,
                    theme.surface, stroke, 16f));
            return convertView;
        }
    }
}
