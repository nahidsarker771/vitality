package com.nahid.vitality;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.core.Themes;
import com.nahid.vitality.notify.WidgetRefresher;

public class ThemesActivity extends BaseActivity {

    private GridView grid;
    private ThemeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_themes);

        grid = findViewById(R.id.gridThemes);
        applyChrome(3);

        adapter = new ThemeAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            if (AppPrefs.themeIndex(this) != position) {
                AppPrefs.setThemeIndex(this, position);
                WidgetRefresher.updateAll(this);
                recreate();
            }
        });
    }

    @Override
    public void onBackPressed() {
        open(SettingsActivity.class);
        finish();
    }

    private final class ThemeAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(ThemesActivity.this);

        @Override
        public int getCount() {
            return Themes.count();
        }

        @Override
        public Theme getItem(int position) {
            return Themes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_theme, parent, false);
            }
            Theme item = getItem(position);
            boolean selected = AppPrefs.themeIndex(ThemesActivity.this) == position;

            convertView.setBackground(ThemeManager.gradient(ThemesActivity.this,
                    item.bg,
                    ThemeManager.blend(item.bg, item.surface, 0.65f),
                    selected ? item.will : item.border, 18f));

            TextView name = convertView.findViewById(R.id.txtThemeName);
            name.setText(item.nameRes);
            name.setTextColor(item.text);

            TextView badge = convertView.findViewById(R.id.txtThemeSelected);
            badge.setTextColor(item.will);
            badge.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);

            setDot(convertView.findViewById(R.id.dotA), ThemeManager.oval(item.will));
            setDot(convertView.findViewById(R.id.dotB), ThemeManager.oval(item.wealth));
            setDot(convertView.findViewById(R.id.dotC), ThemeManager.oval(item.energy));

            GradientDrawable surfaceDot = new GradientDrawable();
            surfaceDot.setShape(GradientDrawable.OVAL);
            surfaceDot.setColor(item.surfaceAlt);
            surfaceDot.setStroke(Math.max(1, (int) ThemeManager.dp(ThemesActivity.this, 1f)),
                    item.border);
            setDot(convertView.findViewById(R.id.dotD), surfaceDot);
            return convertView;
        }

        private void setDot(View view, GradientDrawable drawable) {
            view.setBackground(drawable);
        }
    }
}
