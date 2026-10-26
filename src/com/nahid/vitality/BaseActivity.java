package com.nahid.vitality;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.core.Themes;

public abstract class BaseActivity extends Activity {

    private static final int[] NAV_IDS = {
            R.id.navHome, R.id.navTrack, R.id.navHistory, R.id.navThemes, R.id.navSettings
    };
    private static final Class<?>[] NAV_TARGETS = {
            MainActivity.class, TrackActivity.class, HistoryActivity.class,
            ThemesActivity.class, SettingsActivity.class
    };

    protected Theme theme;
    private int appliedTheme = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = Themes.get(AppPrefs.themeIndex(this));
        appliedTheme = theme.index;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppPrefs.themeIndex(this) != appliedTheme) {
            recreate();
        }
    }

    protected void applyChrome(int activeIndex) {
        ThemeManager.apply(this, theme);
        wireNavigation(activeIndex);
    }

    protected Context dialogContext() {
        return ThemeManager.dialogContext(this, theme);
    }

    private void wireNavigation(int activeIndex) {
        for (int i = 0; i < NAV_IDS.length; i++) {
            TextView item = findViewById(NAV_IDS[i]);
            if (item == null) {
                continue;
            }
            final int index = i;
            boolean selected = i == activeIndex;
            item.setTextColor(selected ? theme.will : theme.muted);
            item.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            item.setBackground(selected
                    ? ThemeManager.rounded(this, ThemeManager.withAlpha(theme.will, 0.14f), 0, 99f)
                    : null);
            item.setOnClickListener(view -> {
                if (index != activeIndex) {
                    startActivity(new Intent(this, NAV_TARGETS[index])
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }
    }

    protected void open(Class<? extends Activity> target) {
        startActivity(new Intent(this, target)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
