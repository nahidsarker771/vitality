package com.nahid.vitality;

import java.util.ArrayList;

final class TabNavigator {

    private static final int HOME = 0;
    private static final int MAX_HISTORY = 24;
    private static final long NAV_COOLDOWN_MS = 350L;

    private static final ArrayList<Integer> history = new ArrayList<>();
    private static int current = -1;
    private static long lastNavigationAt = 0L;

    private TabNavigator() {
    }

    static synchronized void onTabResumed(int tab) {
        if (tab < 0) {
            return;
        }
        if (history.isEmpty()) {
            history.add(tab);
            current = tab;
            return;
        }
        if (current == tab) {
            return;
        }
        history.add(tab);
        current = tab;
        trim();
    }

    static synchronized boolean isCurrent(int tab) {
        return current == tab;
    }

    static synchronized boolean beginNavigation(int target) {
        if (target < 0 || target == current) {
            return false;
        }
        long now = System.nanoTime() / 1_000_000L;
        if (lastNavigationAt != 0L && now - lastNavigationAt < NAV_COOLDOWN_MS) {
            return false;
        }
        lastNavigationAt = now;
        history.add(target);
        current = target;
        trim();
        return true;
    }

    static synchronized int beginBack(int from) {
        if (from < 0) {
            return -1;
        }
        if (from != current) {
            return -2;
        }
        if (history.isEmpty()) {
            history.add(from);
            current = from;
            return -2;
        }
        if (history.size() <= 1) {
            if (from == HOME) {
                return -1;
            }
            history.clear();
            history.add(HOME);
            current = HOME;
            lastNavigationAt = 0L;
            return HOME;
        }
        int position = history.lastIndexOf(from);
        if (position <= 0) {
            return -1;
        }
        history.remove(position);
        current = history.get(history.size() - 1);
        lastNavigationAt = 0L;
        return current;
    }

    static synchronized void clear() {
        history.clear();
        current = -1;
        lastNavigationAt = 0L;
    }

    private static void trim() {
        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }
}
