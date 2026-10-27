package com.nahid.vitality.core;

import com.nahid.vitality.R;

public final class Themes {

    private static final Theme[] ALL = new Theme[]{
            new Theme(0, R.string.theme_midnight,
                    0xFF0D1117, 0xFF161B22, 0xFF1C2430, 0xFF2B3646,
                    0xFFE6EDF3, 0xFF9AA7B8, 0xFF10161F,
                    0xFF9A8CF0, 0xFFE3B341, 0xFF6FD3A6, false),
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
                    0xFF6B57D6, 0xFFC7831A, 0xFF128F63, true),
            new Theme(8, R.string.theme_nord,
                    0xFF2E3440, 0xFF3B4252, 0xFF434C5E, 0xFF4C566A,
                    0xFFECEFF4, 0xFFA3AEC2, 0xFF292F3A,
                    0xFF88C0D0, 0xFFEBCB8B, 0xFFA3BE8C, false),
            new Theme(9, R.string.theme_solarized,
                    0xFFFDF6E3, 0xFFFFFBF0, 0xFFF5EFDC, 0xFFDDD6C1,
                    0xFF4B5D63, 0xFF7C8B8F, 0xFFF2ECDA,
                    0xFF268BD2, 0xFFB58900, 0xFF2AA198, true),
            new Theme(10, R.string.theme_gruvbox,
                    0xFF282828, 0xFF32302F, 0xFF3C3836, 0xFF504945,
                    0xFFEBDBB2, 0xFFA89984, 0xFF242221,
                    0xFF83A598, 0xFFD79921, 0xFFB8BB26, false),
            new Theme(11, R.string.theme_rosepine,
                    0xFF191724, 0xFF1F1D2E, 0xFF26233A, 0xFF403D52,
                    0xFFE0DEF4, 0xFF908CAA, 0xFF16141F,
                    0xFFC4A7E7, 0xFFF6C177, 0xFF9CCFD8, false),
            new Theme(12, R.string.theme_catppuccin,
                    0xFF1E1E2E, 0xFF313244, 0xFF45475A, 0xFF585B70,
                    0xFFCDD6F4, 0xFFA6ADC8, 0xFF181825,
                    0xFFCBA6F7, 0xFFF9E2AF, 0xFFA6E3A1, false),
            new Theme(13, R.string.theme_tokyo,
                    0xFF1A1B26, 0xFF24283B, 0xFF2F3549, 0xFF414868,
                    0xFFC0CAF5, 0xFFA9B1D6, 0xFF16161E,
                    0xFFBB9AF7, 0xFFE0AF68, 0xFF9ECE6A, false),
            new Theme(14, R.string.theme_onedark,
                    0xFF282C34, 0xFF2F343E, 0xFF3A404B, 0xFF4B5263,
                    0xFFDCDFE4, 0xFFA0A8B7, 0xFF23272E,
                    0xFFC678DD, 0xFFE5C07B, 0xFF98C379, false),
            new Theme(15, R.string.theme_kanagawa,
                    0xFF1F1F28, 0xFF2A2A37, 0xFF363646, 0xFF54546D,
                    0xFFDCD7BA, 0xFFA6A69C, 0xFF1A1A22,
                    0xFF957FB8, 0xFFE6C384, 0xFF98BB6C, false)
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
