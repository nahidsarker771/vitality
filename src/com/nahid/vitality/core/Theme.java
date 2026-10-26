package com.nahid.vitality.core;

public final class Theme {

    public final int index;
    public final int nameRes;
    public final int bg;
    public final int surface;
    public final int surfaceAlt;
    public final int border;
    public final int text;
    public final int muted;
    public final int navBg;
    public final int will;
    public final int wealth;
    public final int energy;
    public final boolean light;

    public Theme(int index, int nameRes, int bg, int surface, int surfaceAlt, int border,
                 int text, int muted, int navBg, int will, int wealth, int energy, boolean light) {
        this.index = index;
        this.nameRes = nameRes;
        this.bg = bg;
        this.surface = surface;
        this.surfaceAlt = surfaceAlt;
        this.border = border;
        this.text = text;
        this.muted = muted;
        this.navBg = navBg;
        this.will = will;
        this.wealth = wealth;
        this.energy = energy;
        this.light = light;
    }
}
