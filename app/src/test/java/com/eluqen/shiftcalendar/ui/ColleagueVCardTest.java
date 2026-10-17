package com.eluqen.shiftcalendar.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.Assert.*;

public final class ColleagueVCardTest {
    private static List<ColleagueVCard.Contact> parse(String v){
        return ColleagueVCard.parse(v.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void extractsAllPhoneNumbersFromOneContact(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:علی رضایی\r\n"+
            "TEL;TYPE=CELL:09123456789\r\nTEL;TYPE=WORK:0211234567\r\n"+
            "TEL;TYPE=HOME:+49 123 456\r\nEND:VCARD\r\n");
        assertEquals(1,cards.size());
        assertEquals("علی رضایی",cards.get(0).name);
        assertEquals(3,cards.get(0).numbers.size());
        assertEquals("09123456789",cards.get(0).numbers.get(0).number);
        assertEquals("محل کار",cards.get(0).numbers.get(1).label);
        assertEquals("منزل",cards.get(0).numbers.get(2).label);
    }
    @Test public void multilineFoldedAndTelephoneUri(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\nVERSION:4.0\nFN:مریم نوروزی\n"+
            "TEL;VALUE=uri;TYPE=cell:tel:+4912\n 3456\n"+
            "TEL;TYPE=work:021444444\nEND:VCARD\n");
        assertEquals(2,cards.get(0).numbers.size());
        assertEquals("+49123456",cards.get(0).numbers.get(0).number);
    }
    @Test public void repeatedNumbersAreDeduplicated(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\nFN:Test\nTEL;TYPE=CELL:09123456789\n"+
            "TEL;TYPE=HOME:۰۹۱۲۳۴۵۶۷۸۹\nEND:VCARD\n");
        assertEquals(1,cards.get(0).numbers.size());
    }
    @Test public void multiContactFileIsNotMerged(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\nFN:One\nTEL:123123\nEND:VCARD\n"+
            "BEGIN:VCARD\nFN:Two\nTEL:456456\nTEL:789789\nEND:VCARD\n");
        assertEquals(2,cards.size());
        assertEquals("One",cards.get(0).name);
        assertEquals(1,cards.get(0).numbers.size());
        assertEquals(2,cards.get(1).numbers.size());
    }
    @Test public void samsungVCard21QuotedPrintableUtf8(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\nVERSION:2.1\nFN;CHARSET=UTF-8;ENCODING=QUOTED-PRINTABLE:=D8=B9=D9=84=DB=8C\n"+
            "TEL;CELL;PREF:09123456789\nEND:VCARD\n");
        assertEquals("علی",cards.get(0).name);
        assertEquals("موبایل",cards.get(0).numbers.get(0).label);
    }
    @Test public void untrustedExtraFieldsNotRetained(){
        List<ColleagueVCard.Contact> cards=parse(
            "BEGIN:VCARD\nFN:Known\nEMAIL:secret@example.com\nADR:Home street\n"+
            "NOTE:Private password\nTEL:09123456789\nEND:VCARD\n");
        assertEquals("Known",cards.get(0).name);
        assertEquals(1,cards.get(0).numbers.size());
        assertFalse(cards.get(0).toString().contains("secret@example.com"));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsNonVCard(){parse("TEL:09123456789");}
    @Test(expected=IllegalArgumentException.class)
    public void rejectsTruncatedVCard(){parse("BEGIN:VCARD\nTEL:09123456789");}
    @Test(expected=IllegalArgumentException.class)
    public void rejectsTooLargeFile(){
        ColleagueVCard.parse(new byte[ColleagueVCard.MAX_BYTES+1]);
    }
}
