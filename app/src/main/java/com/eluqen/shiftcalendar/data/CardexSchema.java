package com.eluqen.shiftcalendar.data;

/** Non-destructive schema changes shared by clean install and upgrade. */
public final class CardexSchema {
    public static final int VERSION = 7;
    public static final String CREATE = "CREATE TABLE cardex (date TEXT PRIMARY KEY, jdn INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', note_time TEXT NOT NULL DEFAULT '', note_ink TEXT NOT NULL DEFAULT '', rest_enabled INTEGER NOT NULL DEFAULT 0, rest_start TEXT NOT NULL DEFAULT '', rest_end TEXT NOT NULL DEFAULT '', leave_enabled INTEGER NOT NULL DEFAULT 0, leave_start TEXT NOT NULL DEFAULT '', leave_end TEXT NOT NULL DEFAULT '', swap_enabled INTEGER NOT NULL DEFAULT 0, swap_note TEXT NOT NULL DEFAULT '', swap_time TEXT NOT NULL DEFAULT '', overtime_enabled INTEGER NOT NULL DEFAULT 0, overtime_start TEXT NOT NULL DEFAULT '', overtime_end TEXT NOT NULL DEFAULT '', swap_shift_code TEXT NOT NULL DEFAULT '', rest_full_shift INTEGER NOT NULL DEFAULT 0, leave_full_shift INTEGER NOT NULL DEFAULT 0, overtime_shift_code TEXT NOT NULL DEFAULT '', overtime_day_offset INTEGER NOT NULL DEFAULT 99, overtime_type TEXT NOT NULL DEFAULT '')";
    public interface Store {
        boolean hasColumn(String name);
        void exec(String sql);
    }
    private CardexSchema() {}
    public static void upgrade(Store db) {
        add(db, "note_ink", "TEXT NOT NULL DEFAULT ''");
        add(db, "swap_shift_code", "TEXT NOT NULL DEFAULT ''");
        add(db, "rest_full_shift", "INTEGER NOT NULL DEFAULT 0");
        add(db, "leave_full_shift", "INTEGER NOT NULL DEFAULT 0");
        add(db, "overtime_shift_code", "TEXT NOT NULL DEFAULT ''");
        add(db, "overtime_day_offset", "INTEGER NOT NULL DEFAULT 99");
        add(db, "overtime_type", "TEXT NOT NULL DEFAULT ''");
        // Keep every legacy row and field. Resolution is deterministic in memory;
        // only an explicit new save normalizes the chosen operational mode.
    }
    private static void add(Store db, String name, String definition) {
        if (!db.hasColumn(name)) db.exec("ALTER TABLE cardex ADD COLUMN " + name + " " + definition);
    }
}
