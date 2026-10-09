package com.eluqen.shiftcalendar.work;

import com.eluqen.shiftcalendar.data.*;
import com.eluqen.shiftcalendar.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Executes the production migration/codec against real SQLite (Python stdlib binding). */
public class CardexPersistenceTest {
    private static final class Sqlite implements CardexSchema.Store, CardexMutations.Store, AutoCloseable {
        final Path file;
        int changes;
        Sqlite() throws Exception { file=Files.createTempFile("eluqen-cardex-fixture-", ".db"); }
        JSONArray run(String sql,Object... args) {
            try {
                String script="import sqlite3,json,sys; c=sqlite3.connect(sys.argv[1]); x=c.execute(sys.argv[2],json.loads(sys.argv[3])); rows=x.fetchall(); c.commit(); print(json.dumps(rows))";
                Process p=new ProcessBuilder("python3","-c",script,file.toString(),sql,new JSONArray(Arrays.asList(args)).toString()).redirectErrorStream(true).start();
                String output=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
                if(p.waitFor()!=0)throw new AssertionError(output);
                return new JSONArray(output);
            } catch(Exception e) { throw new AssertionError(e); }
        }
        public void exec(String sql) { run(sql); }
        public boolean hasColumn(String name) {
            JSONArray rows=run("PRAGMA table_info(cardex)");for(int i=0;i<rows.length();i++)if(name.equals(rows.optJSONArray(i).optString(1)))return true;return false;
        }
        Map<String,String> columns() throws JSONException {
            TreeMap<String,String> columns=new TreeMap<>();JSONArray rows=run("PRAGMA table_info(cardex)");
            for(int i=0;i<rows.length();i++){JSONArray row=rows.getJSONArray(i);columns.put(row.getString(1),row.get(2)+"/"+row.get(3)+"/"+row.get(4)+"/"+row.get(5));}return columns;
        }
        void save(CardexRecord r) { CardexMutations.save(r,this); }
        public void write(Map<String,Object> values) {
            String marks=String.join(",",Collections.nCopies(values.size(),"?"));
            run("INSERT OR REPLACE INTO cardex ("+String.join(",",values.keySet())+") VALUES ("+marks+")",values.values().toArray());
        }
        public void delete(String date) { run("DELETE FROM cardex WHERE date=?",date); }
        public void changed() { changes++; }
        CardexRecord load(String date) throws JSONException {
            JSONArray row=run("SELECT "+String.join(",",CardexCodec.COLUMNS)+" FROM cardex WHERE date=?",date).getJSONArray(0);
            Map<String,Object> values=new LinkedHashMap<>();for(int i=0;i<CardexCodec.COLUMNS.length;i++)values.put(CardexCodec.COLUMNS[i],row.get(i));return CardexCodec.fromSql(values);
        }
        public void close() throws IOException { Files.deleteIfExists(file); }
    }
    private String oldSchema() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/legacy-cardex-v1.sql")){assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}
    }
    private CardexRecord row() { CardexRecord r=new CardexRecord();r.date="1405/07/12";r.jdn=PersianDate.parse(r.date).toJdn();r.note="یادداشت محفوظ";r.noteTime="09:10";return r; }
    @Test public void cleanAndV1UpgradeHaveIdenticalColumnsTypesDefaultsAndKeys() throws Exception {
        try(Sqlite clean=new Sqlite();Sqlite upgraded=new Sqlite()){
            clean.exec(CardexSchema.CREATE);upgraded.exec(oldSchema());CardexSchema.upgrade(upgraded);assertEquals(clean.columns(),upgraded.columns());assertEquals(23,clean.columns().size());
        }
    }
    @Test public void upgradePreservesEveryLegacyFieldEvenWhenFlagsConflict() throws Exception {
        try(Sqlite db=new Sqlite()){
            db.exec(oldSchema());db.run("INSERT INTO cardex(date,jdn,note,note_time,rest_enabled,rest_start,rest_end,leave_enabled,leave_start,leave_end,swap_enabled,swap_note,swap_time,overtime_enabled,overtime_start,overtime_end) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", "1405/07/12",PersianDate.parse("1405/07/12").toJdn(),"متن", "09:10",1,"07:00","15:00",1,"08:00","16:00",1,"همکار","15:00",1,"23:00","07:00");
            String before=db.run("SELECT date,jdn,note,note_time,rest_enabled,rest_start,rest_end,leave_enabled,leave_start,leave_end,swap_enabled,swap_note,swap_time,overtime_enabled,overtime_start,overtime_end FROM cardex").toString();
            CardexSchema.upgrade(db);
            assertEquals(before,db.run("SELECT date,jdn,note,note_time,rest_enabled,rest_start,rest_end,leave_enabled,leave_start,leave_end,swap_enabled,swap_note,swap_time,overtime_enabled,overtime_start,overtime_end FROM cardex").toString());
            CardexRecord r=db.load("1405/07/12");assertTrue(r.rest&&r.leave&&r.swap&&r.overtime);assertEquals(DayWorkResolver.MODE_REST,DayWorkResolver.mode(r));assertEquals(1,db.run("SELECT count(*) FROM cardex").getJSONArray(0).getInt(0));
        }
    }
    @Test public void partiallyUpgradedV2AndV3SchemasUpgradeWithoutDuplicateColumns() throws Exception {
        for(int added=1;added<=7;added++)try(Sqlite db=new Sqlite()){
            db.exec(oldSchema());String[] alters={"note_ink TEXT NOT NULL DEFAULT ''","swap_shift_code TEXT NOT NULL DEFAULT ''","rest_full_shift INTEGER NOT NULL DEFAULT 0","leave_full_shift INTEGER NOT NULL DEFAULT 0","overtime_shift_code TEXT NOT NULL DEFAULT ''","overtime_day_offset INTEGER NOT NULL DEFAULT 99","overtime_type TEXT NOT NULL DEFAULT ''"};
            for(int i=0;i<added;i++)db.exec("ALTER TABLE cardex ADD COLUMN "+alters[i]);
            CardexSchema.upgrade(db);CardexSchema.upgrade(db);assertEquals(23,db.columns().size());
        }
    }
    @Test public void upgradeNeverDeletesAnEmptyLegacyRow() throws Exception {
        try(Sqlite db=new Sqlite()){db.exec(oldSchema());db.run("INSERT INTO cardex(date,jdn) VALUES (?,?)","1405/07/12",1);CardexSchema.upgrade(db);assertEquals(1,db.run("SELECT count(*) FROM cardex").getJSONArray(0).getInt(0));}
    }
    @Test public void handwrittenNotePersistsThroughSqlAndBackup() throws Exception {
        try(Sqlite db=new Sqlite()){
            db.exec(CardexSchema.CREATE);CardexRecord r=row();
            r.noteInk="[[1,3,[[0.1,0.2],[0.3,0.4]]]]";db.save(r);
            CardexRecord stored=db.load(r.date);assertEquals(r.noteInk,stored.noteInk);
            CardexRecord restored=CardexCodec.fromJson(CardexCodec.toJson(stored));
            assertEquals(r.noteInk,restored.noteInk);
        }
    }
    @Test public void newSwapCodePersistsThroughRealSQLite() throws Exception {
        try(Sqlite db=new Sqlite()){db.exec(CardexSchema.CREATE);CardexRecord r=row();r.swap=true;r.swapShiftCode="E";db.save(r);assertEquals("E",db.load(r.date).swapShiftCode);assertEquals(r.note,db.load(r.date).note);}
    }
    @Test public void newOvertimeCodePersistsThroughRealSQLite() throws Exception {
        try(Sqlite db=new Sqlite()){db.exec(CardexSchema.CREATE);CardexRecord r=row();r.overtime=true;r.overtimeShiftCode="N";r.overtimeStart="23:00";r.overtimeEnd="07:00";r.overtimeDayOffset=0;r.overtimeType=OvertimePolicy.TYPE_REPLACEMENT;db.save(r);CardexRecord read=db.load(r.date);assertEquals("N",read.overtimeShiftCode);assertEquals(0,read.overtimeDayOffset);assertEquals(OvertimePolicy.TYPE_REPLACEMENT,read.overtimeType);assertEquals(480,CardexStats.calculate(Arrays.asList(read),false).overtimeMinutes);}
    }
    @Test public void editingSwapIntoLeaveClearsPersistedStaleValues() throws Exception {
        try(Sqlite db=new Sqlite()){db.exec(CardexSchema.CREATE);CardexRecord r=row();r.swap=true;r.swapShiftCode="E";r.swapNote="همکار";r.swapTime="15:00";db.save(r);
            CardexRecord draft=db.load(r.date);draft.clearOperational();draft.leave=true;draft.leaveStart="07:00";draft.leaveEnd="15:00";db.save(draft);
            CardexRecord actual=db.load(r.date);assertTrue(actual.leave);assertFalse(actual.swap||actual.rest||actual.overtime);assertEquals("",actual.swapShiftCode);assertEquals("",actual.swapNote);assertEquals("",actual.swapTime);assertEquals(r.note,actual.note);
        }
    }
    @Test public void oldBackupWithoutNewFieldsImportsSafely() throws Exception {
        JSONObject old=new JSONObject("{\"date\":\"1405/07/12\",\"jdn\":1,\"swap\":true,\"swapTime\":\"15:00\",\"note\":\"متن\"}");
        CardexRecord r=CardexCodec.fromJson(old);assertTrue(r.swap);assertEquals("",r.swapShiftCode);assertEquals("15:00",r.swapTime);assertEquals(PersianDate.parse(r.date).toJdn(),r.jdn);assertEquals("",r.noteInk);assertEquals(99,r.overtimeDayOffset);assertEquals("",r.overtimeType);
        try(Sqlite db=new Sqlite()){db.exec(CardexSchema.CREATE);db.save(r);assertEquals("15:00",db.load(r.date).swapTime);}
    }
    @Test public void newBackupRoundtripPreservesEveryExclusiveMode() throws Exception {
        for(int mode=0;mode<=4;mode++)try(Sqlite db=new Sqlite()){
            db.exec(CardexSchema.CREATE);CardexRecord r=row();r.rest=mode==1;r.leave=mode==2;r.swap=mode==3;r.overtime=mode==4;
            if(mode==1){r.restFullShift=true;r.restStart="07:00";r.restEnd="15:00";}
            if(mode==2){r.leaveFullShift=true;r.leaveStart="07:00";r.leaveEnd="15:00";}
            if(mode==3){r.swapShiftCode="E";r.swapNote="همکار";}
            if(mode==4){r.overtimeShiftCode="N";r.overtimeStart="23:00";r.overtimeEnd="07:00";r.overtimeDayOffset=1;r.overtimeType=OvertimePolicy.TYPE_NECESSITY;}
            CardexRecord imported=CardexCodec.fromJson(new JSONObject(CardexCodec.toJson(r).toString()));db.save(imported);CardexRecord actual=db.load(r.date);
            assertEquals(CardexCodec.toJson(r).toString(),CardexCodec.toJson(actual).toString());assertEquals(mode,DayWorkResolver.mode(actual));
        }
    }
    @Test public void oldConflictingBackupNormalizesOnceAndKeepsFreeNote() throws Exception {
        CardexRecord r=row();r.rest=true;r.leave=true;r.swap=true;r.swapShiftCode="E";r.overtime=true;
        CardexRecord imported=CardexCodec.fromJson(CardexCodec.toJson(r));assertTrue(imported.rest);assertFalse(imported.leave||imported.swap||imported.overtime);assertEquals(r.note,imported.note);
    }
    @Test public void upgradeInPlacePreservesDbFileCardexPlanAndUnrelatedTables() throws Exception {
        try(Sqlite db=new Sqlite()){
            db.exec(oldSchema());db.exec("CREATE TABLE shift_plans(id INTEGER PRIMARY KEY, pattern TEXT)");db.exec("CREATE TABLE colleagues(name TEXT)");db.exec("INSERT INTO shift_plans VALUES(9,'D,E,N,O')");db.exec("INSERT INTO colleagues VALUES('همکار')");db.run("INSERT INTO cardex(date,jdn,note) VALUES(?,?,?)","1405/07/12",1,"متن");
            Path same=db.file;db.exec("PRAGMA user_version=1");CardexSchema.upgrade(db);db.exec("PRAGMA user_version="+CardexSchema.VERSION);
            assertEquals(same,db.file);assertEquals("D,E,N,O",db.run("SELECT pattern FROM shift_plans").getJSONArray(0).getString(0));assertEquals("همکار",db.run("SELECT name FROM colleagues").getJSONArray(0).getString(0));assertEquals("متن",db.run("SELECT note FROM cardex").getJSONArray(0).getString(0));assertEquals(7,db.run("PRAGMA user_version").getJSONArray(0).getInt(0));
        }
    }
    @Test public void fullShiftLeaveAndRestMinutesAndSwapCountStayCorrect() throws Exception {
        CardexRecord leave=row();leave.leave=true;leave.leaveFullShift=true;leave.leaveStart="23:00";leave.leaveEnd="07:00";
        CardexRecord rest=row();rest.rest=true;rest.restStart="07:00";rest.restEnd="15:00";
        CardexRecord swap=row();swap.swap=true;swap.swapShiftCode="N";
        CardexStats stats=CardexStats.calculate(Arrays.asList(leave,rest,swap),false);assertEquals(480,stats.leaveMinutes);assertEquals(480,stats.restMinutes);assertEquals(1,stats.swapCount);assertEquals(3,stats.recordedDays);
    }
    @Test public void saveEditAndDeleteEachResyncExactlyAfterPersistence() throws Exception {
        try(Sqlite db=new Sqlite()) {
            db.exec(CardexSchema.CREATE);CardexRecord r=row();r.swap=true;r.swapShiftCode="E";
            db.save(r);assertEquals(1,db.changes);assertEquals("E",db.load(r.date).swapShiftCode);
            r.clearOperational();r.leave=true;db.save(r);assertEquals(2,db.changes);assertTrue(db.load(r.date).leave);
            CardexMutations.delete(r.date,db);assertEquals(3,db.changes);assertEquals(0,db.run("SELECT COUNT(*) FROM cardex").getJSONArray(0).getInt(0));
        }
    }
    @Test public void removingLastOverrideResyncsAndReturnsToPatternWithoutEmptyRow() throws Exception {
        try(Sqlite db=new Sqlite()) {
            db.exec(CardexSchema.CREATE);CardexRecord r=row();r.note="";r.noteTime="";r.leave=true;db.save(r);
            r.clearOperational();db.save(r);assertEquals(2,db.changes);assertEquals(0,db.run("SELECT COUNT(*) FROM cardex").getJSONArray(0).getInt(0));
        }
    }
    @Test public void failedStorageDoesNotClaimSuccessOrResync() {
        final int[] calls={0};CardexMutations.Store failing=new CardexMutations.Store() {
            public void write(Map<String,Object> values) { throw new IllegalStateException("fixture storage failure"); }
            public void delete(String date) { throw new IllegalStateException("fixture storage failure"); }
            public void changed() { calls[0]++; }
        };
        try { CardexMutations.save(row(),failing);fail("Expected failure"); } catch(IllegalStateException expected) { }
        assertEquals(0,calls[0]);
    }

}
