package com.nahid.vitality.core;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;

import com.nahid.vitality.R;
import com.nahid.vitality.ui.ThemeAware;

public final class ThemeManager {

    private ThemeManager() {
    }

    public static void apply(Activity activity, Theme theme) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(theme.bg);
        window.setNavigationBarColor(theme.navBg);
        window.setBackgroundDrawable(new ColorDrawable(theme.bg));
        window.getDecorView().setBackgroundColor(theme.bg);

        int flags = window.getDecorView().getSystemUiVisibility();
        int lightFlags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        if (theme.light) {
            flags |= lightFlags;
        } else {
            flags &= ~lightFlags;
        }
        window.getDecorView().setSystemUiVisibility(flags);

        View root = activity.findViewById(R.id.root);
        if (root != null) {
            root.setBackgroundColor(theme.bg);
            applyTree(root, theme);
        }
    }

    private static void applyTree(View view, Theme theme) {
        Context context = view.getContext();
        Object tag = view.getTag();
        String key = tag instanceof String ? (String) tag : null;

        if (key != null) {
            if ("card".equals(key)) {
                view.setBackground(rounded(context, theme.surface, theme.border, 20f));
            } else if ("cardAlt".equals(key)) {
                view.setBackground(rounded(context, theme.surfaceAlt, theme.border, 18f));
            } else if ("nav".equals(key)) {
                view.setBackground(rounded(context, theme.navBg, theme.border, 22f));
            } else if ("chip".equals(key)) {
                view.setBackground(rounded(context, theme.surfaceAlt, theme.border, 99f));
            } else if ("chipAccent".equals(key)) {
                view.setBackground(rounded(context,
                        withAlpha(theme.will, 0.14f),
                        withAlpha(theme.will, 0.5f), 99f));
            } else if ("btn".equals(key)) {
                view.setBackground(rounded(context,
                        withAlpha(theme.will, 0.16f),
                        withAlpha(theme.will, 0.5f), 99f));
            } else if ("btnAlt".equals(key)) {
                view.setBackground(rounded(context, theme.surfaceAlt, theme.border, 99f));
            } else if ("divider".equals(key)) {
                view.setBackgroundColor(theme.border);
            }
        }

        if (view instanceof TextView) {
            TextView text = (TextView) view;
            if ("primary".equals(key)) {
                text.setTextColor(theme.text);
            } else if ("muted".equals(key)) {
                text.setTextColor(theme.muted);
            } else if ("accent".equals(key)) {
                text.setTextColor(theme.will);
            } else if ("chipAccent".equals(key) || "btn".equals(key)) {
                text.setTextColor(theme.will);
            } else if ("chip".equals(key) || "btnAlt".equals(key)) {
                text.setTextColor(theme.text);
            }
        }

        if (view instanceof Switch) {
            Switch toggle = (Switch) view;
            toggle.setThumbTintList(ColorStateList.valueOf(theme.will));
            toggle.setTrackTintList(ColorStateList.valueOf(withAlpha(theme.muted, 0.35f)));
        }

        if (view instanceof ThemeAware) {
            ((ThemeAware) view).applyTheme(theme);
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyTree(group.getChildAt(i), theme);
            }
        }
    }

    public static GradientDrawable rounded(Context context, int fillColor, int strokeColor, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeColor != 0) {
            drawable.setStroke(Math.max(1, (int) dp(context, 1f)), strokeColor);
        }
        return drawable;
    }

    public static GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    public static int withAlpha(int color, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    public static int blend(int first, int second, float ratio) {
        float r = Math.max(0f, Math.min(1f, ratio));
        int a = Math.round(Color.alpha(first) + (Color.alpha(second) - Color.alpha(first)) * r);
        int red = Math.round(Color.red(first) + (Color.red(second) - Color.red(first)) * r);
        int green = Math.round(Color.green(first) + (Color.green(second) - Color.green(first)) * r);
        int blue = Math.round(Color.blue(first) + (Color.blue(second) - Color.blue(first)) * r);
        return Color.argb(a, red, green, blue);
    }

    public static int lighten(int color, float amount) {
        return blend(color, 0xFFFFFFFF, amount);
    }

    public static float dp(Context context, float value) {
        return value * context.getResources().getDisplayMetrics().density;
    }

    public static Context dialogContext(Context context, Theme theme) {
        int style = theme.light ? R.style.DialogLight : R.style.DialogDark;
        return new android.view.ContextThemeWrapper(context, style);
    }
}
