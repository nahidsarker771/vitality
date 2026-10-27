package com.nahid.vitality.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nahid.vitality.R;
import com.nahid.vitality.core.PowerTime;
import com.nahid.vitality.core.Theme;
import com.nahid.vitality.core.ThemeManager;

public class PowerCardView extends LinearLayout implements ThemeAware {

    public static final int WILL = 0;
    public static final int WEALTH = 1;
    public static final int ENERGY = 2;

    private int kind = WILL;
    private Theme theme;
    private View dot;
    private TextView title;
    private TextView subtitle;
    private TextView percent;
    private TextView status;
    private TextView timeLeft;
    private VitalityBarView bar;

    public PowerCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        setTag("card");
        int pad = Math.round(ThemeManager.dp(context, 11f));
        setPadding(pad, pad, pad, pad);
        LayoutInflater.from(context).inflate(R.layout.view_power_card, this, true);
        dot = findViewById(R.id.pcDot);
        title = findViewById(R.id.pcTitle);
        subtitle = findViewById(R.id.pcSubtitle);
        percent = findViewById(R.id.pcPercent);
        status = findViewById(R.id.pcStatus);
        timeLeft = findViewById(R.id.pcTimeLeft);
        bar = findViewById(R.id.pcBar);
    }

    public void setKind(int kind) {
        this.kind = kind;
        switch (kind) {
            case WEALTH:
                title.setText(R.string.wealth_power);
                subtitle.setText(R.string.wealth_power_sub);
                break;
            case ENERGY:
                title.setText(R.string.energy_power);
                subtitle.setText(R.string.energy_power_sub);
                break;
            default:
                title.setText(R.string.willpower);
                subtitle.setText(R.string.willpower_sub);
                break;
        }
        applyColors();
    }

    public void setPercent(float value) {
        percent.setText(PowerTime.roundedPercent(value) + "%");
        bar.setValue(value);
    }

    public void setStatus(CharSequence left, CharSequence right) {
        status.setText(left);
        timeLeft.setText(right);
    }

    @Override
    public void applyTheme(Theme theme) {
        this.theme = theme;
        applyColors();
    }

    private void applyColors() {
        if (theme == null) {
            return;
        }
        title.setTextColor(theme.text);
        percent.setTextColor(theme.text);
        subtitle.setTextColor(theme.muted);
        status.setTextColor(theme.muted);
        timeLeft.setTextColor(theme.muted);
        int accent;
        switch (kind) {
            case WEALTH:
                accent = theme.wealth;
                break;
            case ENERGY:
                accent = theme.energy;
                break;
            default:
                accent = theme.will;
                break;
        }
        dot.setBackground(ThemeManager.oval(accent));
        bar.setColors(accent, ThemeManager.lighten(accent, 0.35f));
    }
}
