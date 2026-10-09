package com.eluqen.shiftcalendar.data;

import com.eluqen.shiftcalendar.core.PersianDate;
import java.util.Map;

/** A successful work-state mutation always resynchronizes its consumers. */
public final class CardexMutations {
    public interface Store {
        void write(Map<String,Object> values);
        void delete(String date);
        void changed();
    }
    private CardexMutations() {}
    public static void save(CardexRecord record, Store store) {
        if(record==null) throw new IllegalArgumentException("Cardex is required");
        CardexRecord r=record.copy(); r.normalizeOperational();
        r.jdn=PersianDate.parse(r.date).toJdn();
        if(r.isEmpty()) store.delete(r.date); else store.write(CardexCodec.sqlValues(r));
        store.changed();
    }
    public static void delete(String date, Store store) {
        store.delete(date);
        store.changed();
    }
}
