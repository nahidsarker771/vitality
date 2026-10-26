package com.nahid.vitality.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.nahid.vitality.core.PowerTime;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HistoryDb extends SQLiteOpenHelper {

    public static final int SCHEMA_VERSION = 1;
    private static final String NAME = "vitality_history.db";
    private static HistoryDb instance;

    private HistoryDb(Context context) {
        super(context.getApplicationContext(), NAME, null, SCHEMA_VERSION);
    }

    public static synchronized HistoryDb get(Context context) {
        if (instance == null) {
            instance = new HistoryDb(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history ("
                + "day TEXT PRIMARY KEY,"
                + "samples INTEGER NOT NULL DEFAULT 0,"
                + "sum_will REAL NOT NULL DEFAULT 0,"
                + "sum_wealth REAL NOT NULL DEFAULT 0,"
                + "sum_energy REAL NOT NULL DEFAULT 0,"
                + "last_will REAL NOT NULL DEFAULT 0,"
                + "last_wealth REAL NOT NULL DEFAULT 0,"
                + "last_energy REAL NOT NULL DEFAULT 0,"
                + "updated_at INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (int version = oldVersion; version < newVersion; version++) {
            upgradeTo(db, version + 1);
        }
    }

    private void upgradeTo(SQLiteDatabase db, int version) {
        switch (version) {
            default:
                break;
        }
    }

    public static void sample(Context context, float percent) {
        if (percent <= 0f) {
            return;
        }
        String day = PowerTime.powerDayKey();
        long now = System.currentTimeMillis();
        SQLiteDatabase db = get(context).getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues seed = new ContentValues();
            seed.put("day", day);
            seed.put("updated_at", now);
            db.insertWithOnConflict("history", null, seed, SQLiteDatabase.CONFLICT_IGNORE);
            ContentValues update = new ContentValues();
            update.put("sum_will", percent);
            update.put("sum_wealth", percent);
            update.put("sum_energy", percent);
            update.put("last_will", percent);
            update.put("last_wealth", percent);
            update.put("last_energy", percent);
            update.put("updated_at", now);
            db.execSQL("UPDATE history SET samples = samples + 1,"
                            + " sum_will = sum_will + ?,"
                            + " sum_wealth = sum_wealth + ?,"
                            + " sum_energy = sum_energy + ?,"
                            + " last_will = ?,"
                            + " last_wealth = ?,"
                            + " last_energy = ?,"
                            + " updated_at = ?"
                            + " WHERE day = ?",
                    new Object[]{percent, percent, percent, percent, percent, percent, now, day});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<Day> days(int limit) {
        ArrayList<Day> list = new ArrayList<>();
        String sql = "SELECT day, samples, sum_will, sum_wealth, sum_energy,"
                + " last_will, last_wealth, last_energy, updated_at"
                + " FROM history ORDER BY day DESC";
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                list.add(read(cursor));
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    public int countDays() {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM history", null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public int samplesForDay(String dayKey) {
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT samples FROM history WHERE day = ?", new String[]{dayKey});
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public void clear() {
        getWritableDatabase().delete("history", null, null);
    }

    public String csv() {
        StringBuilder out = new StringBuilder();
        out.append("date,willpower_avg,wealth_avg,energy_avg,overall_avg,samples,last_overall\n");
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT day, samples, sum_will, sum_wealth, sum_energy,"
                        + " last_will, last_wealth, last_energy, updated_at"
                        + " FROM history ORDER BY day ASC", null);
        try {
            while (cursor.moveToNext()) {
                Day day = read(cursor);
                out.append(day.dayKey).append(',')
                        .append(String.format(Locale.US, "%.1f", day.avgWill)).append(',')
                        .append(String.format(Locale.US, "%.1f", day.avgWealth)).append(',')
                        .append(String.format(Locale.US, "%.1f", day.avgEnergy)).append(',')
                        .append(String.format(Locale.US, "%.1f", day.average())).append(',')
                        .append(day.samples).append(',')
                        .append(String.format(Locale.US, "%.1f", day.lastAverage()))
                        .append('\n');
            }
        } finally {
            cursor.close();
        }
        return out.toString();
    }

    private static Day read(Cursor cursor) {
        String key = cursor.getString(0);
        int samples = cursor.getInt(1);
        double sumWill = cursor.getDouble(2);
        double sumWealth = cursor.getDouble(3);
        double sumEnergy = cursor.getDouble(4);
        float lastWill = cursor.getFloat(5);
        float lastWealth = cursor.getFloat(6);
        float lastEnergy = cursor.getFloat(7);
        long updated = cursor.getLong(8);
        float divisor = Math.max(1, samples);
        LocalDate date;
        try {
            date = LocalDate.parse(key);
        } catch (Exception error) {
            date = LocalDate.now();
        }
        return new Day(key, date, samples,
                (float) (sumWill / divisor),
                (float) (sumWealth / divisor),
                (float) (sumEnergy / divisor),
                lastWill, lastWealth, lastEnergy, updated);
    }

    public static final class Day {
        public final String dayKey;
        public final LocalDate date;
        public final int samples;
        public final float avgWill;
        public final float avgWealth;
        public final float avgEnergy;
        public final float lastWill;
        public final float lastWealth;
        public final float lastEnergy;
        public final long updatedAt;

        Day(String dayKey, LocalDate date, int samples,
            float avgWill, float avgWealth, float avgEnergy,
            float lastWill, float lastWealth, float lastEnergy, long updatedAt) {
            this.dayKey = dayKey;
            this.date = date;
            this.samples = samples;
            this.avgWill = avgWill;
            this.avgWealth = avgWealth;
            this.avgEnergy = avgEnergy;
            this.lastWill = lastWill;
            this.lastWealth = lastWealth;
            this.lastEnergy = lastEnergy;
            this.updatedAt = updatedAt;
        }

        public float average() {
            return (avgWill + avgWealth + avgEnergy) / 3f;
        }

        public float lastAverage() {
            return (lastWill + lastWealth + lastEnergy) / 3f;
        }
    }
}
