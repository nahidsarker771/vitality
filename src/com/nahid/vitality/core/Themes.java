package com.nahid.vitality.core;

import com.nahid.vitality.R;

public final class Themes {

    private static final Theme[] ALL = new Theme[]{
            new Theme(0, R.string.theme_midnight,
                    0xFF0A0E17, 0xFF121826, 0xFF182032, 0xFF222C44,
                    0xFFF4F6FF, 0xFF8A93A8, 0xFF0D1220,
                    0xFF8B7CFF, 0xFFFFC857, 0xFF46E5A0, false),
            new Theme(1, R.string.theme_aurora,
                    0xFF07131A, 0xFF0E1F28, 0xFF132A35, 0xFF1E3B49,
                    0xFFEAFBFF, 0xFF84A7B2, 0xFF0A1820,
                    0xFF6EE7FF, 0xFFFFD166, 0xFF7CFFB2, false),
            new Theme(2, R.string.theme_ocean,
                    0xFF061521, 0xFF0B2231, 0xFF103042, 0xFF1A4258,
                    0xFFE8F6FF, 0xFF7E9FB3, 0xFF081B28,
                    0xFF4CC9F0, 0xFFF9C74F, 0xFF43AA8B, false),
            new Theme(3, R.string.theme_sunset,
                    0xFF170D12, 0xFF24141C, 0xFF2F1A24, 0xFF472536,
                    0xFFFFF2F6, 0xFFB08A99, 0xFF1D1017,
                    0xFFFF6B9D, 0xFFFFB45C, 0xFF7EE0B8, false),
            new Theme(4, R.string.theme_forest,
                    0xFF0A130E, 0xFF111F17, 0xFF172A20, 0xFF243B2C,
                    0xFFF0FBF3, 0xFF8AA795, 0xFF0D1711,
                    0xFF9C8CFF, 0xFFE6C15A, 0xFF7BC96F, false),
            new Theme(5, R.string.theme_rose,
                    0xFF150E13, 0xFF21141E, 0xFF2B1A26, 0xFF3F2638,
                    0xFFFFF3F8, 0xFFAD889C, 0xFF1A1017,
                    0xFFF28BA8, 0xFFFFC65C, 0xFF7EE0B8, false),
            new Theme(6, R.string.theme_cyber,
                    0xFF070B10, 0xFF0D141D, 0xFF121D28, 0xFF1E3040,
                    0xFFEFFBFF, 0xFF7E93A3, 0xFF0A1017,
                    0xFF00E5FF, 0xFFFFD60A, 0xFF39FF88, false),
            new Theme(7, R.string.theme_linen,
                    0xFFF4EFE6, 0xFFFFFFFF, 0xFFFBF7F0, 0xFFE4D9C9,
                    0xFF231D16, 0xFF7C6F5F, 0xFFEDE6DA,
                    0xFF6B57D6, 0xFFC7831A, 0xFF128F63, true)
    };

    private Themes() {
    }

    public static Theme get(int index) {
        if (index < 0 || index >= ALL.length) {
            return ALL[0];
        }
        return ALL[index];
    }

    public static int count() {
        return ALL.length;
    }
}
