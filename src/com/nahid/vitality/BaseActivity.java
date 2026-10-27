package com.nahid.vitality;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import com.nahid.vitality.core.AppPrefs;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;
import com.nahid.vitality.core.Themes;

public abstract class BaseActivity extends Activity {

    private static final int[] NAV_IDS = {
            R.id.navHome, R.id.navTrack, R.id.navHistory, R.id.navSettings
    };
    private static final Class<?>[] NAV_TARGETS = {
            MainActivity.class, TrackActivity.class, HistoryActivity.class,
            SettingsActivity.class
    };

    protected Theme theme;
    private int appliedTheme = -1;
    private int activeTab = -1;
    private OnBackInvokedCallback backCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PowerTime.configure(this);
        activeTab = resolveTab();
        theme = Themes.get(AppPrefs.themeIndex(this));
        appliedTheme = theme.index;
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::onBackPressed;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeTab >= 0) {
            TabNavigator.onTabResumed(activeTab);
        }
        if (AppPrefs.themeIndex(this) != appliedTheme) {
            recreate();
        }
    }

    private int resolveTab() {
        if (this instanceof MainActivity) {
            return 0;
        }
        if (this instanceof TrackActivity) {
            return 1;
        }
        if (this instanceof HistoryActivity) {
            return 2;
        }
        if (this instanceof SettingsActivity) {
            return 3;
        }
        return -1;
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
            boolean currentTab = i == activeTab;
            item.setTextColor(selected ? theme.will : theme.muted);
            item.setCompoundDrawableTintList(ColorStateList.valueOf(selected ? theme.will : theme.muted));
            item.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            item.setBackground(selected
                    ? ThemeManager.gradient(this,
                    ThemeManager.withAlpha(theme.will, 0.20f),
                    ThemeManager.withAlpha(theme.energy, 0.10f),
                    ThemeManager.withAlpha(theme.will, 0.45f), 18f)
                    : null);
            item.setClickable(!currentTab);
            item.setFocusable(!currentTab);
            item.setOnClickListener(currentTab ? null : view -> openTab(index));
        }
    }

    private void openTab(int index) {
        if (index == activeTab) {
            return;
        }
        if (TabNavigator.isCurrent(index)) {
            if (activeTab >= 0) {
                return;
            }
        } else if (!TabNavigator.beginNavigation(index)) {
            return;
        }
        push(new Intent(this, NAV_TARGETS[index]));
    }

    protected void open(Class<? extends Activity> target) {
        push(new Intent(this, target));
    }

    @Override
    public void onBackPressed() {
        if (activeTab < 0) {
            super.onBackPressed();
            return;
        }
        int previous = TabNavigator.beginBack(activeTab);
        if (previous >= 0) {
            push(new Intent(this, NAV_TARGETS[previous]));
            return;
        }
        if (previous == -2) {
            return;
        }
        TabNavigator.clear();
        finishAffinity();
    }

    private void push(Intent intent) {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        overridePendingTransition(0, 0);
    }
}
