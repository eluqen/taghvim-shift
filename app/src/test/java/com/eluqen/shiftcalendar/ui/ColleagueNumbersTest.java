package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;

public final class ColleagueNumbersTest {
    @Test public void persianDigitsAreComparableWithWesternNumbers(){
        assertEquals("09123456789",ColleagueNumbers.key("۰۹۱۲ ۳۴۵ ۶۷۸۹"));
        assertEquals("+4912345",ColleagueNumbers.key("+49 (123) 45"));
    }
    @Test public void avoidsDuplicateContactsWithoutTruncatingNumbers(){
        assertTrue(ColleagueNumbers.has(Arrays.asList("۰۹۱۲-۳۴۵۶۷۸۹"),"09123456789"));
        assertFalse(ColleagueNumbers.has(Arrays.asList("09123456789"),"09123456788"));
    }
    @Test public void emptyPhonesCannotBeAppended(){
        assertTrue(ColleagueNumbers.has(Collections.emptyList(),""));
        assertTrue(ColleagueNumbers.has(Collections.emptyList(),"  "));
    }
    @Test public void roundTripsUnlimitedLabeledNumbers(){
        java.util.List<ColleagueNumbers.NumberEntry> many=new java.util.ArrayList<>();
        for(int i=0;i<16;i++)
            many.add(new ColleagueNumbers.NumberEntry("شماره "+i,"+49123"+i));
        String encoded=ColleagueNumbers.encode(many);
        java.util.List<ColleagueNumbers.NumberEntry> decoded=ColleagueNumbers.entries(encoded);
        assertEquals(16,decoded.size());
        for(int i=0;i<16;i++){
            assertEquals("شماره "+i,decoded.get(i).label);
            assertEquals("+49123"+i,decoded.get(i).number);
        }
        assertEquals(encoded,ColleagueNumbers.importJson(encoded));
        assertEquals(encoded,ColleagueNumbers.importJson(ColleagueNumbers.decode(encoded)));
    }

    @Test public void oldBackupsWithoutExtraNumbersRemainCompatible(){
        assertEquals("[]",ColleagueNumbers.importJson(null));
        assertEquals("[]",ColleagueNumbers.importJson(""));
        assertTrue(ColleagueNumbers.entries(null).isEmpty());
    }

    @Test public void preservesPhoneLabelsAndNumbers(){
        ColleagueNumbers.NumberEntry entry=new ColleagueNumbers.NumberEntry(
            "  محل کار "," +49 333 ");
        assertEquals("محل کار",entry.label);
        assertEquals("+49 333",entry.number);
    }
}
